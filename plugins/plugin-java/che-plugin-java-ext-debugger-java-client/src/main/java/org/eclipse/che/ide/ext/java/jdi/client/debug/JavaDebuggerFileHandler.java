/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarFileNode;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.text.TextPosition;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.List;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.OPEN;

/**
 * Responsible to open files in editor when debugger stopped at breakpoint.
 *
 * @author Anatoliy Bazko
 */
public class JavaDebuggerFileHandler {

    private final DebuggerManager          debuggerManager;
    private final EditorAgent              editorAgent;
    private final DtoFactory               dtoFactory;
    private final AppContext               appContext;
    private final EventBus                 eventBus;
    private final JavaNodeManager          javaNodeManager;
    private final ProjectExplorerPresenter projectExplorer;

    @Inject
    public JavaDebuggerFileHandler(DebuggerManager debuggerManager,
                                   EditorAgent editorAgent,
                                   DtoFactory dtoFactory,
                                   AppContext appContext,
                                   EventBus eventBus,
                                   JavaNodeManager javaNodeManager,
                                   ProjectExplorerPresenter projectExplorer) {
        this.debuggerManager = debuggerManager;
        this.editorAgent = editorAgent;
        this.dtoFactory = dtoFactory;
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.javaNodeManager = javaNodeManager;
        this.projectExplorer = projectExplorer;
    }

    @Nullable
    public void openFile(List<String> filePaths, String className, final int lineNumber) {
        if (debuggerManager.getActiveDebugger() != debuggerManager.getDebugger(MavenAttributes.MAVEN_ID)) {
            return;
        }

        VirtualFile activeFile = null;
        final EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor != null) {
            activeFile = activeEditor.getEditorInput().getFile();
        }

        if (activeFile == null || !filePaths.contains(activeFile.getPath())) {
            openFile(className, filePaths, 0, new AsyncCallback<VirtualFile>() {
                @Override
                public void onSuccess(VirtualFile result) {
                    scrollEditorToExecutionPoint((EmbeddedTextEditorPresenter)editorAgent.getActiveEditor(), lineNumber);
                }

                @Override
                public void onFailure(Throwable caught) {}
            });
        } else {
            scrollEditorToExecutionPoint((EmbeddedTextEditorPresenter)activeEditor, lineNumber);
        }
    }

    /**
     * Tries to open file from the project.
     * If fails then method will try to find resource from external dependencies.
     */
    private void openFile(@NotNull final String className,
                          final List<String> filePaths,
                          final int pathNumber,
                          final AsyncCallback<VirtualFile> callback) {
        if (pathNumber == filePaths.size()) {
            Log.error(DebuggerPresenter.class, "Can't open resource " + className);
            return;
        }

        String filePath = filePaths.get(pathNumber);

        if (!filePath.startsWith("/")) {
            openExternalResource(className, callback);
            return;
        }

        projectExplorer.getNodeByPath(new HasStorablePath.StorablePath(filePath)).then(new Operation<Node>() {
            public HandlerRegistration handlerRegistration;

            @Override
            public void apply(final Node node) throws OperationException {
                if (!(node instanceof FileReferenceNode)) {
                    return;
                }

                handlerRegistration = eventBus.addHandler(ActivePartChangedEvent.TYPE, new ActivePartChangedHandler() {
                    @Override
                    public void onActivePartChanged(ActivePartChangedEvent event) {
                        if (event.getActivePart() instanceof EditorPartPresenter) {
                            final VirtualFile openedFile = ((EditorPartPresenter)event.getActivePart()).getEditorInput().getFile();
                            if (((FileReferenceNode)node).getStorablePath().equals(openedFile.getPath())) {
                                handlerRegistration.removeHandler();
                                // give the editor some time to fully render it's view
                                new Timer() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess((VirtualFile)node);
                                    }
                                }.schedule(300);
                            }
                        }
                    }
                });
                eventBus.fireEvent(new FileEvent((VirtualFile)node, OPEN));

            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                // try another path
                openFile(className, filePaths, pathNumber + 1, callback);
            }
        });
    }

    private void openExternalResource(String className, final AsyncCallback<VirtualFile> callback) {
        JarEntry jarEntry = dtoFactory.createDto(JarEntry.class);
        jarEntry.setPath(className);
        jarEntry.setName(className.substring(className.lastIndexOf(".") + 1) + ".class");
        jarEntry.setType(JarEntry.JarEntryType.CLASS_FILE);

        final JarFileNode jarFileNode = javaNodeManager.getJavaNodeFactory()
                                                       .newJarFileNode(jarEntry,
                                                                       null,
                                                                       appContext.getCurrentProject().getProjectConfig(),
                                                                       javaNodeManager.getJavaSettingsProvider().getSettings());

        editorAgent.openEditor(jarFileNode, new EditorAgent.OpenEditorCallback() {
            @Override
            public void onEditorOpened(EditorPartPresenter editor) {
                // give the editor some time to fully render it's view
                new Timer() {
                    @Override
                    public void run() {
                        callback.onSuccess(jarFileNode);
                    }
                }.schedule(300);
            }

            @Override
            public void onEditorActivated(EditorPartPresenter editor) {
                new Timer() {
                    @Override
                    public void run() {
                        callback.onSuccess(jarFileNode);
                    }
                }.schedule(300);
            }
        });
    }

    private void scrollEditorToExecutionPoint(EmbeddedTextEditorPresenter editor, int lineNumber) {
        Document document = editor.getDocument();

        if (document != null) {
            TextPosition newPosition = new TextPosition(lineNumber, 0);
            document.setCursorPosition(newPosition);
        }
    }
}