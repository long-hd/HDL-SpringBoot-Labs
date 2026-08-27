package com.hdl.springboot.labs.lab28.task;

import com.hdl.springboot.labs.lab28.task.job.DemoJob01;
import com.hdl.springboot.labs.lab28.task.job.DemoJob02;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Đăng ký job thủ công qua {@link Scheduler} — thay cho bean trong
 * {@link com.hdl.springboot.labs.lab28.task.config.ScheduleConfig}.
 *
 * <p>Nếu đã có {@code ScheduleConfiguration}, không chạy test này cùng JobKey
 * trừ khi xóa row trong {@code QRTZ_*} hoặc bật overwrite.
 */
@SpringBootTest
public class QuartzSchedulerTest {

    @Autowired
    public Scheduler scheduler;

    @Test
    public void addDemoJob01Config() throws SchedulerException {
        // JobDetail
        JobDetail jobDetail = JobBuilder.newJob(DemoJob01.class)
                .withIdentity("demoJob01")
                .storeDurably()
                .build();

        // Trigger
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(5)
                .repeatForever();

        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("demoJob01Trigger")
                .withSchedule(scheduleBuilder)
                .build();

        // Schedule
        scheduler.scheduleJob(jobDetail, trigger);
    }

    @Test
    public void addDemoJob02Config() throws SchedulerException {
        // JobDetail
        JobDetail jobDetail = JobBuilder.newJob(DemoJob02.class)
                .withIdentity("demoJob02")
                .storeDurably()
                .build();

        // Trigger
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0/10 * * * * ?");

        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("demoJob02Trigger")
                .withSchedule(scheduleBuilder)
                .build();

        // Schedule
        scheduler.scheduleJob(jobDetail, trigger);
    }

}
