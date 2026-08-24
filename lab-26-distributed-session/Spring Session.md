# Spring Session (Redis) — Ghi chú học tập (lab-26)

> Tổng hợp kiến thức từ `lab-26-distributed-session`.  
> Yudao source: Boot 2.x + Jedis. HDL target: Boot 3.5 + Lettuce.  
> Phụ thuộc kiến thức: Redis (lab-11) + Security form login / session (lab-01).

---

## 1. Vấn đề lab giải quyết

HTTP Session mặc định nằm **RAM của một JVM**:

```text
Login / setAttribute trên Pod A
Load balancer → Pod B
Pod B không có session → mất login / mất attribute
```

**Spring Session + Redis:** session lưu Redis (chung). Cookie chỉ mang **session id**. Mọi pod đọc cùng Redis → không bắt buộc sticky session cho auth/session.

```text
Browser  Cookie: SESSION=<uuid>
   ↓
Filter springSessionRepositoryFilter
   ↓
Redis  spring:session:...
   ↓
HttpSession API (controller / Security) — không đổi cách gọi
```

Khác JWT: JWT thường stateless; lab-26 là **session stateful + store phân tán**.

---

## 2. Hai submodule HDL

| Module | Mục tiêu |
|---|---|
| `lab-26-distributed-session-redis` | `HttpSession` set/get → Redis; restart còn data |
| `lab-26-distributed-session-springsecurity` | Form login → `SPRING_SECURITY_CONTEXT` trong session Redis; restart còn auth |

**Skip:** yudao `distributed-session-02` (MongoDB).

---

## 3. Dependency (Boot 3.5)

```xml
spring-boot-starter-web
spring-session-data-redis
spring-boot-starter-data-redis
<!-- Module B thêm: -->
spring-boot-starter-security
```

- **Không** exclude Lettuce / **không** thêm Jedis (khác yudao).
- `(Tuỳ)` `starter-json` nếu dùng `RedisSerializer.json()`.

---

## 4. Cấu hình cơ bản

```java
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
}
```

Boot 3 cũng có auto-config khi có `spring-session-data-redis` trên classpath; annotation vẫn rõ ràng, khớp yudao.

### `application.yml` (Boot 3)

```yaml
spring:
  data:
    redis:          # Boot 3 — không phải spring.redis như Boot 2 / yudao
      host: localhost
      port: 6379
  security:         # chỉ Module B
    user:
      name: hdl
      password: 123
      roles: ADMIN
```

> Nếu vẫn dùng `spring.redis` trên Boot 3: có thể “may” kết nối tùy setup, nhưng **đúng chuẩn** là `spring.data.redis`.

---

## 5. Serializer — bài học thực tế (rất quan trọng)

Bean tên **`springSessionDefaultRedisSerializer`** được `RedisHttpSessionConfiguration` inject nếu có.

| Serializer | Khi nào |
|---|---|
| **Không khai báo (JDK mặc định)** | An toàn nhất; Module B nên dùng |
| `RedisSerializer.json()` | Module A — soi Redis dễ; attribute String OK |
| `RedisSerializer.string()` | **Sai** cho Spring Session |

### Vì sao `string()` lỗi?

Session Hash còn meta kiểu `Long` (`creationTime`, `lastAccessedTime`, …).  
`StringRedisSerializer` kỳ vọng mọi value là `String` →:

```text
ClassCastException: Long cannot be cast to String
```

IDE gợi ý `string()` trước `json()` — dễ tab nhầm.

### Đổi serializer giữa chừng

```text
Ghi JSON → Redis còn JSON + browser còn cookie SESSION
Đổi sang JDK → đọc cookie cũ → deserialize JSON bằng JDK
→ SerializationFailedException / DefaultDeserializer
```

**Cách xử lý:** xóa key `spring:session*` trên Redis (Podman: `podman exec ... redis-cli FLUSHDB` hoặc `DEL` pattern) **và** xóa cookie `SESSION`, rồi test lại.

**Quy tắc:** chọn một serializer cho một môi trường; đổi thì dọn Redis + cookie.

---

## 6. Cookie / Header session id (yudao comment — đọc overview)

| Resolver | Ý nghĩa |
|---|---|
| Mặc định | Cookie tên `SESSION` |
| `CookieHttpSessionIdResolver` + `DefaultCookieSerializer` | Đổi tên cookie (vd. `JSESSIONID`), tùy chỉnh cookie |
| `HeaderHttpSessionIdResolver("token")` | Session id qua header; client tự gửi lại |

Chỉ bật **một** `sessionIdResolver`. Lab HDL: giữ mặc định cookie `SESSION`.

---

## 7. Module A — Session Redis thuần

Controller (`jakarta.servlet.http.HttpSession`):

- `GET /session/set?key=&value=` → `setAttribute`
- `GET /session/get_all` → map mọi attribute

**Verify đã đạt:**

1. Set → có Set-Cookie `SESSION`
2. `redis-cli KEYS 'spring:session*'` có key
3. Restart app + giữ cookie → `get_all` còn data

---

## 8. Module B — Session + Security

Yudao gần như: Module A deps + `starter-security` + user yml + `@EnableRedisHttpSession` — **không** `SecurityConfig` phức tạp, **không** JSON serializer.

### `SPRING_SECURITY_CONTEXT` trong `get_all`

Sau login, Security tự ghi vào session. Ví dụ thấy:

```json
{
  "a": "1",
  "SPRING_SECURITY_CONTEXT": {
    "authentication": {
      "authenticated": true,
      "principal": { "username": "hdl", "password": null, ... }
    }
  }
}
```

| Field | Ý nghĩa |
|---|---|
| `authenticated: true` | Đã login |
| `principal.username` | User hiện tại |
| `password` / `credentials: null` | Không giữ mật khẩu trong session sau auth |
| `authorities` | Role; rỗng nếu yml không set `roles` |

Đây là lý do **restart vẫn login**: cookie → Redis → load lại SecurityContext.

### `FindByIndexNameSessionRepository` — không copy mù

Yudao `/session/list?username=` inject `FindByIndexNameSessionRepository`.

Boot 3 mặc định: `RedisSessionRepository` (**không index**) → **không có bean** đó →:

```text
required a bean of type 'FindByIndexNameSessionRepository' that could not be found
```

**HDL:** bỏ endpoint list; dùng URL đơn giản / `set`+`get_all` để test.  
Muốn list theo user sau: `@EnableRedisIndexedHttpSession` (nâng cao).

**Verify Module B:** login → Redis có session → restart + cookie → vẫn authenticated; `get_all` thấy `SPRING_SECURITY_CONTEXT`.

---

## 9. Redis trên Podman — dọn session

```bash
podman ps
podman exec -it <redis-container> redis-cli FLUSHDB
# hoặc chỉ session:
podman exec -it <redis-container> redis-cli KEYS 'spring:session*'
```

Nếu Redis map port 6379 ra host và có `redis-cli` local: `redis-cli -h localhost FLUSHDB`.

---

## 10. Checklist Boot 2 → 3.5

- [ ] `spring.data.redis.*` (không `spring.redis`)
- [ ] Lettuce, không Jedis exclude
- [ ] `jakarta.servlet.http.HttpSession`
- [ ] Không giả định `FindByIndexNameSessionRepository` với `@EnableRedisHttpSession` thường
- [ ] Không dùng `RedisSerializer.string()` làm session default serializer
- [ ] Module B: ưu tiên JDK serializer (không JSON) lần đầu
- [ ] Skip Mongo module

---

## 11. So với lab đã học

| Lab | Liên quan lab-26 |
|---|---|
| lab-11 Redis | Connection + serialize concept |
| lab-01 Security | Form login; SecurityContext trong session |
| lab-26 | Session store = Redis thay vì chỉ RAM |

Không dùng `RLock` / `RRateLimiter` cho bài này.

---

## 12. Không làm lần đầu (skip)

- Mongo session  
- Header session id / đổi `JSESSIONID`  
- Indexed + `findByPrincipalName`  
- JWT / `STATELESS`  

---

## 13. HDL modules

| Module | Nội dung |
|---|---|
| `lab-26-distributed-session-redis` | EnableRedisHttpSession, set/get, (serializer JSON comment/JDK) |
| `lab-26-distributed-session-springsecurity` | + Security auto user, SPRING_SECURITY_CONTEXT trên Redis |

Ghi chú: file này (`Spring Session.md`).

---

## 14. Lộ trình

```text
lab-11 Redis ✅ → lab-01 Security ✅ → lab-26 Session ✅
                                              ↓
                                        lab-29 Async (~ tiếp theo)
```
