package com.hdl.mt.transfer.client;

/**
 * Lỗi NGHIỆP VỤ khi gọi account-service (account trả 4xx): số dư không đủ (409), không tìm
 * thấy tài khoản (404), dữ liệu sai (400)...
 *
 * <p><b>Vì sao tách riêng khỏi lỗi hạ tầng:</b> đây là loại lỗi mà gọi lại BAO NHIÊU LẦN cũng
 * vẫn hỏng (tài khoản thiếu tiền thì retry vô nghĩa), và nó KHÔNG phản ánh account-service
 * đang trục trặc — nên KHÔNG được retry và KHÔNG được tính vào tỉ lệ lỗi làm mở circuit
 * breaker. Cấu hình Resilience4j sẽ "ignore" loại exception này.</p>
 */
public class AccountBusinessException extends AccountClientException {

    public AccountBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
