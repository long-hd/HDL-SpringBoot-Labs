package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.AccountApi;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Tạo proxy {@link AccountApi} (HTTP Interface) — BƯỚC 3 gọi theo TÊN service qua discovery.
 *
 * <p>Khác bước 2 (URL cứng): giờ {@link RestClient} được tạo từ một builder gắn nhãn
 * {@link LoadBalanced}. Khi request có host là TÊN service ({@code http://account-service}),
 * Spring Cloud LoadBalancer sẽ chặn lại, hỏi Eureka "account-service có những instance nào",
 * chọn một instance rồi thay host bằng địa chỉ thật. Nhờ vậy không còn hardcode localhost:8081,
 * và khi chạy nhiều instance account-service thì tải được chia (bước 4 sẽ thử).</p>
 *
 * <p><b>Vì sao phải khai tay thế này</b> (chính là trade-off đã cảnh báo ở bước 2): ở Boot 3.5
 * / Spring Cloud 2025.0, HTTP Interface CHƯA tự tích hợp load-balancing — nên ta phải tự dựng
 * {@code @LoadBalanced RestClient.Builder} rồi đưa vào {@code HttpServiceProxyFactory}. Từ Spring
 * Cloud 2025.1 (Boot 4) việc này được làm tự động. Cách dùng {@code @LoadBalanced} với
 * {@code RestClient.Builder} là theo tài liệu Spring Cloud Commons.</p>
 *
 * <p>Lưu ý bean: Spring Boot đã có sẵn một {@code RestClient.Builder} tự cấu hình (KHÔNG
 * load-balanced). Ta thêm một builder CÓ {@code @LoadBalanced}; vì thế khi lấy phải dùng đúng
 * qualifier {@code @LoadBalanced} để không nhầm sang builder thường.</p>
 */
@Configuration
public class AccountClientConfig {

    /** Builder có gắn cơ chế load-balancing (đánh dấu bằng @LoadBalanced). */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    /** Proxy AccountApi dựng trên RestClient load-balanced; host = TÊN service. */
    @Bean
    public AccountApi accountApi(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://account-service") // "account-service" là TÊN đăng ký ở Eureka
                .requestFactory(timeoutRequestFactory()) // BƯỚC 7: timeout ở tầng HTTP client
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(AccountApi.class);
    }

    /**
     * Timeout cho lời gọi HTTP (thay cho @TimeLimiter — cái đó chỉ hợp lời gọi bất đồng bộ).
     * connect = thời gian chờ MỞ kết nối; read = thời gian chờ account-service TRẢ LỜI. Quá hạn
     * -> ném lỗi I/O -> adapter dịch thành AccountUnavailableException -> retry/circuit breaker xử lý.
     * Dùng SimpleClientHttpRequestFactory (JDK thuần) cho ổn định qua các phiên bản.
     */
    private SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        return factory;
    }
}
