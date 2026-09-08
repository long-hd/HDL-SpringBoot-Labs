package com.hdl.mt.transfer.web;

import com.hdl.mt.transfer.config.TransferProperties;
import com.hdl.mt.transfer.domain.Transfer;
import com.hdl.mt.transfer.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Cổng HTTP của transfer-service.
 *
 * <p>Chú ý ranh giới lỗi: nếu lệnh chuyển KHÔNG hoàn tất vì lý do nghiệp vụ (nguồn thiếu
 * tiền...), bước 0 vẫn trả HTTP 200 kèm một Transfer có {@code status=FAILED} và
 * {@code failureReason}. Nghĩa là "đã xử lý xong lệnh, kết quả là thất bại" — khác với
 * lỗi 4xx/5xx (không xử lý được). Cách phân biệt này sẽ được tinh chỉnh ở các bước sau.</p>
 */
@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransferProperties properties;

    public TransferController(TransferService transferService, TransferProperties properties) {
        this.transferService = transferService;
        this.properties = properties;
    }

    @PostMapping
    public TransferResponse createTransfer(@RequestBody TransferRequest request) {
        Transfer transfer = transferService.transfer(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount());
        return TransferResponse.from(transfer);
    }

    /**
     * Xem hạn mức chuyển tiền hiện hành. Dùng để kiểm chứng refresh nóng: sửa giá trị ở
     * Config Server -> POST /actuator/refresh -> gọi lại endpoint này thấy số ĐỔI mà không restart.
     */
    @GetMapping("/limit")
    public BigDecimal currentLimit() {
        return properties.getMaxAmountPerTransaction();
    }

    /** Dữ liệu vào không hợp lệ (ví dụ nguồn trùng đích) -> 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadInput(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
