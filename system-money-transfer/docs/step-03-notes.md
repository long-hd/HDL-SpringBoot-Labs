# Bước 3 — Service Discovery (Eureka)

Mục tiêu: bỏ URL cứng. account-service có thể có nhiều bản, IP/port đổi — không hardcode được.
Ta thêm một "danh bạ" (Eureka): service tự đăng ký khi khởi động, service khác tra theo TÊN.

## Cơ chế (why)

```
account-service khởi động ──► đăng ký "account-service @ ip:8081" vào Eureka
transfer-service khởi động ──► đăng ký + kéo danh sách service về, cache lại
transfer-service gọi "account-service" ──► LoadBalancer hỏi danh sách instance
                                        ──► chọn 1 instance ──► gọi địa chỉ thật
heartbeat định kỳ: service báo "còn sống"; ngừng heartbeat quá lâu ──► Eureka loại khỏi danh bạ
```

Điểm cốt lõi: transfer-service KHÔNG còn biết account-service ở đâu; nó chỉ biết TÊN, phần
"tên -> địa chỉ thật" do Eureka + LoadBalancer lo. Đây là điều kiện cần để scale (bước 4).

## Đã thêm/đổi gì

- **discovery-server** (mới, :8761): `@EnableEurekaServer`, chạy standalone
  (`register-with-eureka=false`, `fetch-registry=false` vì nó chính là registry).
- **account-service**: thêm `spring-cloud-starter-netflix-eureka-client` + trỏ
  `eureka.client.service-url.defaultZone`. Không cần loadbalancer vì nó không gọi ai. Tên
  đăng ký = `spring.application.name` = `account-service`.
- **transfer-service**: thêm eureka-client + `spring-cloud-starter-loadbalancer`. Bỏ hẳn
  property URL cứng.
  - **Feign**: `@FeignClient(name="account-service")` — bỏ `url`. Feign tự discovery + LB. Xong.
  - **HTTP Interface**: phải khai TAY `@LoadBalanced RestClient.Builder`, rồi dựng proxy với
    `baseUrl("http://account-service")` (host = tên service). LoadBalancer chặn request có host
    là tên service và thay bằng instance thật.

## Đây là chỗ hai client PHÂN KỲ (bài học chính của bước 3)

Ở bước 2, HTTP Interface và Feign "cho kết quả như nhau". Bước 3 mới lộ khác biệt thật:

| | Feign | HTTP Interface (Boot 3.5) |
|---|---|---|
| Nối discovery/LB | Tự động, chỉ cần bỏ `url` | Phải tự khai `@LoadBalanced RestClient.Builder` |
| Lý do | OpenFeign tích hợp LB sẵn | SF 6.2 chưa auto; Spring Cloud 2025.1/Boot 4 mới auto |

Kết luận rút ra được (đáng nói khi phỏng vấn): "cùng gọi được service" không có nghĩa hai cơ
chế tương đương — khác biệt nằm ở tích hợp hệ sinh thái (discovery, LB, sau này là circuit
breaker, tracing, security). Chọn client phải tính tới bối cảnh phiên bản.

Căn cứ: `@LoadBalanced` dùng được với `RestClient.Builder` theo tài liệu Spring Cloud Commons
(mục Common Abstractions / LoadBalancer). Auto-config LB cho HTTP Interface là điểm của Spring
Cloud 2025.1 (Boot 4) — repo đang ở 2025.0 (Boot 3.5) nên phải khai tay.

## Cách tự kiểm chứng (khi chạy ở máy)

1. Chạy theo thứ tự: discovery-server -> account-service -> transfer-service.
2. Mở http://localhost:8761 — thấy `ACCOUNT-SERVICE` và `TRANSFER-SERVICE` trong danh sách.
3. Chuyển tiền như cũ (mặc định http-interface): vẫn chạy, nhưng giờ đi qua discovery, không
   còn localhost:8081 trong cấu hình transfer-service.
4. Đổi `account-service.client: feign`, khởi động lại transfer-service: vẫn chạy đúng.
5. (Xem trước bước 4) Chạy thêm một instance account-service ở cổng khác
   (`mvn -pl account-service spring-boot:run -Dspring-boot.run.arguments=--server.port=8083`),
   cả hai cùng đăng ký tên `account-service` -> LoadBalancer chia request giữa hai instance.

## Lỗ hổng cố ý vẫn còn

- "Tiền kẹt" (Saga, bước 8), chưa idempotency, chưa retry/circuit-breaker (bước 7),
  chưa API gateway (bước 5), chưa config tập trung (bước 6).

> Chưa build/chạy trong môi trường tạo file. Build + chạy ở máy; nghi ngờ nhất là phần
> `@LoadBalanced RestClient.Builder` (HTTP Interface) — nếu lỗi gửi stacktrace.
