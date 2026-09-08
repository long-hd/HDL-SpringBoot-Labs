package com.hdl.mt.account.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Hợp đồng gọi account-service, khai báo kiểu "HTTP Interface" của Spring 6.
 *
 * <p>Đây là bước tiến của cách A so với bước 0: ngoài DTO, ta chia sẻ luôn một INTERFACE
 * mô tả các lời gọi. Ở phía client (transfer-service), Spring sinh sẵn một proxy hiện thực
 * interface này bằng {@code HttpServiceProxyFactory} — ta gọi method Java, Spring lo phần
 * HTTP. Không phải viết tay URL/method/parse như bước 0.</p>
 *
 * <p><b>Vì sao interface này CHỈ dùng ở phía client (bước 2):</b> việc để controller server
 * implement chính interface {@code @HttpExchange} này (một interface, hai đầu dùng) là tính
 * năng được Spring nhấn mạnh cho Spring Framework 7 / Spring Boot 4. Repo đang ở Boot 3.5
 * (Spring Framework 6.2), nên account-service vẫn giữ controller {@code @PostMapping} riêng.
 * Khi nào nâng lên Boot 4 có thể cho controller implement interface này để đồng bộ hai đầu.</p>
 *
 * <p>{@code @HttpExchange} là annotation trung lập (thuộc spring-web), không gắn với OpenFeign —
 * nên interface này cũng minh hoạ được ưu điểm "không phụ thuộc annotation của một thư viện
 * client cụ thể".</p>
 */
@HttpExchange("/accounts")
public interface AccountApi {

    /** Trừ tiền tài khoản {id}. */
    @PostExchange("/{id}/debit")
    AccountResponse debit(@PathVariable Long id, @RequestBody DebitRequest request);

    /** Cộng tiền tài khoản {id}. */
    @PostExchange("/{id}/credit")
    AccountResponse credit(@PathVariable Long id, @RequestBody CreditRequest request);

    /** Xem thông tin tài khoản {id}. */
    @GetExchange("/{id}")
    AccountResponse getAccount(@PathVariable Long id);
}
