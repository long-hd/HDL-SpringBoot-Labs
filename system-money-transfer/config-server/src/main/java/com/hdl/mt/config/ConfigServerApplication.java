package com.hdl.mt.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server — nơi phát config nghiệp vụ tập trung (cổng 8888).
 *
 * <p>Nhớ: Config Server KHÔNG tự chứa config. Nó là "cái loa" đọc config từ một BACKEND rồi
 * phát cho service qua HTTP. Bước 6 dùng backend {@code native} = đọc file từ thư mục local
 * {@code config-repo/} (khai trong application.yml). Về sau có thể đổi backend sang git mà
 * không đổi code service.</p>
 *
 * <p>Service lấy config bằng cách khai {@code spring.config.import=optional:configserver:...}
 * (cơ chế Spring Boot 2.4+). Config Server phục vụ theo đường dẫn
 * {@code /{application}/{profile}}, nên file {@code config-repo/transfer-service.yml} sẽ được
 * phát cho service có {@code spring.application.name=transfer-service}.</p>
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
