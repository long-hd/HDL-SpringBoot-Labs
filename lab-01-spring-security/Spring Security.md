# Spring Security — Ghi chú học tập (lab-01)

> Tổng hợp kiến thức từ quá trình học `lab-01-spring-security`.  
> Yudao source: Boot 2.x + `WebSecurityConfigurerAdapter`. HDL target: Boot 3.5 + `SecurityFilterChain`.  
> Mô hình auth lab: **form login + HTTP Session (cookie)** — không JWT. JWT monolith để cuối lộ trình.

---

## 1. AuthN vs AuthZ

```text
AuthN (Authentication) — Xác thực: "Mày là ai?"
AuthZ (Authorization)  — Phân quyền: "Mày được làm gì?"
```

Mọi cấu hình Security xoay quanh hai việc này.

---

## 2. Kiến trúc Filter Chain

Spring Security nằm **trước** DispatcherServlet. Request đi qua chuỗi filter:

```text
Request
  ↓
[ SecurityFilterChain ]
  ├── UsernamePasswordAuthenticationFilter  ← form login
  ├── ExceptionTranslationFilter            ← 401/403, redirect login
  ├── AuthorizationFilter                   ← kiểm quyền URL (Boot 3)
  └── ...
  ↓
DispatcherServlet → Controller
  ↓ (nếu có @EnableMethodSecurity)
Method Security interceptor → @PreAuthorize / @PermitAll
```

**Thứ tự quan trọng:** URL-level (filter) chạy **trước** method-level (`@PreAuthorize`).

---

## 3. Hai submodule HDL

| Module | Mục tiêu |
|---|---|
| `lab-01-springsecurity-demo` | Auto-config: chỉ starter + `spring.security.user` trong yml, **không** `SecurityConfig` |
| `lab-01-springsecurity-demo-role` | Tự cấu hình: `SecurityFilterChain` + 2 user in-memory + URL RBAC + method security |

### Demo — dependency & yml

```xml
spring-boot-starter-web
spring-boot-starter-security
```

```yaml
spring:
  security:
    user:
      name: user
      password: user
      roles: ADMIN
```

Mọi endpoint mặc định cần auth. Form login `/login` do Spring cung cấp. Account: `user` / `user`.

### Demo-role — endpoint cần nhớ

**URL-level (`TestController` `/test/*`) — rule trong `HttpSecurity`:**

| Path | Rule |
|---|---|
| `/test/demo` | `permitAll` |
| `/test/admin` | `hasRole("ADMIN")` |
| `/test/normal` | `hasRole("NORMAL")` |
| còn lại | `authenticated()` |

**Method-level (`DemoController` `/demo/*`) — annotation:**

| Path | Annotation |
|---|---|
| `/demo/echo` | `@PermitAll` (jakarta) — vẫn có thể bị URL rule chặn trước |
| `/demo/home` | chỉ cần login (`anyRequest().authenticated()`) |
| `/demo/admin` | `@PreAuthorize("hasRole('ADMIN')")` |
| `/demo/normal` | `@PreAuthorize("hasRole('NORMAL')")` |

User: `admin`/`admin` (role ADMIN), `normal`/`normal` (role NORMAL).

---

## 4. Boot 2.x → Boot 3.5 (không copy yudao)

| Yudao (Boot 2) | HDL (Boot 3.5) |
|---|---|
| `extends WebSecurityConfigurerAdapter` | class thường + `@Bean SecurityFilterChain` |
| `configure(HttpSecurity)` | `filterChain(HttpSecurity)` → `return http.build()` |
| `authorizeRequests()` | `authorizeHttpRequests(auth -> …)` |
| `antMatchers(...)` | `requestMatchers(...)` |
| `.and().formLogin()` | `.formLogin(form -> …)` hoặc method reference |
| `configure(AuthenticationManagerBuilder)` | `@Bean UserDetailsService` |
| `NoOpPasswordEncoder` + plaintext | **`BCryptPasswordEncoder`** + `encoder.encode(...)` |
| `@EnableGlobalMethodSecurity(prePostEnabled = true)` | **`@EnableMethodSecurity`** |
| `javax.annotation.security.PermitAll` | `jakarta.annotation.security.PermitAll` |

`WebSecurityConfigurerAdapter` **bị xóa** trong Boot 3 — copy yudao → không compile.

---

## 5. Config mẫu Boot 3 (demo-role)

```java
@Configuration
@EnableMethodSecurity  // BẮT BUỘC nếu dùng @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/test/demo").permitAll()
                .requestMatchers("/test/admin").hasRole("ADMIN")
                .requestMatchers("/test/normal").hasRole("NORMAL")
                .anyRequest().authenticated()
            )
            .formLogin(AbstractAuthenticationFilterConfigurer::permitAll)
            .logout(LogoutConfigurer::permitAll);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // lab dùng strength 4 cũng được
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin"))
                .roles("ADMIN")
                .build();
        UserDetails normal = User.builder()
                .username("normal")
                .password(encoder.encode("normal"))
                .roles("NORMAL")
                .build();
        return new InMemoryUserDetailsManager(admin, normal);
    }
}
```

---

## 6. URL-level vs Method-level

| | URL (`HttpSecurity`) | Method (`@PreAuthorize`) |
|---|---|---|
| Chỗ cấu hình | `SecurityFilterChain` | Annotation trên method/class |
| Khi chạy | Filter, **trước** controller | AOP interceptor, **sau** khi đã vào method mapping |
| Cần bật riêng? | Có chain là chạy | **`@EnableMethodSecurity` bắt buộc** |
| Thiếu enable | — | Annotation **bị bỏ qua** — chỉ còn rule URL |

### Bài học thực tế (đã gặp)

Login `normal` → `/test/admin` **403** (URL rule), nhưng `/demo/admin` **200** nếu thiếu `@EnableMethodSecurity` — vì `@PreAuthorize` không chạy, chỉ còn `anyRequest().authenticated()`.

**Template project thường đã có `@EnableMethodSecurity` sẵn** → dễ nhầm tưởng annotation “tự chạy”. Luôn search enable trước khi kết luận.

### `/demo/echo` + `@PermitAll`

`@PermitAll` trên method **không** mở URL nếu `HttpSecurity` đã `.anyRequest().authenticated()` và path không nằm trong `permitAll` của chain. Filter chặn trước → vẫn redirect login.

---

## 7. Role vs Authority — bẫy prefix

```text
.roles("ADMIN")     → authority lưu = "ROLE_ADMIN"
.hasRole("ADMIN")   → Spring tự tìm "ROLE_ADMIN"  ✅
.hasRole("ROLE_ADMIN") → tìm "ROLE_ROLE_ADMIN"   ❌
```

Yudao dùng `.access("hasRole('ROLE_NORMAL')")` — dễ gây nhầm. HDL dùng `hasRole("NORMAL")`.

`@PreAuthorize("hasRole('ADMIN')")` — **không** viết `ROLE_ADMIN` trong `hasRole`.

---

## 8. PasswordEncoder

| | NoOp (yudao demo) | BCrypt (HDL) |
|---|---|---|
| Lưu | plaintext `"admin"` | hash `$2a$...` |
| Login form gõ | `admin` | vẫn gõ `admin` (so khớp bằng matches) |
| Production | ❌ Deprecated | ✅ |

Luôn `@Bean PasswordEncoder` và `encoder.encode(...)` khi build `UserDetails`.

---

## 9. Session / cookie (mô hình lab này)

```text
Login form thành công
  → Authentication lưu vào SecurityContext
  → SecurityContext lưu vào HTTP Session
  → Cookie JSESSIONID gửi về browser
Request sau: cookie → session → biết user
```

Đây là **stateful web auth** (form + cookie), không phải REST JWT:

| | Lab hiện tại | REST JWT (sau) |
|---|---|---|
| Login | Form → redirect | JSON + token |
| Credential sau | Cookie session | `Authorization: Bearer` |
| State | Server session | Thường STATELESS |
| Client | Browser | Mobile / SPA / API client |

`@RestController` vẫn dùng được với session — “REST controller” ≠ “REST auth stateless”.

CSRF mặc định **bật** với form login — curl POST `/login` có thể 403 nếu thiếu CSRF token; browser hoặc Basic (nếu bật) dễ test hơn.

---

## 10. Cách test nhanh

**Demo:** `http://localhost:8080/admin/demo` → login `user`/`user` → `Demo`.

**Demo-role (browser):**

1. `/test/demo` chưa login → 200  
2. Login `admin` → `/test/admin` 200, `/test/normal` 403  
3. Logout → login `normal` → `/test/admin` 403, `/test/normal` 200  
4. Method security: `normal` → `/demo/admin` **403** (sau khi có `@EnableMethodSecurity`)

Hai module cùng port 8080 — chỉ chạy một app tại một thời điểm.

Form login **không** bằng `curl -u` trừ khi bật thêm `httpBasic()`.

---

## 11. Checklist review Security (trước khi “test OK”)

Khi review / tự kiểm:

1. Có `@EnableMethodSecurity` nếu controller dùng `@PreAuthorize`?
2. URL rules trong `SecurityFilterChain` khớp path controller?
3. Boot 3 API: không còn `WebSecurityConfigurerAdapter` / `antMatchers`?
4. `hasRole("X")` khớp `.roles("X")` (không double `ROLE_`)?
5. Password BCrypt đã `encode`?
6. `jakarta.*` cho `@PermitAll`?

---

## 12. Không làm trong lab-01 (để sau)

| Chủ đề | Khi nào |
|---|---|
| User từ DB / JPA | Khi cần realistic |
| JWT monolith / STATELESS | Cuối lộ trình (không có lab yudao sạch; đọc overview `lab-68` OAuth JWT) |
| Session Redis đa instance | **`lab-26`** (tiếp theo) |
| OAuth2 | `lab-02` / `lab-68` — sau Security vững |
| Custom login page / JSON 401 | Khi làm API client |

---

## 13. HDL modules đã tạo

| Module | Nội dung |
|---|---|
| `lab-01-springsecurity-demo` | Auto Security + yml user + `AdminController` |
| `lab-01-springsecurity-demo-role` | `SecurityFilterChain`, BCrypt, 2 user, URL + method RBAC |

Ghi chú: `Spring Security.md` (file này).

---

## 14. Liên kết lộ trình

```text
lab-01 Security (session + form)  ✅
        ↓
lab-26 Distributed Session (session → Redis, nhiều instance)
        ↓
lab-29 Async
        ↓
... Pha 2 MQ ...
        ↓
(sau cùng) JWT monolith / OAuth overview
```
