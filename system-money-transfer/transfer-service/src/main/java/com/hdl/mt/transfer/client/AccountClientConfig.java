package com.hdl.mt.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Tạo một {@link RestClient} đã gắn sẵn địa chỉ gốc của account-service.
 *
 * <p>{@link RestClient} là HTTP client đồng bộ (blocking) hiện đại của Spring 6, thay cho
 * {@code RestTemplate} cũ. Bước 0 dùng nó với URL CỨNG (đọc từ property
 * {@code account-service.base-url}).</p>
 *
 * <p><b>Đây chính là điểm yếu mà các bước sau sẽ vá:</b> địa chỉ đang bị đóng cứng
 * ({@code http://localhost:8081}). Trong thực tế, account-service có thể có nhiều bản
 * (instance) với IP đổi liên tục. Vì vậy:
 * <ul>
 *   <li>Bước 2 sẽ thay lời gọi thủ công bằng một interface khai báo (HTTP Interface/OpenFeign).</li>
 *   <li>Bước 3 sẽ thay URL cứng bằng gọi theo TÊN service qua service discovery.</li>
 *   <li>Bước 4 sẽ chia tải giữa nhiều instance.</li>
 * </ul>
 * Giữ URL cứng ở bước 0 là cố ý — để thấy rõ vấn đề trước khi thấy lời giải.</p>
 */
@Configuration
public class AccountClientConfig {

    @Bean
    public RestClient accountRestClient(@Value("${account-service.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
