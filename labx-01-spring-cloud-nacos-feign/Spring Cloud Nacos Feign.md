# Spring Cloud Nacos + Feign — Ghi chú học tập (labx-01)

> Mini system gộp yudao `labx-01` (discovery) + `labx-03` (Feign), **không** port hết submodule.  
> Yudao: Boot **2.2** + Cloud **Hoxton** + Alibaba **2.2** + Ribbon.  
> HDL: Java 21, Boot **3.5.14** + Cloud **2025.0.3** + Alibaba **2025.0.0.0** + LoadBalancer.  
> Pha 3 — block **#7** (sau Kafka).

**Xong:** 2026-08-25 (3/3 module bắt buộc).

---

## Mục lục

1. [Lab đứng đâu?](#1-lab-đứng-đâu)
2. [Kế hoạch submodule HDL](#2-kế-hoạch-submodule-hdl)
3. [Nacos (Podman)](#3-nacos-podman)
4. [BOM / dependency](#4-bom--dependency)
5. [Provider](#5-provider)
6. [Consumer discovery](#6-consumer-discovery)
7. [Consumer Feign](#7-consumer-feign)
8. [Lệch API 2.x → 3.5](#8-lệch-api-2x--35)
9. [Việc còn lại](#9-việc-còn-lại)

---

## 1. Lab đứng đâu?

```text
Pha 2  MQ          = service gửi event, không cần biết IP consumer
Pha 3  #7 (lab này) = HTTP đồng bộ: tìm service theo tên (Nacos) rồi gọi
Pha 3  #8 Config    = config ngoài jar
Pha 3  #9 Gateway   = cổng vào
Pha 3  #10 Sentinel = limit / circuit
```

```text
Nacos :8848 (API)  :18080 (console, map từ 8080)
        ▲
        │ đăng ký / query
   ┌────┴─────────────────────────────┐
   │ demo-provider        :8081       │
   │ demo-consumer-discovery :8082    │  RestClient + DiscoveryClient / LoadBalancerClient
   │ demo-consumer-feign     :8083    │  @FeignClient(name = "demo-provider")
   └──────────────────────────────────┘
```

Cổng **máy lab này** (không copy yudao `18080` cho provider — host `18080` là Nacos UI):

| Process | Host |
|---------|------|
| Nacos console | 18080 |
| Nacos API / gRPC | 8848 / 9848 |
| provider | 8081 |
| consumer-discovery | 8082 |
| consumer-feign | 8083 |

---

## 2. Kế hoạch submodule HDL

| # | Module | Trạng thái | Yudao | Học gì |
|---|--------|------------|-------|--------|
| 1 | `labx-01-sc-nacos-feign-provider` | ✅ | `labx-01` demo01-provider + API `labx-03` demo04 | Đăng ký Nacos; `/echo`, `/get_demo`, `/post_demo` |
| 2 | `labx-01-sc-nacos-feign-consumer-discovery` | ✅ | `labx-01` demo01-consumer | `DiscoveryClient.getInstances` + `LoadBalancerClient.choose` + `RestClient` |
| 3 | `labx-01-sc-nacos-feign-consumer-feign` | ✅ | `labx-03` demo01 + demo04 | `@FeignClient(name)` + `@SpringQueryMap` + POST body |

**Bắt buộc:** 3/3 xong. **Không** clone 18 Maven module yudao.

Overview / skip lần đầu:

| Yudao | Lý do skip |
|-------|------------|
| `labx-01` demo02 namespace | Đọc yaml; code lại ở **labx-05** Config |
| `labx-01` demo03 Actuator | Lộ trình skip actuator lần đầu |
| `labx-03` demo02 logger module | Gộp yaml `logger-level: full` vào consumer-feign |
| `labx-03` demo03 shared API jar | Đọc; không tách module |
| `labx-03` demo05 `url=` | Bỏ discovery |
| `labx-03` demo06 HttpClient/OkHttp | Transport |
| `labx-03` demo07 Ribbon retry | Ribbon chết; retry/circuit → Sentinel |

---

## 3. Nacos (Podman)

Image pin **`nacos/nacos-server:v3.0.3`** — khớp client SCA 2025.0.0.0 ([matrix SCA](https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/)). `MODE=standalone` bắt buộc.

Phải map **8080 + 8848 + 9848**. Thiếu **9848** (gRPC) thì client hay fail dù UI mở.

Máy này `8080` host bận → console: **`-p 18080:8080`**. UI: <http://127.0.0.1:18080> (Nacos 3 **không** còn `http://host:8848/nacos` như bài yudao).

`NACOS_AUTH_TOKEN` phải Base64 decode ra **≥ 32 byte (256 bit)**. Token lab cũ 30 byte → container `Exited (1)`. Sinh:

```bash
python3 -c "import os,base64; print(base64.b64encode(os.urandom(32)).decode())"
```

Tên container trùng: `podman rm -f nacos` rồi run lại.

App yaml lab (auth bật): `spring.cloud.nacos.username` / `password` + `discovery.server-addr: 127.0.0.1:8848` + `ip: 127.0.0.1` (app trên host, Nacos trong Podman).

---

## 4. BOM / dependency

Ba catalog: Boot parent **3.5.14** + import `spring-cloud-dependencies` **2025.0.3** + `spring-cloud-alibaba-dependencies` **2025.0.0.0**.

**Không** `2025.1.0.0` (Boot 4).

| Module | Starter |
|--------|---------|
| provider | `web` + `nacos-discovery` |
| discovery | + `loadbalancer` |
| feign | + `openfeign` + `loadbalancer` |

`dependencyManagement` = version; `dependencies` = jar trên classpath.

---

## 5. Provider

**Chứng minh (2026-08-25):**

- `GET :8081/echo?name=long` → `8081-provider:long`
- `GET :8081/get_demo?username=a&password=b` → JSON DTO
- `POST :8081/post_demo` JSON → cùng DTO
- Nacos UI `public` / `DEFAULT_GROUP`: `demo-provider`, 1 instance healthy

`@EnableDiscoveryClient` **không** gắn — vẫn đăng ký. [Spring Cloud Commons](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html): annotation no longer required nếu có `DiscoveryClient` trên classpath.

---

## 6. Consumer discovery

`RestClient.create()` — **không** `@LoadBalanced` (URL đã là `instance.getUri()`).

| URL | Cách chọn |
|-----|-----------|
| `GET /hello` | `getInstances("demo-provider")` rồi `get(0)` — list rỗng thì không `get(0)` |
| `GET /hello-lb` | `LoadBalancerClient.choose(...)` — **có thể `null`**; check nằm `callEcho` |

**Chứng minh:** `http://127.0.0.1:8082/hello?name=long` và `/hello-lb` → `consumer:8081-provider:long`. Một instance thì hai URL **trùng** port.

`get(0)` = lab (không LB). Prod không viết vậy.

Prod HTTP + discovery (không Feign): `@LoadBalanced` `RestClient.Builder`, URI **tên service** `http://demo-provider/echo` — [Commons 4.3](https://docs.spring.io/spring-cloud-commons/reference/4.3/spring-cloud-commons/common-abstractions.html). Cùng LB, không tự `choose()`.

---

## 7. Consumer Feign

`@EnableFeignClients` **bắt buộc**. Thiếu → `ProviderFeignClient` bean not found (đã gặp khi start).

`@FeignClient(name = "demo-provider")` — không `url=`.

Yaml 3.x: `spring.cloud.openfeign.client.config.default.logger-level` ([OpenFeign 4.3](https://docs.spring.io/spring-cloud-openfeign/reference/4.3/spring-cloud-openfeign.html)) — không `feign.client` Hoxton. Logger Feign cần `logging.level` package client = `DEBUG`.

**Chứng minh:**

```text
GET  :8083/hello?name=long              → consumer:8081-provider:long
GET  :8083/get-demo?username=a&password=b → {"username":"a","password":"b"}
POST :8083/post-demo JSON                 → cùng DTO
```

`@SpringQueryMap` = GET object (yudao demo04 cách recommended). Không port 3 overload GET.

**Feign vs `@LoadBalanced` RestClient:** cùng Nacos + LB. Feign = interface; RestClient LB = gõ `http://demo-provider/...`. Lab HDL bước này = Feign (stack Nacos). VN/US nhiều JD dùng K8s DNS, không bắt Feign.

---

## 8. Lệch API 2.x → 3.5

| | Yudao 2.x | HDL 3.5 |
|--|-----------|---------|
| Cloud / Alibaba | Hoxton / 2.2.0 | **2025.0.3** / **2025.0.0.0** |
| HTTP tay | `RestTemplate` | `RestClient` |
| LB | Ribbon (`ribbon.*`, demo07) | `spring-cloud-starter-loadbalancer` |
| Config Nacos | hay `bootstrap.yml` | `application.yml` |
| Feign yaml | `feign.client.config` | `spring.cloud.openfeign.client.config` |
| Nacos UI | `:8848/nacos` | Nacos 3 console **:8080** (lab map **18080**) |
| Nacos server | 1.x/2.x bài cũ | **3.0.3** (client 3.x) |

---

## 9. Việc còn lại

Block #7 **bắt buộc xong**. **#8 Config** xong 2026-08-25. **#9 Gateway** xong 2026-08-26 (`static` / `registry` / `rate-limit`). Note: `Spring Cloud Gateway.md`.

Không đếm vào xong #7: namespace module (đã làm ở #8), actuator, shared API, `url=`, OkHttp, Ribbon.
