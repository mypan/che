# Che job scheduling framework
## About
There is such common programming usecase then you need to execute some method periodically.
Usually it implemented with help of some sort ThreadPoolExecutor. But often developers not pay
nemeses attention for thread start stop routine. As a result we have unnamed thread or thread
that never stops, etc. Scheduling framework build to take away all threading routine away from developer
and add a couple of new features.

## Features
- Run job with fixed rate
- Run job with fixed delay
- Run job according to the cron expression
- Error logging
- Container configuration
- Automatic job discovering
- Automatic thread pull start and shutdown.
## TODO
- Ability to run demon jobs (can be terminated during JVM shutdown)
- Metrics and statistic
- Ability to control thread names
- Time by UTC
- Do not interrupt future jobs on exceptions
- Ability to disable task.

## How to use
### Installation
There is o couple of steps you need to do before start. Usually you need to do it once, in target war.


First: add maven dependency.
```
        <dependency>
            <groupId>org.eclipse.che.core</groupId>
            <artifactId>che-core-commons-schedule</artifactId>
        </dependency>
```
Second: You need to install Guice module
```
        install(new org.eclipse.che.commons.schedule.executor.ScheduleModule());
```
Thread: You need to configure core pool size. This is the minimum number of workers to keep alive.
```
  @Named("schedule.core_pool_size") Integer corePoolSize
```
Note: actual number of threads will be corePoolSize+1. One thread is needed to monitor cron jobs.

### Implementations notes
Framework can execute methods with any visibility and any name. But method mast have 0 parameters.
If method that need to be executed is
```
 void run()
```
and class implements java.lang.Runnable then this method will be executed without reflection that suppose to be faster then using reflections. Best practices
is to implement Runnable interface and schedule method run.
Classes mast be annotated with javax.inject.Singleton or com.google.inject.Singleton.



### Run job with fixed rate
If you would like to execute some method with fixed rate. You can mark it with annotation
for execution a periodic action that becomes enabled first after the given initial delay,
and subsequently with the given period; that is executions will commence after initialDelay
then initialDelay+period, then initialDelay + 2 * period, and so on.
If any execution of the task encounters an exception, subsequent  executions are suppressed.
Otherwise, the task will only terminate via cancellation or termination of the executor.
If any execution of this task takes longer than its period,
then subsequent executions may start late, but will not concurrently execute.
Analogue of java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate

Example 1: Given method scheduleBackup will be executed once a minute after 1 minute initial delay.
```

@Singleton
public class WorkspaceFsBackupScheduler {
    ...
    @ScheduleRate(initialDelay = 1, period = 1, unit = TimeUnit.MINUTES)
    public void scheduleBackup() {
       ...
    }
```

Example 2: Same as example 1, but timings configured over container named parameters.
```
@Singleton
public class WorkspaceFsBackupScheduler {
    ...
    @ScheduleRate(initialDelayParameterName = "fs.backup.init_dalay", periodParameterName = "fs.backup.period", unit = TimeUnit.MINUTES)
    public void scheduleBackup() {
       ...
    }
```
NOTE: if initialDelay and initialDelayParameterName  configured at the same time, initialDelayParameterName has grater weight
when statically configured value. Same for period and periodParameterName.

<blockquote>
    <p>NOTE: if initialDelay and initialDelayParameterName  configured at the same time, initialDelayParameterName has grater weight
       when statically configured value. Same for period and periodParameterName.</p>
</blockquote>

### Run job with fixed delay
If you would like to execute some method with fixed delay. You can mark method for execution periodic action that becomes enabled first after the given initial delay, and subsequently
 * with the given delay between the termination of one execution and the commencement of the next.
 * <p/>
 * Analogue of {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long,
 * java.util.concurrent.TimeUnit)}  }
 *


