package com.hdl.mt.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của transfer-service (cổng 8082, DB riêng transfer_db).
 *
 * <p>transfer-service không tự giữ tiền — nó ĐIỀU PHỐI: gọi account-service để trừ ở
 * tài khoản nguồn và cộng ở tài khoản đích. Đây là service sẽ tiến hoá thành "bộ điều
 * phối Saga" ở bước 8.</p>
 */
@SpringBootApplication
public class TransferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferServiceApplication.class, args);
    }
}
