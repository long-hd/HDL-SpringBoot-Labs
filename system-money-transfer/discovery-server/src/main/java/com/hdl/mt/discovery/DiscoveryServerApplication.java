package com.hdl.mt.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka server — "danh bạ" của hệ.
 *
 * <p>Các service (account-service, transfer-service) khi khởi động sẽ tự ĐĂNG KÝ tên + địa chỉ
 * vào đây, và gửi heartbeat định kỳ để báo "còn sống". Khi transfer-service muốn gọi
 * account-service, nó hỏi Eureka "account-service đang ở đâu" rồi mới gọi — thay cho việc
 * đóng cứng {@code http://localhost:8081} như bước 0–2.</p>
 *
 * <p>{@code @EnableEurekaServer} biến app Spring Boot này thành một Eureka server. Bản thân nó
 * KHÔNG tự đăng ký vào chính mình (xem cấu hình register-with-eureka=false).</p>
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
