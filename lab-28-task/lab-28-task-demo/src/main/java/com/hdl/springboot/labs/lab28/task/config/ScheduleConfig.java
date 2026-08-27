package com.hdl.springboot.labs.lab28.task.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật Spring Task scheduling cho toàn bộ application context.
 *
 * <p>Khi có {@link EnableScheduling}, Boot auto-config {@code TaskSchedulingAutoConfiguration}
 * và tạo bean {@code ThreadPoolTaskScheduler} (trừ khi bật virtual thread — xem bên dưới).
 * Các method {@code @Scheduled} trên bean Spring sẽ được đăng ký vào scheduler đó.
 *
 * <p>Cấu hình thread pool / graceful shutdown: {@code spring.task.scheduling.*}
 * trong {@code application.yml} → {@code TaskSchedulingProperties}.
 *
 * @see org.springframework.scheduling.annotation.Scheduled
 * @see org.springframework.boot.autoconfigure.task.TaskSchedulingProperties
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
