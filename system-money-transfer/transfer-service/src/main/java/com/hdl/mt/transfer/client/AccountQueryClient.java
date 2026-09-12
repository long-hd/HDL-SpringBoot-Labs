package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.OperationStatus;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client CHỈ-ĐỌC hỏi account-service "operationId đã xử lý chưa?" — dùng cho reconcile.
 *
 * <p>Cố ý TÁCH khỏi {@link AccountPort} (luồng chuyển tiền): đây là truy vấn nền, không cần đi
 * qua circuit breaker/retry của luồng nóng. Nếu account-service không trả lời, lời gọi ném lỗi ->
 * reconcile bỏ qua lệnh đó lượt này, lượt sau chạy lại (job vốn định kỳ).</p>
 *
 * <p>Vẫn gọi theo TÊN service qua {@code @LoadBalanced} builder (tái dùng bean từ
 * AccountClientConfig), nên độc lập với việc luồng chính đang dùng HTTP Interface hay Feign.</p>
 */
@Component
public class AccountQueryClient {

    private final RestClient restClient;

    public AccountQueryClient(@LoadBalanced RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restClient = builder
                .baseUrl("http://account-service")
                .requestFactory(factory)
                .build();
    }

    /** Trả true nếu account-service đã xử lý operationId này (đã trừ/cộng tương ứng). */
    public boolean isProcessed(String operationId) {
        OperationStatus status = restClient.get()
                .uri("/operations/{id}", operationId)
                .retrieve()
                .body(OperationStatus.class);
        return status != null && status.processed();
    }
}
