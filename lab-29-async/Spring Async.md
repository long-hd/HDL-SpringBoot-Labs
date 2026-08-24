# Spring Async — Ghi chú học tập (lab-29)

> Tổng hợp kiến thức từ `lab-29-async`.  
> Yudao source: Boot 2.x (`AsyncResult` / `ListenableFuture`). HDL target: Boot 3.5 + `CompletableFuture`.  
> Đóng **Pha 1** lộ trình (Redis → Security → Session → Async).

---

## 1. Lab-29 đứng đâu trong concurrent?

```text
┌─────────────────────────────────────────────┐
│  Distributed (Redis lock, MQ consumer)      │  ← lab-11 Redisson, Pha 2
├─────────────────────────────────────────────┤
│  Spring @Async / TaskExecutor / @Scheduled  │  ← **lab-29**
├─────────────────────────────────────────────┤
│  java.util.concurrent                       │  ← Thread, Runnable,
│  (Executor, Future, Lock…)                  │     ThreadPoolExecutor
└─────────────────────────────────────────────┘
```

Lab dạy **application-level concurrency trong Spring**, không dạy lại JMM / `synchronized` từ đầu.

`Runnable` **có** nhưng ẩn: Spring AOP bọc `@Async` method thành task rồi `executor.execute(runnable)`.

---

## 2. Vấn đề lab giải quyết

Caller (HTTP / test) gọi việc chậm tuần tự → block thread:

```text
execute01 10s + execute02 5s ≈ 15s trên cùng thread
```

`@Async`: chạy trên **thread pool**, caller return sớm (fire-and-forget) hoặc giữ `CompletableFuture` để chờ / callback.

---

## 3. Hai submodule HDL

| Module | Mục tiêu |
|---|---|
| `lab-29-async-basic` | `@EnableAsync`, sync vs async, `CompletableFuture`, exception handler |
| `lab-29-async-multi-executor` | Hai `ThreadPoolTaskExecutor` + `@Async("beanName")` |

---

## 4. `Executor` vs `ThreadPoolTaskExecutor`

| | `Executor` (JDK) | `ThreadPoolTaskExecutor` (Spring) |
|---|---|---|
| Là gì | Interface: `execute(Runnable)` | Class Spring, bọc pool, lifecycle Bean |
| Quan hệ | Cha khái niệm | Implement `TaskExecutor` (Spring) ≈ dùng như Executor |

Boot thường có bean `applicationTaskExecutor`. `@Async` mặc định / `AsyncConfigurer.getAsyncExecutor()` trỏ vào pool đó.

---

## 5. Module basic — config

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    // inject applicationTaskExecutor + GlobalAsyncExceptionHandler
    // getAsyncExecutor() → taskExecutor
    // getAsyncUncaughtExceptionHandler() → handler
}
```

- Chỉ cần **một** `@EnableAsync` (config hoặc Application — đừng trùng hai chỗ).
- Handler chỉ bắt exception của `@Async` **void** (không phải Future mang lỗi về `join()`).

---

## 6. Return type `@Async` (Boot 3 — lệch yudao)

| Return | Boot 2 (yudao) | Boot 3.5 |
|---|---|---|
| `void` | OK | OK — fire-and-forget |
| `Integer` / kiểu thường | OK (caller = null) | **Cấm** → `IllegalArgumentException` |
| `Future` + `AsyncResult` | OK | Deprecated |
| `ListenableFuture` | OK | Deprecated |
| `CompletableFuture` | — | **Khuyên dùng** |

Fire-and-forget HDL: `@Async public void execute01Async()`.  
Có kết quả: `CompletableFuture.completedFuture(...)` — **không** lồng thêm `supplyAsync` (tránh double async).

---

## 7. Ý nghĩa các test (basic)

Giả sử sleep 10s + 5s:

| Test | Chứng minh | Thời gian caller (gần đúng) |
|---|---|---|
| `task01` sync | Tuần tự cùng thread | ~**15s** |
| `task02` `@Async` void | Fire-and-forget; pool `task-1`/`task-2` | ~**ms**; nên `sleep` cuối tránh WARN shutdown |
| `task03` + `join()`/`allOf` | Song song + chờ kết quả | ~**10s** |
| `task04` + `whenComplete` | Callback khi xong/lỗi | ~ thời gian task; `join()` để test không tắt sớm |
| `boom` + sleep | Exception void → `AsyncUncaughtExceptionHandler` | — |

**WARN** `Timed out while waiting for executor ... to terminate`: test/context shutdown trong khi task còn sleep — không phải `@Async` sai. Sửa: `sleep` / `join()` đủ lâu.

### `whenComplete` vs `thenAccept`

- `whenComplete((r, ex) -> …)` — đủ thay `ListenableFutureCallback` (success + fail).
- `thenAccept` — chỉ success; fail cần `exceptionally`. Lab chỉ cần `whenComplete`.

---

## 8. Module multi-executor

**Cách đơn giản (HDL):** tạo 2 bean `ThreadPoolTaskExecutor` trong Java — không cần yaml `execution-one` / `execution-two` của yudao.

```java
@Bean(name = "executorOne")
ThreadPoolTaskExecutor executorOne() {
    // core/max/queue + setThreadNamePrefix("task-one-"); initialize();
}
```

```java
@Async(AsyncConfig.EXECUTOR_ONE)  // hoặc "executorOne"
public void execute01() { ... }

@Async(AsyncConfig.EXECUTOR_TWO)
public void execute02() { ... }
```

Verify log: `thread=task-one-1` vs `task-two-1`.

### Ý yaml yudao (khi đọc, không bắt buộc code)

| Property | Ý | Java tương đương |
|---|---|---|
| `thread-name-prefix` | Tên thread log | `setThreadNamePrefix` |
| `pool.core-size` | Thread giữ sẵn | `setCorePoolSize` |
| `pool.max-size` | Tối đa khi quá tải | `setMaxPoolSize` |
| `pool.queue-capacity` | Hàng đợi khi core bận | `setQueueCapacity` |
| `pool.keep-alive` | Hủy thread thừa khi idle | `setKeepAliveSeconds` |
| `allow-core-thread-timeout` | Core cũng co được khi idle | `setAllowCoreThreadTimeOut` |
| `shutdown.await-termination` | Tắt app có chờ task | `setWaitForTasksToCompleteOnShutdown` |
| `await-termination-period` | Chờ tối đa | `setAwaitTerminationSeconds` |

Thứ tự pool điển hình: core → queue đầy → tạo đến max → reject.

---

## 9. Bẫy quan trọng

1. **Self-invocation:** `this.asyncMethod()` trong cùng class → không qua proxy → **không async**. Gọi qua bean Spring (`@Autowired`).
2. Thiếu `@EnableAsync` → annotation bị bỏ qua.
3. Test fire-and-forget: cần **sleep / latch / join**, không thì JVM tắt sớm.
4. Boot 3: không return `Integer` từ `@Async`.
5. SecurityContext / `@Transactional` **không** tự sang thread async (đọc overview; lab chưa đào sâu).

---

## 10. Concurrent — trả lời phỏng vấn (map ngắn)

**Java:** Thread, Runnable/Callable, Executor/thread pool, Future, đồng bộ shared state (race/deadlock).

**Spring (lab này):** `@Async` = submit lên `TaskExecutor`; cấu hình pool; nhiều executor; exception handler; proxy.

**Distributed:** lock Redis / MQ — nhiều process; khác `@Async` (nhiều thread một process).

---

## 11. Checklist Boot 2 → 3.5

- [ ] JUnit 5
- [ ] `CompletableFuture` thay `AsyncResult` / `ListenableFuture`
- [ ] `@Async` chỉ `void` hoặc Future-like
- [ ] `@EnableAsync` một lần
- [ ] Multi-executor: bean tay đủ cho lab (yaml yudao optional)

---

## 12. HDL modules

| Module | Nội dung |
|---|---|
| `lab-29-async-basic` | AsyncConfigurer, handler, sync/async/Future/callback/boom |
| `lab-29-async-multi-executor` | 2 pool + `@Async("…")` |

Ghi chú: file này (`Spring Async.md`).

---

## 13. Lộ trình

```text
Pha 1 ✅  Redis → Security → Session → Async (lab-29)
Pha 2 ~   lab-04 RabbitMQ → lab-03 Kafka
```

Consumer MQ cũng dùng thread pool — lab-29 là nền “nhiều thread trong một process”.
