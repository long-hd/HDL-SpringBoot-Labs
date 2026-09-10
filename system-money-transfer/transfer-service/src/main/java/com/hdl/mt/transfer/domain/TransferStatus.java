package com.hdl.mt.transfer.domain;

/**
 * Trạng thái một lệnh chuyển tiền — mở rộng ở bước 8 để mô tả luồng Saga có bù trừ.
 */
public enum TransferStatus {

    /** Vừa tạo, chưa xử lý xong. */
    PENDING,

    /** Đã trừ nguồn và cộng đích thành công. */
    COMPLETED,

    /** Không hoàn tất và KHÔNG để lại hậu quả cần bù (ví dụ trừ nguồn đã fail ngay). */
    FAILED,

    /** Đã trừ nguồn nhưng cộng đích fail -> đang chạy bù trừ (hoàn tiền về nguồn). */
    COMPENSATING,

    /** Đã bù trừ xong: tiền đã hoàn lại nguồn, hệ thống về trạng thái nhất quán. */
    COMPENSATED,

    /** Bù trừ cũng fail -> tiền đang kẹt ở nguồn, CẦN can thiệp/đối soát (reconcile). */
    COMPENSATION_FAILED
}
