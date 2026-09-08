package com.hdl.mt.transfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Config nghiệp vụ của transfer-service, lấy từ Config Server (file
 * {@code config-repo/transfer-service.yml}, prefix {@code transfer}).
 *
 * <p><b>Vì sao KHÔNG cần {@code @RefreshScope} ở đây:</b> bean kiểu
 * {@code @ConfigurationProperties} được Spring Cloud tự động "rebind" (nạp lại) mỗi khi có
 * {@code POST /actuator/refresh} — đây là điểm nhiều người hiểu nhầm là phải gắn
 * {@code @RefreshScope}. {@code @RefreshScope} chỉ cần cho bean dùng {@code @Value} hoặc bean
 * thường muốn được tạo lại khi refresh.</p>
 *
 * <p>Có giá trị mặc định phòng khi Config Server chưa phát được (vì client dùng
 * {@code optional:configserver}), để service vẫn chạy an toàn thay vì null.</p>
 */
@ConfigurationProperties(prefix = "transfer")
public class TransferProperties {

    /**
     * Hạn mức số tiền tối đa cho một lần chuyển. Tên property dạng kebab
     * {@code transfer.max-amount-per-transaction} tự khớp field này (relaxed binding).
     */
    private BigDecimal maxAmountPerTransaction = new BigDecimal("100000000");

    public BigDecimal getMaxAmountPerTransaction() {
        return maxAmountPerTransaction;
    }

    public void setMaxAmountPerTransaction(BigDecimal maxAmountPerTransaction) {
        this.maxAmountPerTransaction = maxAmountPerTransaction;
    }
}
