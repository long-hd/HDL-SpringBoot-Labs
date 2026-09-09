# Kế hoạch dựng & tiến độ — system-money-transfer

Dựng theo kiểu incremental: mỗi bước thêm ĐÚNG một mảnh vào cùng một hệ, để mỗi khái niệm
có chỗ bấu víu. Bản đồ này để đối chiếu về sau "đã làm tới đâu, vì sao chọn thế".

## Tiến độ

| Bước | Nội dung | Module/mảnh | Trạng thái |
|---|---|---|---|
| 0 | Khung tối giản: account + transfer, gọi nhau bằng REST + URL cứng, happy-path | account-service, transfer-service, account-service-api | ✅ Xong |
| 1 | Nền distributed systems (why) | (lý thuyết) | ✅ Ghi note (`m1-distributed-systems-notes.md`) |
| 2 | Thay lời gọi thủ công bằng interface khai báo (cả HTTP Interface lẫn OpenFeign, swap qua config) | HTTP Interface + OpenFeign, cổng AccountPort | ✅ Xong |
| 3 | Service discovery — gọi theo tên, bỏ URL cứng (cả Feign lẫn HTTP Interface) | discovery-server (Eureka) + LoadBalancer | ✅ Xong |
| 4 | Load balancing giữa nhiều instance | Spring Cloud LoadBalancer | ✅ Không code mới — LB đã cấu hình ở bước 3; xác nhận bằng chạy 2 instance account-service |
| 5 | API Gateway — một cửa vào + demo rate limit | api-gateway (Gateway WebFlux) + Redis | ✅ Xong |
| 6 | Config tập trung + refresh nóng (hạn mức chuyển tiền) | config-server (native) | ✅ Xong |
| 7 | Resilience — circuit breaker + retry + timeout (bọc quanh AccountPort) | Resilience4j | ✅ Xong |
| 8 | **Saga + outbox + idempotency** (vá lỗ hổng bước 0) | transfer-service (orchestrator) | ⬜ Chưa |
| 9 | Async event | Kafka | ⬜ Chưa |
| 10 | Observability — tracing/metrics/log | Micrometer Tracing + Zipkin | ⬜ Chưa |
| 11 | Security phân tán | OAuth2/JWT ở gateway | ⬜ Chưa |
| 12 | Đóng gói lên K8s | minikube | ⬜ Chưa |
| (sau) | Sổ cái ghi kép + reconciliation (fintech nâng cao, tuỳ chọn) | ledger-service | ⬜ Tuỳ chọn |

## Quyết định đã chốt (để không phải quyết lại)

- **Stack**: quốc tế (Eureka/Consul, OpenFeign/HTTP Interface, Spring Cloud LoadBalancer,
  Gateway, Config Server, Resilience4j, Kafka, Micrometer Tracing). Không dùng Netflix cũ
  (Hystrix/Ribbon/Zuul); không 2PC/Seata.
- **Contract sharing = Cách A**: DTO ở jar chung `account-service-api`; server implement,
  client add dependency. Lý do: Long quản cả hai service trong monorepo → đổi contract báo
  lỗi compile ngay, không phải chờ runtime. (Cách B — consumer tự khai — để dành nếu sau này
  muốn nếm bài học decoupling ở một cặp service khác.)
- **Async (bước 9) = Kafka**.
- **DB = PostgreSQL, database-per-service** (một instance chung khi học, nhưng không join chéo).
- **Config (bước 6) = Config Server backend `native`** (đã chốt): bắt đầu đọc file local cho
  gọn, chuyển `git` sau (chỉ đổi backend, không đụng code). Ví dụ đưa ra config: **hạn mức
  chuyển tiền** của transfer-service (property nghiệp vụ thuần, demo refresh nóng sạch hơn cấu
  hình route của gateway). Client nối Config Server bằng `spring.config.import=optional:configserver`
  (cơ chế Boot 2.4+, KHÔNG dùng bootstrap.yml cũ). `@ConfigurationProperties` tự rebind khi
  `/actuator/refresh` (không cần `@RefreshScope`).

## Known gaps (đã nhận diện, xử lý ở bước sau)

- [từ bước 5] **Gateway bị bypass nếu gọi thẳng port service** (8081/8082 vẫn mở). Rate-limit
  và auth ở gateway chỉ có tác dụng nếu service KHÔNG lộ trực tiếp. Cách đúng: chặn ở tầng
  mạng/deploy (K8s `ClusterIP` cho service, chỉ gateway expose; hoặc Docker compose không
  publish port service) — KHÔNG phải soi header ở mỗi service. Xử lý ở **bước 12 (K8s)**.
- [từ bước 5] **Route khai tường minh, mỗi service một khối** — nhiều service sẽ dài. Lựa chọn:
  bật Discovery Locator (`spring.cloud.gateway.server.webflux.discovery.locator.enabled=true`)
  để gateway tự sinh route từ Eureka; đánh đổi là mất tinh chỉnh filter/rate-limit per-route.
  Đa số dự án vẫn nghiêng khai tường minh vì cần kiểm soát per-route. Giữ tường minh hiện tại.
- **Discovery (bước 3) = Eureka** (đã chốt): mục tiêu hiểu cơ chế discovery; Eureka thuần
  Spring, dựng nhanh, không phải vận hành hạ tầng lạ. Consul có thể làm sau như lab đối chiếu.
- **Gateway (bước 5) = Spring Cloud Gateway WebFlux + rate limit** (đã chốt): route theo tên
  (lb://) + demo RequestRateLimiter (RedisRateLimiter, token bucket) để thấy giá trị đặt
  cross-cutting concern ở gateway. Artifact tên mới `spring-cloud-starter-gateway-server-webflux`
  (2025.0), prefix `spring.cloud.gateway.server.webflux.*`. Rate limit cần Redis (shared store).
- **Resilience (bước 7) = Resilience4j, bọc quanh AccountPort** (cách 2 đã chốt): decorator
  `ResilientAccountPort` (@Primary) đặt @CircuitBreaker + @Retry một chỗ, dùng chung cho cả hai
  adapter (@Qualifier "rawAccountPort"). Phân loại lỗi: 4xx -> AccountBusinessException (KHÔNG
  retry, không trip breaker); 5xx/timeout/mất kết nối -> AccountUnavailableException (retry +
  breaker). Timeout đặt ở tầng HTTP client (RestClient + Feign), KHÔNG dùng @TimeLimiter (chỉ
  hợp async). Đây là tầng resilience ỨNG DỤNG; tầng HẠ TẦNG (service mesh) là chuyện khác, chưa dùng.
  ⚠️ Retry chưa an toàn tuyệt đối vì thao tác chưa idempotent (double-debit nếu phản hồi mất) —
  bước 8 (idempotency) vá.
- **Version**: gom về BOM tập trung ở parent pom (khác các lab rời khai ở leaf), vì các
  service ở đây chạy cùng và phải tương thích.
- **Client (bước 2) = CẢ HAI** (đã chốt): HTTP Interface (mặc định) và OpenFeign, chọn qua
  property `account-service.client`, cùng chui qua cổng `AccountPort` nên `TransferService`
  không đổi khi swap. OpenFeign feature-complete từ Cloud 2022.0.0; Spring khuyến nghị HTTP
  Interface cho dự án mới, nhưng OpenFeign vẫn phổ biến trong JD -> biết cả hai.
- **Ràng buộc version cần nhớ (có dẫn chứng)**:
  - Boot 3.5 = Spring Framework 6.2 (bản 3.x cuối). Boot 4.0 = Spring Framework 7.0.
  - Server-side `@HttpExchange` (controller implement chính interface) là điểm nhấn của
    SF7/Boot4 -> bước 2 chỉ dùng `@HttpExchange` phía CLIENT, account-service giữ `@PostMapping`.
  - HTTP Interface tích hợp mượt load-balancing/discovery từ Spring Cloud 2025.1 (Boot 4);
    ở Cloud 2025.0 (Boot 3.5) hiện tại, bước 3–4 nối HTTP Interface với discovery sẽ phải
    config tay hơn OpenFeign (Feign tích hợp discovery/LB sẵn). Cân nhắc ở bước 3.
- **Javadoc = tiếng Việt** (project luyện tập).

## Quy ước làm việc (theo AGENTS.md của repo)

- Không viết code khi chưa yêu cầu rõ ràng.
- Giải thích hành vi framework/broker phải dựa docs/source, cấm suy diễn.
- Xong mỗi bước: cập nhật note của bước + bảng tiến độ này TRƯỚC khi sang bước kế.
