package com.hdl.springboot.labs.lab28.task.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Job demo — CronTrigger mỗi 10 giây (0/10 * * * * ? *).
 */
@DisallowConcurrentExecution
public class DemoJob02 extends QuartzJobBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void executeInternal(JobExecutionContext context) {
        logger.info("[executeInternal][DemoJob02 chạy]");
    }

}
