package com.hdl.mt.transfer.client;

/**
 * Lỗi HẠ TẦNG khi gọi account-service: account trả 5xx, hết thời gian chờ (timeout), không
 * kết nối được, hoặc circuit breaker đang mở.
 *
 * <p><b>Vì sao tách riêng:</b> đây là loại lỗi TẠM THỜI, có khả năng thành công nếu thử lại
 * (retry) và là dấu hiệu account-service đang trục trặc — nên ĐƯỢC retry và ĐƯỢC tính vào tỉ
 * lệ lỗi để circuit breaker quyết định mở/đóng.</p>
 */
public class AccountUnavailableException extends AccountClientException {

    public AccountUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
