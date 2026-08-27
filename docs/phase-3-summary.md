# Pha 3 — Tổng kết: Spring Cloud (Nacos · Feign · Config · Gateway · Sentinel)

> Hoàn thành: 2026-08-26  
> Stack: Java 21, Spring Boot **3.5.14**, Cloud **2025.0.3**, Alibaba **2025.0.0.0**  
> Nguồn học: yudao-labs (Boot 2.x / Cloud Hoxton) → code lại HDL, không copy starter và YAML 2.x.

---

## 1. Pha 3 là gì?

**Phạm vi:** Nhiều **process / service**, tìm nhau qua registry, config ngoài jar, cổng vào, chặn lưu lượng **trong service**. Không còn “một app + broker” như Pha 2.

```text
Pha 1  Nền (1 process)     Redis → Security → Session → Async   ✅ (2026-08-21)
Pha 2  Message              RabbitMQ → Kafka                     ✅ (2026-08-24)
Pha 3  Spring Cloud         Nacos · Feign · Config · Gateway · Sentinel   ✅ (2026-08-26)
Pha 4  Mở rộng              Stream (skip) · Job ✅ (2026-08-27)
```

**Kết quả pha (theo lộ trình):**  
Service đăng ký Nacos, gọi theo tên (RestClient / OpenFeign), kéo config từ Nacos, client vào qua Gateway, QPS chặn bằng Sentinel rule trên Nacos.

Khái niệm Cloud dài hơn: [spring-cloud.md](./spring-cloud.md). File này = **đã làm gì trên HDL**, không lặp encyclopedia.

---

## 2. Bốn block đã xong

| # | Lab yudao | HDL | Note |
|---|---|---|---|
| 7 | `labx-01` + `labx-03` | `labx-01-spring-cloud-nacos-feign` (`provider` :8081, `consumer-discovery` :8082, `consumer-feign` :8083) | [Spring Cloud Nacos Feign.md](../labx-01-spring-cloud-nacos-feign/Spring%20Cloud%20Nacos%20Feign.md) |
| 8 | `labx-05` | `labx-05-spring-cloud-nacos-config` (`demo` :8084, `profiles` :8085) | [Spring Cloud Nacos Config.md](../labx-05-spring-cloud-nacos-config/Spring%20Cloud%20Nacos%20Config.md) |
| 9 | `labx-08` | `labx-08-spring-cloud-gateway` (`static` :8086, `registry` :8087, `rate-limit` :8088) | [Spring Cloud Gateway.md](../labx-08-spring-cloud-gateway/Spring%20Cloud%20Gateway.md) |
| 10 | `labx-04` | `labx-04-spring-cloud-alibaba-sentinel` (`demo` :8089 skip Dashboard, `nacos` :8090) | [Spring Cloud Sentinel.md](../labx-04-spring-cloud-alibaba-sentinel/Spring%20Cloud%20Sentinel.md) |

Thứ tự học: **#7 → #8 → #9 → #10**. Feign **gọi HTTP** (#7) trước Gateway; Sentinel **trong service** sau Gateway (không nhầm Redis 429 trên `:8088`).

```mermaid
flowchart LR
    L7["#7 Nacos + Feign"] --> L8["#8 Config"]
    L8 --> L9["#9 Gateway"]
    L9 --> L10["#10 Sentinel"]
```

Nacos **một server** (console :18080, API :8848) dùng chung #7–#10. Redis :6379 chỉ **Gateway rate-limit**.

---

## 3. Từng block — chứng minh gì

### Block 7 — Nacos Discovery + OpenFeign

**Một câu:** Service không hard-code IP; đăng ký tên trên Nacos, caller chọn instance rồi HTTP.

| Module | Chứng minh |
|--------|------------|
| `provider` :8081 | Đăng ký `demo-provider`; `/echo` → `8081-provider:long` |
| `consumer-discovery` :8082 | `DiscoveryClient.getInstances` + `LoadBalancerClient.choose` + `RestClient` |
| `consumer-feign` :8083 | `@FeignClient(name = "demo-provider")` + query/POST |

**Skip lần đầu:** namespace discovery (làm ở #8), Actuator, Ribbon retry, Feign `url=` tắt discovery.

**Boot 3:** Ribbon → `spring-cloud-starter-loadbalancer`. Feign yaml: `spring.cloud.openfeign.client.config` (không `feign.client` Hoxton).

---

### Block 8 — Nacos Config

**Một câu:** Config sống trên Nacos (dataId / group / namespace); app import lúc start; sửa UI có thể refresh **không** rebuild.

| Module | Chứng minh |
|--------|------------|
| `demo` :8084 | `spring.config.import` yaml Nacos; `@Value` + `@RefreshScope` đổi theo publish; `OrderProperties` không `@RefreshScope` thì snapshot lúc start |
| `profiles` :8085 | Namespace **`dev`** ≠ **public** → giá trị khác (`10/1`) |

Starter **`nacos-config`**, không nhầm `nacos-discovery`. Không `bootstrap.yml`.

**Skip:** multi dataId (đọc overview), jasypt, actuator `/env`.

---

### Block 9 — Spring Cloud Gateway

**Một câu:** Client gọi **một** cổng; Gateway khớp route rồi forward backend. Ba module = **ba bài**, không nối hai hop.

| Module | Chứng minh |
|--------|------------|
| `static` :8086 | `Path=/api/**` + `StripPrefix` → `http://127.0.0.1:8081/echo` |
| `registry` :8087 | `lb://demo-provider` + Nacos + LoadBalancer |
| `rate-limit` :8088 | `RequestRateLimiter` + Redis reactive; burst 2 → **HTTP 429** |

Backend reuse `:8081` `/echo`. Gateway = **WebFlux / Netty** — **không** `spring-boot-starter-web` trên app Gateway.

Yaml 3.x: `spring.cloud.gateway.server.webflux.routes` (không `spring.cloud.gateway.routes` Hoxton).

**Không nhầm:** limiter Redis trên Gateway ≠ Sentinel QPS trong service (#10).

---

### Block 10 — Sentinel

**Một câu:** Chặn QPS **trong service** bằng rule; HDL chứng minh rule JSON trên Nacos, không RAM Dashboard.

| Module | Trạng thái |
|--------|------------|
| `demo` :8089 | Code có; **skip proof Dashboard** (:7070 / client 8719) |
| `nacos` :8090 | ✅ Data ID `sentinel-nacos-flow-rule`, resource **`/demo/echo`**, QPS 1 → JSON `code` 1024 |
| Feign consumer | **skip** — xem dưới |

Adapter **webmvc-v6x**: resource = **path** `/demo/echo`, không `GET:/demo/echo`.

JSON block (yudao): `CustomBlockExceptionHandler` **`throw e`** → `@ExceptionHandler(BlockException)` trên `@RestControllerAdvice`. `basePackages` phải đúng package controller (`sentinelnacos`, không copy `sentineldemo`) — sai thì `/error` **500**. HTTP **200** + `code` 1024 (không 429 trừ khi tự `setStatus` trong `handle()`).

**Feign skip:** module yudao = Sentinel trên **lời gọi Feign** (`feign.sentinel.enabled` + `FallbackFactory`). Khác #7 (OpenFeign + Nacos). Khác nacos (chặn URL trên chính service). Yudao dùng `feign.hystrix.FallbackFactory` — không copy Boot 3. Nếu làm sau: `org.springframework.cloud.openfeign.FallbackFactory`, cổng 8091. Chi tiết: [Spring Cloud Sentinel.md](../labx-04-spring-cloud-alibaba-sentinel/Spring%20Cloud%20Sentinel.md) §9.

---

## 4. Cổng lab (máy này)

| Process | Host |
|---------|------|
| Nacos console / API / gRPC | 18080 / 8848 / 9848 |
| Redis (chỉ Gateway rate-limit) | 6379 |
| `demo-provider` | 8081 |
| consumer-discovery / feign | 8082 / 8083 |
| config-demo / profiles | 8084 / 8085 |
| gateway static / registry / rate-limit | 8086 / 8087 / 8088 |
| sentinel-demo / nacos | 8089 / 8090 |
| Sentinel Dashboard (nếu chạy) | 7070 |

---

## 5. Ba chỗ “limit” — đừng gộp

| | Redis `RRateLimiter` (Pha 1) | Gateway `RequestRateLimiter` | Sentinel FLOW |
|---|---|---|---|
| Chỗ | Trong JVM service | Cổng `:8088` | Trong service `:8090` |
| Store | Redis | Redis reactive | Rule Nacos (JSON) / Dashboard RAM |
| Proof lab | lab-11 | burst 2 → **429** | QPS 1 → body `code` 1024 |

---

## 6. Lệch API 2.x → 3.5 (checklist)

| Chủ đề | Yudao 2.x | HDL 3.5 |
|--------|-----------|---------|
| Servlet | `javax.*` | `jakarta.*` |
| LB | Ribbon | `spring-cloud-starter-loadbalancer` |
| Feign yaml | `feign.client.config` | `spring.cloud.openfeign.client.config` |
| Config | `bootstrap.yml` | `spring.config.import` |
| Gateway starter | `spring-cloud-starter-gateway` | `spring-cloud-starter-gateway-server-webflux` |
| Gateway yaml | `spring.cloud.gateway.routes` | `spring.cloud.gateway.server.webflux.routes` |
| Redis property (rate-limit) | `spring.redis` | `spring.data.redis` |
| Sentinel adapter | `webmvc` (`handle(req,res,e)`) | **`webmvc-v6x`** (`handle(..., resourceName, e)`) |
| Sentinel resource URL | hay `GET:/path` | **path** `/demo/echo` |
| Feign + Sentinel fallback | `feign.hystrix.FallbackFactory` | **skip**; nếu làm: OpenFeign `FallbackFactory` |

BOM lab: Boot **3.5.14**, Cloud **2025.0.3**, Alibaba **2025.0.0.0**, Sentinel **1.8.9**. Nacos 3: `username` / `password` trên datasource.

---

## 7. Skip có chủ đích (không thuộc “thiếu Pha 3”)

| Mục | Lý do |
|-----|--------|
| Eureka / Ribbon / Hystrix / Zuul | Legacy |
| ~18 module Nacos+Feign yudao | Mini 3 module đủ |
| Config multi / jasypt / actuator | Overview |
| Gateway Weight / Auth toy / Dubbo / locator curl `:8088` | Không đếm xong #9 |
| Sentinel Dashboard proof | Rule persist = nacos |
| Sentinel Feign / Apollo / file / RestTemplate | Feign skip §3; còn lại kitchen-sink |
| Sentinel trên Gateway (yudao demo07-sentinel) | Tách #9 Redis vs #10 in-process |

---

## 8. Checklist “đã xong Pha 3?”

Tự trả lời được:

- [ ] Nacos: service name vs IP; console :18080 ≠ API :8848
- [ ] `DiscoveryClient` / `LoadBalancerClient` vs `@FeignClient(name)`
- [ ] dataId / group / namespace; `@RefreshScope` trên `@Value` vs properties không refresh
- [ ] Gateway WebFlux; yaml `server.webflux.routes`; `lb://` vs URI tĩnh; 429 Redis ≠ Sentinel
- [ ] Sentinel v6x resource = path; `throw e` + `basePackages` đúng module; JSON Nacos `rule-type: FLOW`

---

## 9. Thời gian

**Ước lượng lộ trình:** #7 12–18h, #8 6–10h, #9 12–18h, #10 8–12h → Pha 3 ~38–58h (`learning-path.md` / `spring-cloud.md`).

**Lịch repo:** bắt đầu #7 **2026-08-25** (sau Pha 2 **2026-08-24**); đóng #10 **2026-08-26**. Không ghi giờ máy từng block.

---

## 10. Tiếp theo — Pha 4 (đã xong 2026-08-27)

```text
✅ Pha 1  Redis → Security → Session → Async
✅ Pha 2  RabbitMQ → Kafka
✅ Pha 3  Nacos → Config → Gateway → Sentinel
✅ Pha 4  lab-28 Job (task-demo + quartz-jdbc); labx-11 Stream skip
```

Chi tiết: [phase-4-summary.md](./phase-4-summary.md), [Spring Boot Job.md](../lab-28-task/Spring%20Boot%20Job.md).

---

## 11. Tài liệu liên quan

- [learning-path.md](./learning-path.md) — Lộ trình tổng thể
- [spring-cloud.md](./spring-cloud.md) — Microservices & Spring Cloud (khái niệm)
- [phase-1-summary.md](./phase-1-summary.md) — Tổng kết Pha 1
- [phase-2-summary.md](./phase-2-summary.md) — Tổng kết Pha 2
- [Spring Cloud Nacos Feign.md](../labx-01-spring-cloud-nacos-feign/Spring%20Cloud%20Nacos%20Feign.md) — #7
- [Spring Cloud Nacos Config.md](../labx-05-spring-cloud-nacos-config/Spring%20Cloud%20Nacos%20Config.md) — #8
- [Spring Cloud Gateway.md](../labx-08-spring-cloud-gateway/Spring%20Cloud%20Gateway.md) — #9
- [Spring Cloud Sentinel.md](../labx-04-spring-cloud-alibaba-sentinel/Spring%20Cloud%20Sentinel.md) — #10

---

**Cập nhật lần cuối:** 2026-08-26  
**Tác giả:** HDL Spring Boot Labs
