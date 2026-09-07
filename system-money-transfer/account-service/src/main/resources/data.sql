-- Seed vài tài khoản mẫu để thử luồng chuyển tiền ngay mà không cần API tạo tài khoản
-- (bước 0 chưa có endpoint tạo tài khoản).
--
-- ON CONFLICT DO NOTHING: để chạy lại nhiều lần (sql.init.mode=always) không lỗi trùng
-- khoá chính. Cú pháp này là của PostgreSQL.
INSERT INTO account (id, owner_name, balance) VALUES
    (1, 'Nguyen Van A', 1000000.00),
    (2, 'Tran Thi B',    500000.00),
    (3, 'Le Van C',           0.00)
ON CONFLICT (id) DO NOTHING;
