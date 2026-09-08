package com.hdl.mt.transfer.client;

/**
 * Lỗi khi gọi account-service không thành công (dù vì lý do nghiệp vụ như thiếu số dư,
 * hay lý do hạ tầng như account-service chết/mạng đứt).
 *
 * <p>Bước 2 chưa cần phân biệt hai loại đó — {@code TransferService} chỉ cần biết "lời gọi
 * này hỏng". Bước 7 (resilience) sẽ tinh chỉnh: phân biệt lỗi nghiệp vụ (không retry) với
 * lỗi hạ tầng tạm thời (nên retry) để circuit breaker/retry xử lý đúng.</p>
 */
public class AccountClientException extends RuntimeException {

    public AccountClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
