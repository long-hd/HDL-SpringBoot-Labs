package com.hdl.mt.account.exception;

/**
 * Ném ra khi không tìm thấy tài khoản theo id.
 *
 * <p>Được map sang HTTP 404 (Not Found) ở {@code GlobalExceptionHandler}.</p>
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super("Không tìm thấy tài khoản với id = " + accountId);
    }
}
