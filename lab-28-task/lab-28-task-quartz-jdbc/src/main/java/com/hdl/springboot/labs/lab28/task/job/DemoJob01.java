package com.hdl.springboot.labs.lab28.task.job;

import com.hdl.springboot.labs.lab28.task.service.DemoService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Job demo — SimpleTrigger 5s.
 *
 * <p>{@link DisallowConcurrentExecution}: cùng JobKey trong cluster,
 * tối đa một execution đồng thời (kể cả nhiều JVM).
 *
 * <p>Quartz tạo instance job mới mỗi lần fire; Spring vẫn inject {@link DemoService}
 * qua {@link QuartzJobBean}.
 */
@DisallowConcurrentExecution
public class DemoJob01 extends QuartzJobBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DemoService demoService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        logger.info("[executeInternal][DemoJob01 chạy, demoService={}]", demoService);
    }

}
