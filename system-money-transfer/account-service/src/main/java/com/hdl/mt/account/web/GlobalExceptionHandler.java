package com.hdl.mt.account.web;

import com.hdl.mt.account.exception.AccountNotFoundException;
import com.hdl.mt.account.exception.InsufficientBalanceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Chuyển exception nghiệp vụ thành phản hồi HTTP có mã đúng nghĩa.
 *
 * <p>Vì sao việc chọn mã HTTP quan trọng: bên gọi (transfer-service) sẽ dựa vào mã này
 * để quyết định hành xử. Ta phân tách rõ ba nhóm:</p>
 * <ul>
 *   <li>404 — không tìm thấy tài khoản (yêu cầu sai đối tượng).</li>
 *   <li>409 — vi phạm quy tắc nghiệp vụ (không đủ số dư): yêu cầu hợp lệ về mặt kỹ
 *       thuật nhưng không thực hiện được lúc này.</li>
 *   <li>400 — dữ liệu vào không hợp lệ (số tiền &le; 0).</li>
 * </ul>
 * Nhờ tách bạch, transfer-service phân biệt được "lỗi có thể đoán trước" (4xx) với
 * "hệ thống trục trặc" (5xx) — điều này sẽ rất quan trọng ở bước resilience/saga.
 *
 * <p>Dùng {@link ProblemDetail} (chuẩn RFC 7807) làm khuôn phản hồi lỗi thay vì tự chế
 * body — đây là cách Spring 6 khuyến nghị.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleNotFound(AccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficient(InsufficientBalanceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadInput(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
