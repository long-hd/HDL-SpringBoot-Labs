# Spring Cloud Sentinel — Ghi chú học tập (labx-04)

> Chặn **trong service** (QPS / rule), không phải Gateway `RequestRateLimiter`.  
> Yudao: Boot **2.2** + Cloud **Hoxton** + Alibaba **2.2** + adapter `sentinel-spring-webmvc` (`javax`).  
> HDL: Java 21, Boot **3.5.14** + Cloud **2025.0.3** + Alibaba **2025.0.0.0** + Sentinel **1.8.9** + adapter **`webmvc-v6x`** (`jakarta`).  
> Pha 3 — block **#10** (sau Gateway). **Không** Sentinel Console Alibaba Cloud (billing). **Không** gắn rule lên Gateway `#9`.

**Tiến độ:** 2026-08-26 — **nacos** xong (rule JSON trên Nacos + `throw e` → `@ExceptionHandler`). Dashboard **demo** skip. **Feign skip** (lý do §9). Block **#10 đóng**. **Pha 3 xong.**

---

## Mục lục

1. [Lab đứng đâu?](#1-lab-đứng-đâu)
2. [Kế hoạch submodule HDL](#2-kế-hoạch-submodule-hdl)
3. [BOM / dependency](#3-bom--dependency)
4. [Resource name v6x](#4-resource-name-v6x)
5. [Block → JSON (yudao)](#5-block--json-yudao)
6. [Nacos](#6-nacos)
7. [Demo (Dashboard)](#7-demo-dashboard)
8. [Lệch API 2.x → 3.5](#8-lệch-api-2x--35)
9. [Feign (skip)](#9-feign-skip)

---

## 1. Lab đứng đâu?

```text
Pha 3  #7 Discovery+Feign = service ở đâu, gọi theo tên
Pha 3  #8 Config          = config ngoài jar
Pha 3  #9 Gateway         = cổng vào (rate-limit Redis = Gateway filter)
Pha 3  #10 (lab này)      = limit / degrade trong service (Sentinel)
```

```text
Client curl
  →  sentinel-demo  :8089   rule RAM Dashboard :7070   (skip proof)
  →  sentinel-nacos :8090   rule JSON Nacos :8848      ✅
  →  sentinel-feign :8091   Feign + Sentinel fallback  (skip — không có module HDL)
```

Hai module demo / nacos dùng **cùng** thư viện Sentinel. Khác chỗ **rule sống**: Dashboard RAM vs Nacos JSON. Restart `:8090` vẫn chặn nếu Data ID còn.

| Process | Host |
|---------|------|
| Nacos console / API | 18080 / 8848 |
| Sentinel Dashboard (nếu chạy) | **7070** — client Command Center **8719** |
| `sentinel-demo` | **8089** |
| `sentinel-nacos` | **8090** |
| `sentinel-feign` | **không tạo** (skip) |

---

## 2. Kế hoạch submodule HDL

| # | Module | Trạng thái | Yudao | Học gì |
|---|--------|------------|-------|--------|
| 1 | `labx-04-sc-sentinel-demo` | skip Dashboard | demo01 | Adapter MVC + Dashboard. Proof QPS **không** bắt buộc nếu đã có nacos |
| 2 | `labx-04-sc-sentinel-nacos` | ✅ | nacos-provider | Rule FLOW JSON trên Nacos; restart app vẫn chặn |
| 3 | `labx-04-sc-sentinel-feign` | **skip** | feign-consumer | Sentinel trên **lời gọi Feign** + `FallbackFactory` — không làm HDL |

**Bắt buộc lab:** nacos ✅. Demo Dashboard skip. Feign skip (§9). **Không** clone Apollo / file / actuator / RestTemplate yudao.

---

## 3. BOM / dependency

| | Version |
|---|---|
| Boot | 3.5.14 |
| Cloud | 2025.0.3 |
| Alibaba | 2025.0.0.0 |
| Sentinel (BOM Alibaba) | 1.8.9 |
| Adapter | `sentinel-spring-webmvc-v6x-adapter` (Jakarta) |

Nacos module: `spring-cloud-starter-alibaba-sentinel` + `sentinel-datasource-nacos`. Nacos 3: `username` / `password` trên datasource (SCA 2025 `NacosDataSourceProperties` có field).

Starter **web** (Tomcat). **Không** `starter-gateway`.

---

## 4. Resource name v6x

Adapter **v6x** ghi resource = **path** (URI / `BEST_MATCHING_PATTERN`), **không** prefix HTTP method.

JSON Nacos **đúng** (đã curl):

```json
[{"resource":"/demo/echo","limitApp":"default","grade":1,"count":1}]
```

`grade: 1` = QPS. `count: 1` = 1 request/giây.

**Sai** (rule không khớp, curl mãi `echo`): `"resource": "GET:/demo/echo"` — tên kiểu adapter **cũ** / đoán. Verify: Nacos JSON `/demo/echo` thì lần 2 trong vòng `for` bị chặn.

Nguồn: `SentinelWebInterceptor` / `AbstractSentinelInterceptor` Sentinel **1.8.9** (`webmvc_v6x`).

---

## 5. Block → JSON (yudao)

Hai file **một đường**, không hai chỗ cùng ghi body.

1. `CustomBlockExceptionHandler` (`BlockExceptionHandler` v6x): **`throw e`**. Không ghi `HttpServletResponse`.
2. `GlobalExceptionHandler` (`@RestControllerAdvice` + `@ExceptionHandler(BlockException.class)`): JSON `{code:1024, msg:...}`.

Sentinel interceptor bắt `BlockException` trong `preHandle`, gọi `handle()`, rồi `return false` **nếu `handle` return**. Ném lại thì exception ra `DispatcherServlet.processHandlerException` (Spring **6.2**) → `@ExceptionHandler`.

`basePackages` phải khớp **controller của app đó**:

| App | Package controller | `basePackages` đúng |
|-----|--------------------|---------------------|
| nacos | `...sentinelnacos.controller` | `...sentinelnacos` |
| demo | `...sentineldemo.controller` | `...sentineldemo` |

Copy nacos mà để `...sentineldemo` → advice **không** áp → `FlowException` ra `/error` **500** (`Request processing failed: FlowException`). Sửa package rồi curl:

```text
===== 1 =====
echo
===== 2 =====
{"code":1024,"msg":"blocked: FlowException"}
===== 3 =====
{"code":1024,"msg":"blocked: FlowException"}
```

HTTP **200** + `code` 1024 trong body (yudao). **Không** 429 — 429 chỉ khi tự `response.setStatus(429)` trong `handle()` (default adapter: 429 + text `"Blocked by Sentinel (flow limiting)"`).

`CustomRequestOriginParser` (`s-user` → origin): rule FLOW `limitApp: "default"` **không** lọc origin. Dùng khi authority / `limitApp` gắn origin cụ thể.

---

## 6. Nacos

Cổng **8090**. `spring.application.name: sentinel-nacos`.

Nacos UI **:18080**, namespace **public**, **trước** khi start app (datasource đọc lúc lên):

| | Giá trị |
|---|---|
| Data ID | `sentinel-nacos-flow-rule` |
| Group | `DEFAULT_GROUP` |
| Format | JSON |
| `rule-type` yaml | `FLOW` |

Yaml **không** cần `transport.dashboard` cho proof này. Restart `:8090` vẫn chặn nếu JSON còn trên Nacos.

```bash
for i in 1 2 3; do echo "===== $i ====="; curl -sS "http://127.0.0.1:8090/demo/echo"; echo; done
```

---

## 7. Demo (Dashboard)

Cổng **8089**, `spring.application.name: sentinel-demo` — **không** đặt `demo-provider` (trùng `:8081`).

Dashboard jar **1.8.9**, UI tiếng Trung: `java -Dserver.port=7070 -jar …`. `eager: true` + `transport.dashboard: 127.0.0.1:7070`. Cùng máy: `transport.client-ip: 127.0.0.1` nếu Dashboard `MetricFetcher` **Connection refused** IP LAN `:8719`.

Chưa có rule: curl `/demo/echo` chỉ chứng minh MVC, **không** chứng minh QPS. Proof QPS nằm module **nacos**. Skip Dashboard **được**.

Code demo đã `throw e` + `basePackages` đúng `sentineldemo`. Comment yaml `GET:/demo/echo` là **sai v6x** — resource = `/demo/echo`.

---

## 8. Lệch API 2.x → 3.5

| | Yudao 2.x | HDL 3.5 |
|---|---|---|
| Servlet | `javax.servlet` | `jakarta.servlet` |
| Adapter | `...webmvc.callback.BlockExceptionHandler` (`handle(req, res, e)`) | `...webmvc_v6x.callback.BlockExceptionHandler` (`handle(req, res, resourceName, e)`) |
| Resource URL | hay thấy `GET:/path` | **path** `/demo/echo` |
| JSON block | `throw e` + `@ControllerAdvice` | **cùng pattern**; `basePackages` phải đúng module |
| Ghi JSON trong `handle()` | không (yudao ném) | **không** dùng nếu muốn advice — ghi `response` thì interceptor `return false`, advice **không** chạy |
| Feign fallback | `feign.hystrix.FallbackFactory` + `feign.sentinel.enabled` | **skip HDL.** Nếu làm sau: `org.springframework.cloud.openfeign.FallbackFactory` — **không** `feign.hystrix` |

---

## 9. Feign (skip)

Yudao `labx-04-sca-sentinel-feign-consumer`: consumer `@FeignClient` gọi provider `/demo/echo`, `feign.sentinel.enabled: true`, `FallbackFactory` trả `"fallback:" + tên exception` khi provider chặn hoặc chết.

Khác **#7** (OpenFeign + Nacos, gọi theo tên). Khác **nacos** (chặn URL **trên chính service**). Feign lab = Sentinel bọc **RPC phía caller**.

**Skip:** proof #10 chốt ở FLOW HTTP provider (`:8090`). OpenFeign đã học ở #7. Module yudao dùng `feign.hystrix.FallbackFactory` — không copy Boot 3.

Làm sau (không đếm lộ trình): cổng **8091**, `feign.sentinel.enabled`, `org.springframework.cloud.openfeign.FallbackFactory`. Apollo / file / actuator / RestTemplate yudao vẫn skip.

Nguồn interceptor: [AbstractSentinelInterceptor.java (1.8.9)](https://github.com/alibaba/Sentinel/blob/1.8.9/sentinel-adapter/sentinel-spring-webmvc-v6x-adapter/src/main/java/com/alibaba/csp/sentinel/adapter/spring/webmvc_v6x/AbstractSentinelInterceptor.java).
