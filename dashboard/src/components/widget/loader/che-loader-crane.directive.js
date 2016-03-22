/*
 * Copyright (c) 2015-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

/**
 * Defines a directive for animating iteration process
 * @author Oleksii Kurinnyi
 */
export class CheLoaderCrane {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($timeout, $window) {
    this.$timeout = $timeout;
    this.$window = $window;
    this.restrict = 'E';
    this.templateUrl = 'components/widget/loader/che-loader-crane.html';

    // scope values
    this.scope = {
      step: '@cheStep',
      allSteps: '=cheAllSteps',
      excludeSteps: '=cheExcludeSteps',
      switchOnIteration: '=?cheSwitchOnIteration',
      inProgress: '=cheInProgress'
    };
  }

  link($scope, element) {
    let craneEl = element.find('.che-loader-crane'),
      cargoEl = element.find('#che-loader-crane-load'),
      oldSteps = [],
      newStep,
      animationStopping = false,
      animationRunning = false;

    $scope.$watch(() => {
      return $scope.step;
    }, (newVal) => {
      newVal = parseInt(newVal, 10);

      // try to stop animation on last step
      if (newVal === $scope.allSteps.length - 1) {
        animationStopping = true;

        if (!$scope.switchOnIteration) {
          // stop animation immediately if it shouldn't wait until next iteration
          setNoAnimation();
        }
      }

      // skip steps excluded
      if ($scope.excludeSteps.indexOf(newVal) !== -1) {
        return;
      }

      newStep = newVal;

      // go to next step
      // if animation hasn't run yet or it shouldn't wait until next iteration
      if (!animationRunning || !$scope.switchOnIteration) {
        setAnimation();
        setCurrentStep();
      }

      if (oldSteps.indexOf(newVal) === -1) {
        oldSteps.push(newVal);
      }
    });

    let destroyResizeEvent;
    $scope.$watch(() => {
      return $scope.inProgress;
    }, (inProgress) => {

      if (!inProgress) {
        // destroy event
        if (typeof destroyResizeEvent === 'function') {
          destroyResizeEvent();
        }
        return;
      }

      // initial resize
      this.$timeout(() => {
        setCraneSize();
      },0);

      let timeoutPromise;
      destroyResizeEvent = angular.element(this.$window).bind('resize', (event) => {
        if (timeoutPromise) {
          this.$timeout.cancel(timeoutPromise);
        }
        timeoutPromise = this.$timeout(() => {
          setCraneSize();
        }, 100);
      });
    });

    if (!!$scope.switchOnIteration) {
      element.find('.che-loader-animation.trolley-block').bind('animationstart', () => {
        animationRunning = true;
      });
      element.find('.che-loader-animation.trolley-block').bind('animationiteration', () => {
        if (oldSteps.length){
          setCurrentStep();
        }
        else if (animationStopping) {
          setNoAnimation();
        }
      });
    }

    let applyScale = (el, scale) => {
        let height = craneEl.height(),
          width = craneEl.width();
        el.css('transform', 'scale3d('+scale+','+scale+','+scale+')');
        el.css('height', height * scale);
        el.css('width', width * scale);
      },
      hasScrollMoreThan = (el,diff) => {
        return el.scrollHeight - el.offsetHeight > diff;
      },
      setCraneSize = () => {
        let contentPage = angular.element('#create-project-content-page')[0],
          scrollEl = element.find('.che-loader-crane-scale-wrapper'),
          scaleStep = 0.1,
          scaleMin = 0.6,
          scale = scaleMin,
          bodyEl = angular.element(document).find('body')[0];

        applyScale(scrollEl, scale);
        scrollEl.css('display','block');

        // do nothing if loader is hidden by hide-sm directive
        if (element.find('.che-loader-crane-scale-wrapper:visible').length === 0) {
          return;
        }

        // hide loader if there is scroll on minimal scale
        if ((hasScrollMoreThan(bodyEl, 5) || hasScrollMoreThan(contentPage, 5)) && scale === scaleMin) {
          scrollEl.css('display','none');
          return;
        }

        while (scale < 1) {
          applyScale(scrollEl, scale + scaleStep);

          // check for scroll appearance
          if (hasScrollMoreThan(bodyEl, 5) || hasScrollMoreThan(contentPage, 5)) {
            applyScale(scrollEl, scale);
            break;
          }

          scale = scale + scaleStep;
        }
      },
      setAnimation = () => {
        craneEl.removeClass('che-loader-no-animation');
      },
      setNoAnimation = () => {
        animationRunning = false;
        craneEl.addClass('che-loader-no-animation');
      },
      setCurrentStep = () => {
        for (let i = 0; i < oldSteps.length; i++) {
          craneEl.removeClass('step-' + oldSteps[i]);
          cargoEl.removeClass('layer-' + oldSteps[i]);
        }
        oldSteps.length = 0;

        // avoid next layer blinking
        let currentLayer = element.find('.layers-in-box').find('.layer-'+newStep);
        currentLayer.css('visibility','hidden');
        this.$timeout(() => {
          currentLayer.removeAttr('style');
        },500);

        craneEl.addClass('step-' + newStep);
        cargoEl.addClass('layer-' + newStep);
      };
  }
}
