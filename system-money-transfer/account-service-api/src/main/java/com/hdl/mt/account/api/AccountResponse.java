package com.hdl.mt.account.api;

import java.math.BigDecimal;

/**
 * Dữ liệu một tài khoản khi trả về cho bên gọi (client).
 *
 * <p>Đây là một phần của "hợp đồng" account-service phơi ra. Vì nằm trong module
 * {@code account-service-api} dùng chung, nên cả account-service (bên tạo dữ liệu)
 * lẫn transfer-service (bên đọc dữ liệu) đều nhìn thấy đúng một định nghĩa này.</p>
 *
 * <p>Dùng {@link BigDecimal} cho số tiền — KHÔNG bao giờ dùng double/float cho tiền,
 * vì số thực nhị phân làm tròn sai (ví dụ 0.1 + 0.2 != 0.3), không chấp nhận được
 * trong nghiệp vụ tài chính.</p>
 *
 * @param id        định danh tài khoản
 * @param ownerName tên chủ tài khoản
 * @param balance   số dư hiện tại
 */
public record AccountResponse(
        Long id,
        String ownerName,
        BigDecimal balance
) {
}
