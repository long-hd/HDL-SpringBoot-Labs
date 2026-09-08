package com.hdl.mt.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

/**
 * API Gateway — cửa vào duy nhất của hệ.
 *
 * <p>Client không gọi thẳng từng service nữa mà đi qua đây (cổng 8080). Gateway lo:
 * <ul>
 *   <li><b>Routing</b>: /accounts/** -> account-service, /transfers/** -> transfer-service,
 *       định tuyến theo TÊN service (lb://) qua Eureka.</li>
 *   <li><b>Cross-cutting concern</b>: đặt ở một chỗ thay vì lặp ở mọi service. Bước 5 demo
 *       rate limit; sau này auth (bước 11) cũng đặt ở đây.</li>
 * </ul>
 * Đây là lý do tồn tại của gateway: nếu chỉ để route thuần thì client gọi discovery trực
 * tiếp cũng được — giá trị thật nằm ở chỗ gom các mối quan tâm chung về một cửa.</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * Cách xác định "ai" để đếm hạn mức (key của token bucket). Ở đây dùng địa chỉ IP của
     * client: mỗi IP có một bucket riêng, vượt hạn mức thì gateway trả HTTP 429.
     *
     * <p>Được tham chiếu trong application.yml qua SpEL {@code #{@ipKeyResolver}}.</p>
     *
     * <p>KeyResolver trả {@link Mono} vì Gateway là reactive. Nếu không phân giải được key,
     * mặc định request bị từ chối (đổi được qua deny-empty-key).</p>
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remote = exchange.getRequest().getRemoteAddress();
            String ip = (remote != null) ? remote.getAddress().getHostAddress() : "unknown";
            return Mono.just(ip);
        };
    }
}
