# Bước 5 — API Gateway (route + rate limit)

Mục tiêu: một CỬA VÀO duy nhất cho client, và demo một cross-cutting concern (rate limit) đặt
ở gateway thay vì lặp ở từng service.

## Vì sao cần gateway (không chỉ để route)

Nếu chỉ để route thuần thì client tra discovery trực tiếp cũng được — gateway sẽ thừa. Giá trị
thật là gom các mối quan tâm CHUNG về một chỗ: rate limit, auth (bước 11), CORS, logging,
che giấu topology nội bộ. Đặt một lần ở gateway thay vì nhét vào mọi service.

## Đã thêm gì

- **api-gateway** (:8080), Spring Cloud Gateway (WebFlux/reactive):
  - Route: `/accounts/**` -> `lb://account-service`, `/transfers/**` -> `lb://transfer-service`
    (`lb://` = theo tên qua Eureka + LoadBalancer).
  - Filter `RequestRateLimiter` (RedisRateLimiter, token bucket) trên cả hai route.
- **Redis** thêm vào docker-compose — làm store cho token bucket.
- Client giờ gọi qua **:8080** thay vì gọi thẳng :8081/:8082.

## Hai điểm về phiên bản (có dẫn chứng, dễ vấp)

1. **Tên artifact đổi (Spring Cloud 2025.0):** dùng `spring-cloud-starter-gateway-server-webflux`.
   Tên cũ `spring-cloud-starter-gateway` vẫn chạy nhưng in warning (deprecated). Prefix cấu
   hình cũng đổi: `spring.cloud.gateway.*` -> `spring.cloud.gateway.server.webflux.*`.
2. **Gateway là reactive (WebFlux + Netty):** KHÔNG thêm `spring-boot-starter-web` (servlet)
   vào module gateway — hai web stack xung đột. Vì thế RedisRateLimiter phải dùng bản
   `spring-boot-starter-data-redis-reactive` (không phải bản blocking).

## Vì sao rate limit cần Redis (bài học)

Token bucket phải đếm "IP này đã dùng bao nhiêu token". Khi chạy NHIỀU instance gateway, nếu
mỗi instance đếm trong bộ nhớ riêng thì hạn mức thật bị nhân lên theo số instance (mỗi instance
cho qua 4 req/giây -> 3 instance thành 12). Redis là store DÙNG CHUNG để mọi instance đếm vào
cùng một bucket. Đây cùng một nguyên lý với "state phải shared khi scale ngang" (giống lý do
WebSocket multi-node cần broker chung).

`KeyResolver` quyết định "đếm theo ai" — ở đây theo IP client (`ipKeyResolver` bean). Có thể
đổi sang theo user/principal (khi có auth ở bước 11) hoặc theo API key.

## Cách tự kiểm chứng (khi chạy ở máy)

1. Chạy: docker compose up -d (có Postgres + Redis) -> discovery -> account -> transfer -> gateway.
2. Chuyển tiền qua gateway: `POST http://localhost:8080/transfers` -> vẫn COMPLETED như trước,
   nhưng giờ đi qua cửa 8080.
3. Rate limit: bắn dồn > 4 request/giây tới `http://localhost:8080/accounts/1`:
   ```bash
   for i in $(seq 1 12); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/accounts/1; done
   ```
   Vài request đầu trả 200, sau đó xuất hiện **429** (Too Many Requests). Header phản hồi có
   `X-RateLimit-Remaining` giảm dần.
4. Tắt Redis (`docker stop mt-redis`) rồi gọi lại -> thấy gateway lỗi khi áp rate limit ->
   chứng minh rate limit phụ thuộc Redis.

## Lỗ hổng / phần còn để dành

- "Tiền kẹt" (Saga, bước 8), chưa idempotency, chưa retry/circuit-breaker (bước 7),
  chưa config tập trung (bước 6), chưa auth ở gateway (bước 11).
- Rate limit đang đặt thấp (2/4) cho dễ demo — số thật chỉnh theo nhu cầu.

> Chưa build/chạy trong môi trường tạo file. Build + chạy ở máy; điểm dễ vấp nhất bước này là
> tên artifact/prefix gateway (2025.0) và việc gateway phải là reactive (không kèm starter-web).
> Nếu routes không active hoặc rate limit không kích hoạt, gửi log tôi rà.
