package com.hdl.mt.transfer.domain;

/**
 * Trạng thái một lệnh chuyển tiền.
 *
 * <p>Bước 0 chỉ dùng ba trạng thái đơn giản. Ở bước 8 (Saga) tập trạng thái sẽ giàu hơn
 * để mô tả các bước trung gian và bù trừ (ví dụ: đã-trừ-nguồn, đang-bù-trừ...).</p>
 */
public enum TransferStatus {

    /** Vừa tạo, chưa xử lý xong. */
    PENDING,

    /** Đã trừ nguồn và cộng đích thành công. */
    COMPLETED,

    /** Không hoàn tất (nguồn không đủ tiền, hoặc lỗi giữa chừng). */
    FAILED
}
