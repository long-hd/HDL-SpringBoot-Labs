package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.AccountResponse;
import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cùng các lời gọi account-service, nhưng khai bằng OpenFeign — để đặt cạnh
 * {@link com.hdl.mt.account.api.AccountApi} (HTTP Interface) mà so sánh.
 *
 * <p>Khác biệt đáng chú ý giữa hai cách (đây là điểm học của bước 2):</p>
 * <ul>
 *   <li><b>Annotation:</b> OpenFeign tái dùng annotation Spring MVC ({@code @PostMapping},
 *       {@code @PathVariable}); HTTP Interface dùng annotation riêng của spring-web
 *       ({@code @PostExchange}...). HTTP Interface là core Spring, không cần thư viện ngoài.</li>
 *   <li><b>Vị trí đặt:</b> {@code @FeignClient} là annotation dành riêng cho client, nên
 *       interface Feign thường khai ở phía consumer (ở đây), KHÔNG đặt trong module
 *       contract dùng chung. Vì thế ta phải khai lại các lời gọi ở đây — chấp nhận được vì
 *       payload (DTO) vẫn dùng chung từ account-service-api (cách A).</li>
 *   <li><b>URL:</b> BƯỚC 3 đã BỎ thuộc tính {@code url}. Chỉ còn {@code name="account-service"}
 *       — Feign tự hỏi Eureka địa chỉ các instance rồi load-balance, giống hệt phía HTTP
 *       Interface nhưng KHÔNG phải khai {@code @LoadBalanced} tay: OpenFeign tích hợp
 *       discovery/LB sẵn. Đây là điểm Feign gọn hơn HTTP Interface trên Boot 3.5.</li>
 * </ul>
 *
 * <p>Lưu ý về hướng của Spring: OpenFeign được coi feature-complete từ Spring Cloud 2022.0.0;
 * Spring khuyến nghị HTTP Interface cho dự án mới. OpenFeign vẫn dùng tốt và rất phổ biến
 * trong JD/codebase thực tế, nên biết cả hai là hợp lý.</p>
 */
@FeignClient(name = "account-service")
public interface AccountFeignClient {

    @PostMapping("/accounts/{id}/debit")
    AccountResponse debit(@PathVariable("id") Long id, @RequestBody DebitRequest request);

    @PostMapping("/accounts/{id}/credit")
    AccountResponse credit(@PathVariable("id") Long id, @RequestBody CreditRequest request);
}
