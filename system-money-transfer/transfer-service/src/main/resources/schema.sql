-- Bảng ShedLock dùng để giữ khóa phân tán cho job reconcile (mỗi lượt chỉ 1 node chạy).
-- ShedLock KHÔNG tự tạo bảng này (nó không phải JPA entity), nên ta tạo tay ở đây.
-- IF NOT EXISTS để chạy lại nhiều lần không lỗi. Cấu trúc theo tài liệu ShedLock cho PostgreSQL.
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
