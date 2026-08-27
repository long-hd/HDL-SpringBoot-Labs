package com.hdl.springboot.labs.lab28.task.config;

import com.hdl.springboot.labs.lab28.task.job.DemoJob01;
import com.hdl.springboot.labs.lab28.task.job.DemoJob02;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký Quartz job bằng {@link JobDetail} + {@link Trigger} bean.
 *
 * <p>Startup: Boot persist job/trigger vào MySQL (JobStore JDBC).
 * Cách thay thế (prod hay dùng): {@code scheduler.scheduleJob(...)} trong test/service.
 *
 * <p>Chỉ dùng <em>một</em> cách đăng ký nếu không muốn trùng JobKey với DB cũ.
 */
@Configuration
public class ScheduleConfig {

    /**
     * Job01: {@link SimpleScheduleBuilder} — mỗi 5 giây.
     */
    public static class DemoJob01Configuration {
        @Bean
        public JobDetail demoJob01() {
            return JobBuilder.newJob(DemoJob01.class)
                    .withIdentity("demoJob01")
                    .storeDurably()
                    .build();
        }

        @Bean
        public Trigger demoJob01Trigger() {
            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(5)
                    .repeatForever();
            return TriggerBuilder.newTrigger()
                    .forJob(demoJob01())
                    .withIdentity("demoJob01Trigger")
                    .withSchedule(scheduleBuilder)
                    .build();
        }
    }

    /**
     * Job02: {@link CronScheduleBuilder} — giây 0, 10, 20, …
     */
    public static class DemoJob02Configuration {
        @Bean
        public JobDetail demoJob02() {
            return JobBuilder.newJob(DemoJob02.class)
                    .withIdentity("demoJob02")
                    .storeDurably()
                    .build();
        }

        @Bean
        public Trigger demoJob02Trigger() {
            CronScheduleBuilder scheduleBuilder =
                    CronScheduleBuilder.cronSchedule("0/10 * * * * ? *");
            return TriggerBuilder.newTrigger()
                    .forJob(demoJob02())
                    .withIdentity("demoJob02Trigger")
                    .withSchedule(scheduleBuilder)
                    .build();
        }
    }

}
