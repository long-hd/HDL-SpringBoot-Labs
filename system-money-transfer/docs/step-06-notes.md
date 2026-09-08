# Bước 6 — Config Server (config tập trung + refresh nóng)

Mục tiêu: đưa config NGHIỆP VỤ ra một chỗ tập trung, đổi được lúc chạy mà không rebuild/restart.
Ví dụ: hạn mức số tiền tối đa mỗi lần chuyển của transfer-service.

## Điểm cốt lõi — KHÔNG chuyển hết yml ra Config Server

Chia đôi config:
- **Bootstrap (ở lại `application.yml` local)**: tên app, port, địa chỉ eureka + config server,
  datasource. Đây là thứ service cần để KHỞI ĐỘNG và tự tìm được Config Server. Không thể đưa
  địa chỉ Config Server vào chính Config Server (vòng luẩn quẩn).
- **Nghiệp vụ đổi lúc chạy (đưa ra Config Server)**: hạn mức chuyển tiền, timeout, feature flag...

Config Server KHÔNG tự chứa config — nó đọc từ một BACKEND rồi phát. Bước 6 dùng backend
`native` = đọc file từ thư mục local `config-repo/`.

## Đã thêm gì

- **config-server** (:8888): `@EnableConfigServer`, `spring.profiles.active=native`,
  `search-locations` trỏ tới `config-repo/`. Phát file theo `/{application}/{profile}`.
- **config-repo/transfer-service.yml**: chứa `transfer.max-amount-per-transaction: 50000000`.
- **transfer-service**:
  - `spring-cloud-starter-config` + `actuator`.
  - `spring.config.import=optional:configserver:http://localhost:8888` (cơ chế Boot 2.4+; KHÔNG
    dùng bootstrap.yml cũ). `optional:` để service vẫn chạy nếu Config Server chưa lên.
  - `TransferProperties` (`@ConfigurationProperties(prefix="transfer")`) — KHÔNG cần
    `@RefreshScope`: bean `@ConfigurationProperties` tự rebind khi `/actuator/refresh`.
  - Luật nghiệp vụ mới trong `TransferService`: amount > hạn mức -> từ chối (400) ngay từ đầu.
  - Endpoint `GET /transfers/limit` để xem hạn mức hiện hành (tiện kiểm chứng refresh).

## Vì sao có 2 đường trong search-locations

`spring-boot:run` có thể chạy với working directory là thư mục gốc dự án HOẶC thư mục
config-server (tuỳ cách gọi). Nên khai cả `file:./config-repo` và `file:./config-server/config-repo`;
đường nào không tồn tại thì bỏ qua. File thật đặt ở `config-server/config-repo/transfer-service.yml`.
(Cố ý dùng filesystem, KHÔNG để trong `src/main/resources` — vì file trong resources sẽ bị
đóng vào target/jar, sửa xong phải rebuild, mất ý nghĩa demo đổi-nóng.)

## Cách tự kiểm chứng refresh nóng (khi chạy ở máy)

1. Chạy: docker compose up -d -> discovery -> **config-server** -> account -> transfer -> gateway.
2. Xem hạn mức hiện tại: `curl http://localhost:8082/transfers/limit` -> `50000000`.
3. Chuyển 60.000.000 (vượt) -> transfer từ chối 400 "vượt hạn mức".
4. Sửa `config-server/config-repo/transfer-service.yml`: đổi max-amount thành `100000000`.
5. Nạp lại KHÔNG restart: `curl -X POST http://localhost:8082/actuator/refresh`.
6. `curl http://localhost:8082/transfers/limit` -> giờ là `100000000`. Chuyển 60.000.000 -> qua.
   => Đổi config lúc chạy, không rebuild/restart. Đó là điểm của Config Server.

(Có thể xem Config Server phát gì trực tiếp: `curl http://localhost:8888/transfer-service/default`.)

## Known gaps ghi nhận ở bước này (từ bước 5)

- **Gateway bị bypass**: port service (8081/8082) vẫn gọi thẳng được -> rate-limit/auth ở
  gateway vô hiệu nếu client đi cửa sau. Cách đúng: chặn ở tầng mạng/deploy (K8s ClusterIP,
  chỉ gateway expose), KHÔNG soi header ở mỗi service. -> xử lý ở bước 12.
- **Route mỗi service một khối**: nhiều service sẽ dài; có thể bật Discovery Locator để tự sinh
  route, đánh đổi mất tinh chỉnh per-route. Hiện giữ khai tường minh.

## Còn để dành

- "Tiền kẹt" (Saga, bước 8), idempotency, retry/circuit-breaker (bước 7), tracing (bước 10),
  security (bước 11), K8s (bước 12).

> Chưa build/chạy trong môi trường tạo file. Build + chạy ở máy; điểm dễ vấp: đường dẫn
> search-locations của config-server (đã khai 2 đường để chắc) và thứ tự khởi động (config-server
> trước transfer-service, dù `optional:` cho phép không chết nếu thiếu). Vướng gì gửi log.
