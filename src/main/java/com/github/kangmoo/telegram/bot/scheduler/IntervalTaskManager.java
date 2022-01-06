package com.github.kangmoo.telegram.bot.scheduler;

import com.github.kangmoo.telegram.bot.scheduler.handler.IpChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Session Manager
 * 주기적으로 세션을 필터링하고 처리 (Default 1초)
 *
 * @author Kangmoo Heo
 */
public class IntervalTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(IntervalTaskManager.class);
    private static final IntervalTaskManager instance = new IntervalTaskManager();
    private final Map<String, IntervalTaskUnit> jobs = new HashMap<>();
    private Set<ScheduledExecutorService> executorService;
    private int defaultInterval = 30000;
    private boolean isStarted = false;

    private IntervalTaskManager() {
    }

    public static IntervalTaskManager getInstance() {
        return instance;
    }

    public IntervalTaskManager init() {
        addJob(IpChecker.class.getSimpleName(), new IpChecker(defaultInterval));
        return this;
    }

    public void start() {
        if (isStarted) {
            logger.info("Already Started Interval Task Manager");
            return;
        }
        isStarted = true;
        executorService = new HashSet<>(jobs.size());
        for (IntervalTaskUnit runner : jobs.values()) {
            ScheduledExecutorService job = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ITV_" + runner.getClass().getSimpleName()));
            executorService.add(job);
            job.scheduleAtFixedRate(runner, runner.getInterval() - System.currentTimeMillis() % runner.getInterval(),
                    runner.getInterval(), runner.timeUnit);
        }
        logger.info("Interval Task Manager Start");
    }

    public void stop() {
        if (!isStarted) {
            logger.info("Already Stopped Interval Task Manager");
            return;
        }
        isStarted = false;
        executorService.forEach(ExecutorService::shutdown);
        logger.info("Interval Task Manager Stop");
    }

    public void addJob(String name, IntervalTaskUnit runner) {
        if (jobs.get(name) != null) {
            logger.warn("Hashmap Key duplication");
            return;
        }
        logger.debug("Add Runner [{}]", name);
        jobs.put(name, runner);
    }

    public void removeJob(String name) {
        jobs.remove(name);
    }

    public void removeAllJob() {
        jobs.clear();
    }

    public void sessionCheck() {
        for (IntervalTaskUnit runner : jobs.values()) {
            runner.run();
        }
    }

    public int getDefaultInterval() {
        return defaultInterval;
    }

    public void setDefaultInterval(int msec) {
        this.defaultInterval = msec;
    }
}