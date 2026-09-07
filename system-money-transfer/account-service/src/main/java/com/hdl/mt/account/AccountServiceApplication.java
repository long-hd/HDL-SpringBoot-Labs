package com.hdl.mt.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của account-service.
 *
 * <p>account-service là một ứng dụng Spring Boot ĐỘC LẬP: có main riêng, DB riêng
 * (account_db), chạy trên cổng riêng (8081). Đây là nguyên tắc nền của microservices
 * — mỗi service là một tiến trình tự lập, không chia sẻ runtime với service khác.</p>
 */
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
