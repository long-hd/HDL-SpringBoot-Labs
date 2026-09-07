-- Tạo hai database tách biệt cho hai service.
-- Script này được Postgres chạy tự động lần đầu khi container khởi tạo (thư mục
-- /docker-entrypoint-initdb.d). Nếu volume đã có dữ liệu từ trước, script sẽ KHÔNG chạy
-- lại — muốn chạy lại thì xoá volume: `docker compose down -v`.
CREATE DATABASE account_db;
CREATE DATABASE transfer_db;
