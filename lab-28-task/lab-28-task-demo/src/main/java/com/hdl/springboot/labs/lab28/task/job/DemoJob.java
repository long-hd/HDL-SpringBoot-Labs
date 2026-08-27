package com.hdl.springboot.labs.lab28.task.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DemoJob {

    Logger logger = LoggerFactory.getLogger(DemoJob.class);

    private final AtomicInteger counts = new AtomicInteger(0);

    @Scheduled(fixedRate = 2000)
    public void execute() {
        logger.info("[execute][Định kỳ thực hiện lần thứ ({})]", counts.incrementAndGet());
    }

}
