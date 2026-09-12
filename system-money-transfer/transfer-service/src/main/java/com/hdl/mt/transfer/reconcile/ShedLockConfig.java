package com.hdl.mt.transfer.reconcile;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Bật lịch chạy ({@code @EnableScheduling}) + khóa phân tán ShedLock.
 *
 * <p>Vì sao cần: {@code @Scheduled} là lịch CỤC BỘ của mỗi JVM — chạy nhiều instance
 * transfer-service thì cả mấy node cùng chạy reconcile một lúc, trùng việc. ShedLock bắt node
 * phải GIÀNH một khóa (lưu ở bảng shedlock trong Postgres) trước khi chạy; chỉ node giành được
 * mới chạy, các node khác bỏ lượt. Khóa có hạn ({@code lockAtMostFor}) nên node giữ khóa mà chết
 * thì khóa tự nhả, node khác tiếp quản lượt sau.</p>
 *
 * <p>ShedLock chỉ GIẢM xác suất chạy trùng (khóa vẫn có kẽ hở khi hết hạn lúc node cũ chưa chết
 * hẳn). Lớp phòng thủ cuối vẫn là IDEMPOTENCY của hành động: bù trừ dùng compensateOpId ổn định
 * nên lỡ chạy trùng cũng không hoàn tiền hai lần. Lock + idempotency là hai lớp, dùng cả hai.</p>
 *
 * <p>Dùng {@code usingDbTime()} để mọi node dựa trên ĐỒNG HỒ CỦA DB (tránh lệch giờ giữa các máy).</p>
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}
