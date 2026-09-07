# Kế hoạch dựng & tiến độ — system-money-transfer

Dựng theo kiểu incremental: mỗi bước thêm ĐÚNG một mảnh vào cùng một hệ, để mỗi khái niệm
có chỗ bấu víu. Bản đồ này để đối chiếu về sau "đã làm tới đâu, vì sao chọn thế".

## Tiến độ

| Bước | Nội dung | Module/mảnh | Trạng thái |
|---|---|---|---|
| 0 | Khung tối giản: account + transfer, gọi nhau bằng REST + URL cứng, happy-path | account-service, transfer-service, account-service-api | ✅ Xong |
| 1 | Nền distributed systems (why) | (lý thuyết) | ✅ Ghi note (`m1-distributed-systems-notes.md`) |
| 2 | Thay lời gọi thủ công bằng interface khai báo | HTTP Interface / OpenFeign | ⬜ Chưa |
| 3 | Service discovery — gọi theo tên, bỏ URL cứng | discovery-server (Eureka/Consul) | ⬜ Chưa |
| 4 | Load balancing giữa nhiều instance | Spring Cloud LoadBalancer | ⬜ Chưa |
| 5 | API Gateway — một cửa vào | Spring Cloud Gateway | ⬜ Chưa |
| 6 | Config tập trung | Config Server | ⬜ Chưa |
| 7 | Resilience — circuit breaker/retry/timeout | Resilience4j | ⬜ Chưa |
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
- **Config (bước 6)**: bắt đầu `native` (đọc file local) cho gọn, chuyển `git` sau.
- **Version**: gom về BOM tập trung ở parent pom (khác các lab rời khai ở leaf), vì các
  service ở đây chạy cùng và phải tương thích.
- **Client (bước 2)**: CHƯA chốt OpenFeign vs Spring HTTP Interface. Lưu ý đã ghi nhận:
  Spring Cloud OpenFeign được coi feature-complete từ Cloud 2022.0.0, Spring khuyến nghị
  HTTP Interface cho dự án mới (OpenFeign vẫn dùng được, chỉ không phải hướng khuyến nghị).
  Sẽ quyết ở đầu bước 2 — có thể làm cả hai để so sánh + cả hai lên CV.
- **Javadoc = tiếng Việt** (project luyện tập).

## Quy ước làm việc (theo AGENTS.md của repo)

- Không viết code khi chưa yêu cầu rõ ràng.
- Giải thích hành vi framework/broker phải dựa docs/source, cấm suy diễn.
- Xong mỗi bước: cập nhật note của bước + bảng tiến độ này TRƯỚC khi sang bước kế.
