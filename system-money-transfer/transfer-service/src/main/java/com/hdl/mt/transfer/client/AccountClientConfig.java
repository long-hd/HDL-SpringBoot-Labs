package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.AccountApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Tạo proxy {@link AccountApi} cho phía HTTP Interface.
 *
 * <p>Luồng: dựng một {@link RestClient} gắn địa chỉ gốc account-service (BƯỚC 2 vẫn dùng
 * URL CỨNG, bước 3 sẽ thay bằng gọi theo tên qua discovery) -> bọc bằng
 * {@code RestClientAdapter} -> {@code HttpServiceProxyFactory} sinh ra một hiện thực của
 * interface {@link AccountApi}. Từ đó gọi {@code api.debit(...)} như gọi method thường,
 * Spring tự dịch thành lời gọi HTTP.</p>
 *
 * <p>So với bước 0 (tự viết {@code restClient.post().uri(...).body(...).retrieve()}), cách
 * này khai báo (declarative): interface mô tả "gọi gì", không lặp code HTTP ở mỗi lời gọi.</p>
 */
@Configuration
public class AccountClientConfig {

    @Bean
    public AccountApi accountApi(@Value("${account-service.base-url}") String baseUrl) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(AccountApi.class);
    }
}
