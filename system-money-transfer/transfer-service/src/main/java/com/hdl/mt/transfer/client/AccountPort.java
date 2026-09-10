package com.hdl.mt.transfer.client;

import java.math.BigDecimal;

/**
 * Cổng (port) mà transfer-service dùng để tác động lên tài khoản.
 *
 * <p>BƯỚC 8: mỗi thao tác mang thêm {@code operationId} — khóa idempotency. transfer-service
 * sinh khóa này MỘT LẦN cho mỗi bước của Saga; nếu resilience retry gọi lại, khóa giữ nguyên
 * (vì là tham số cố định trước khi vào retry) nên account-service nhận ra và không áp dụng lần hai.</p>
 *
 * <p>Vẫn giữ vai trò tách TransferService khỏi cơ chế client + resilience: TransferService chỉ
 * gọi debit/credit với một khóa, không cần biết HTTP Interface hay Feign, có retry hay không.</p>
 */
public interface AccountPort {

    /** Trừ {@code amount} khỏi {@code accountId}, gắn khóa idempotency {@code operationId}. */
    void debit(Long accountId, BigDecimal amount, String operationId);

    /** Cộng {@code amount} vào {@code accountId}, gắn khóa idempotency {@code operationId}. */
    void credit(Long accountId, BigDecimal amount, String operationId);
}
