# Spring Boot Redis — Ghi chú học tập (lab-11)

> Tổng hợp kiến thức từ quá trình học `lab-11-spring-data-redis`.  
> Yudao source: Boot 2.x + Jedis. HDL target: Boot 3.5 + Lettuce + Redisson.

---

## 1. Kiến trúc tổng quan

```text
Spring Data Redis
    └── RedisTemplate / StringRedisTemplate
            └── RedisConnectionFactory
                    ├── LettuceConnectionFactory (default Boot 3)
                    └── JedisConnectionFactory  (cần exclude Lettuce, thêm Jedis)

Redisson (lớp cao hơn)
    └── RedissonClient
            ├── RLock, RFairLock, RReadWriteLock
            ├── RRateLimiter
            ├── RBucket, RMap, RSet, ...
            └── RTopic (Pub/Sub riêng)
```

**Lettuce vs Jedis:**

| | Lettuce | Jedis |
|---|---|---|
| Default Boot 3 | ✅ | Cần thêm dependency + exclude |
| Non-blocking | Netty, async | Thread-per-connection |
| Thread-safe | Một connection dùng được nhiều thread | Mỗi thread cần connection riêng |

**Redisson không thay Lettuce.** Starter Redisson bọc connection pool riêng, thay `RedisConnectionFactory` bằng `RedissonConnectionFactory`. Hai cái phục vụ mục đích khác nhau:

- `RedisTemplate` → cache, CRUD key/value thông thường
- `RedissonClient` → distributed lock, rate limiter, object phân tán

---

## 2. Dependency (Boot 3.5)

**with-lettuce:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- Jackson bắt buộc khi dùng GenericJackson2JsonRedisSerializer -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-json</artifactId>
</dependency>
```

> `spring-boot-starter-data-redis` không kéo Jackson. Thiếu `starter-json` → `ClassNotFoundException: TypeResolverBuilder` khi khởi động.

**with-redisson:**

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>4.4.0</version> <!-- chọn version khớp Boot 3.5 -->
</dependency>
```

> Starter Redisson đã kéo Spring Data Redis. Không cần thêm `starter-data-redis`.

---

## 3. Cấu hình `application.yml` (Boot 3)

```yaml
spring:
  data:
    redis:          # Boot 3: spring.data.redis (không phải spring.redis như Boot 2)
      host: localhost
      port: 6379
```

> **Khác yudao:** Boot 2.x dùng `spring.redis.*`. Boot 3 đổi thành `spring.data.redis.*`.  
> Copy yml yudao sang → Boot 3 không bind → context lên nhưng kết nối sai.

---

## 4. RedisTemplate & Serializer

```java
@Bean
RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    var stringSerializer = new StringRedisSerializer();
    var jsonSerializer = new GenericJackson2JsonRedisSerializer();

    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
}
```

### GenericJackson2JsonRedisSerializer vs Jackson2JsonRedisSerializer

| | `GenericJackson2JsonRedisSerializer` | `Jackson2JsonRedisSerializer` |
|---|---|---|
| JSON lưu | Có thêm `@class` field (FQCN) | Chỉ fields của object |
| Deserialize | Tự động nhờ `@class` | Phải chỉ định class khi tạo serializer |
| Cross-service | ❌ Rủi ro nếu service khác đọc (FQCN khác) | ✅ Field-only, an toàn hơn |
| Cùng service | ✅ Dùng được | ✅ |

> **Nguyên tắc:** Same-service replicas → `GenericJackson2JsonRedisSerializer` OK.  
> Cross-service share Redis → dùng `StringRedisTemplate` + Jackson thủ công, không FQCN.

---

## 5. Cache Object Pattern

```java
@Repository
public class UserCacheDao {
    private static final String KEY_PATTERN = "user:%d";
    private final RedisTemplate<String, Object> redisTemplate;

    public UserCacheObject get(Integer id) {
        Object value = redisTemplate.opsForValue().get(String.format(KEY_PATTERN, id));
        return value instanceof UserCacheObject user ? user : null;
    }

    public void set(Integer id, UserCacheObject obj) {
        redisTemplate.opsForValue().set(String.format(KEY_PATTERN, id), obj);
    }
}
```

**Lưu ý production:**
- Luôn set TTL: `opsForValue().set(key, value, Duration.ofMinutes(30))`
- Không có TTL → key tồn tại mãi → memory leak

---

## 6. Lua Script (CAS — Compare And Set)

**compareAndSet.lua:**

```lua
if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return 0
end
redis.call('SET', KEYS[1], ARGV[2])
return 1
```

**Gọi từ Java:**

```java
ClassPathResource resource = new ClassPathResource("lua/compareAndSet.lua");
String script = resource.getContentAsString(StandardCharsets.UTF_8);
RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

Long result = stringRedisTemplate.execute(
    redisScript,
    Collections.singletonList("key:1"),
    "expected_value",
    "new_value"
);
// 1 = CAS thành công, 0 = giá trị hiện tại khác expected
```

**Tại sao Lua thay vì GET + SET riêng?**

GET rồi SET là 2 lệnh → không atomic → race condition giữa 2 lệnh.  
Lua script chạy atomic trên Redis → không có kẽ hở.

**Khác yudao:**
- Yudao dùng `commons-io` để đọc file. HDL dùng `ClassPathResource.getContentAsString()` (JDK, không cần thêm dependency).

---

## 7. Pub/Sub

```java
// Config: đăng ký listener
@Bean
public RedisMessageListenerContainer listenerContainer(RedisConnectionFactory connectionFactory) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(new TestChannelTopicMessageListener(), new ChannelTopic("TEST"));
    container.addMessageListener(new TestPatternTopicMessageListener(), new ChannelTopic("TEST"));
    return container;
}

// Listener
public class TestChannelTopicMessageListener implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        System.out.println("Received: " + message);
    }
}

// Publish
stringRedisTemplate.convertAndSend("TEST", "hello");
```

### Pub/Sub vs Redis Stream

| | Pub/Sub | Redis Stream |
|---|---|---|
| Lưu message | ❌ Fire-and-forget | ✅ Persist |
| Consumer offline | Mất message | Đọc lại được |
| Consumer group | ❌ | ✅ |
| Dùng khi | Notification, realtime | MQ-like, reliable |

### ChannelTopic vs PatternTopic

- `ChannelTopic("TEST")` → subscribe đúng channel `TEST`
- `PatternTopic("TEST*")` → subscribe mọi channel bắt đầu bằng `TEST`

> **Lưu ý:** `addMessageListener(listener, new ChannelTopic("TEST"))` với `PatternTopicMessageListener` vẫn nhận message từ channel `TEST` — nhưng không phải pattern match. Để demo PatternTopic đúng, phải dùng `new PatternTopic("TEST*")`.

### Test Pub/Sub với CountDownLatch (thay Thread.sleep)

```java
// Trong listener: latch.countDown() sau mỗi onMessage
// Trong test:
CountDownLatch latch = new CountDownLatch(expectedCount);
TestChannelTopicMessageListener.latch = latch;
stringRedisTemplate.convertAndSend("TEST", "msg");
assertTrue(latch.await(5, TimeUnit.SECONDS));
```

`Thread.sleep` = đoán thời gian. `CountDownLatch` = đợi đúng sự kiện.

---

## 8. Pipeline

Gom nhiều lệnh Redis thành 1 round-trip → giảm network latency.

```java
List<Object> results = stringRedisTemplate.executePipelined(connection -> {
    for (int i = 0; i < 1000; i++) {
        connection.stringCommands().set(("key:" + i).getBytes(), ("val:" + i).getBytes());
    }
    return null; // bắt buộc trả null
});
```

**Lưu ý:**
- Không atomic (khác `MULTI/EXEC`)
- Các lệnh không phụ thuộc nhau (không dùng kết quả lệnh trước)
- Lettuce dùng multiplexing → lợi ích pipeline thấp hơn so với Jedis
- Dùng nhiều cho: cache warm-up, bulk insert, analytics counter

> HDL: đọc overview, skip code lại lần đầu.

---

## 9. Redisson — RLock (Distributed Lock)

### Cơ chế bên trong

```text
lock.lock() / tryLock()
    → Lua trên Redis: SET "lockKey" <clientId:threadId> NX PX <ttl>
    → Nếu bận: subscribe Pub/Sub channel, ngủ, được đánh thức khi unlock
unlock()
    → Lua: giảm reentrant counter / xóa key
    → Pub/Sub: notify các waiter
```

### Watchdog vs Lease

| | `lock()` — không lease | `lock(10, SECONDS)` — có lease |
|---|---|---|
| Watchdog | ✅ Gia hạn TTL mỗi 10s | ❌ Không |
| Tự hết hạn | Không (trừ khi JVM chết) | Sau 10s |
| Dùng khi | Task dài, không biết thời gian | Task ngắn, thời gian cố định |
| Rủi ro | Phải `unlock` trong `finally` | Lease hết trước task xong → race condition |

### Pattern đúng (production)

```java
RLock lock = redissonClient.getLock("order:pay:" + orderId);
boolean acquired = false;
try {
    acquired = lock.tryLock(3, TimeUnit.SECONDS); // wait 3s, watchdog bật
    if (!acquired) {
        throw new ConflictException("Đang xử lý, thử lại sau");
    }
    // critical section
} finally {
    if (acquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

**Tại sao cần `isHeldByCurrentThread()`?**

Nếu dùng lease và task chạy quá lease time → key bị xóa → process khác lấy lock → unlock trong finally sẽ unlock của người khác → `IllegalMonitorStateException` hoặc mất an toàn.

### Các loại lock

| API | Đặc điểm |
|---|---|
| `RLock` | Reentrant mutex, phổ biến nhất |
| `RFairLock` | FIFO, giảm starvation |
| `RReadWriteLock` | Nhiều đọc / một ghi |
| `RMultiLock` | Khóa nhiều key cùng lúc |
| `RFencedLock` | Có fencing token, bảo vệ external resource |
| `RRedLock` | **Deprecated** — không dùng |

### Sai lầm phổ biến

- `lock(lease)` cho task dài → lease hết sớm, hai process vào cùng lúc
- Không `unlock` trong `finally` → lock bị giữ cho đến khi JVM chết hoặc watchdog dừng
- Lock key quá rộng (`"payment"` thay vì `"payment:order:123"`) → block không cần thiết
- Bỏ qua return value của `tryLock` → chạy critical section dù không có lock

---

## 10. Redisson — RRateLimiter (Token Bucket)

### Cơ chế

Token bucket phân tán trên Redis. Mỗi kỳ T giây nạp N token. `tryAcquire()` lấy 1 token, hết token trả `false`.

```java
RRateLimiter rateLimiter = redissonClient.getRateLimiter("sms:gateway");
rateLimiter.trySetRate(RateType.OVERALL, 100, Duration.ofSeconds(1)); // 100/giây
// API cũ (deprecated): trySetRate(OVERALL, 100, 1, RateIntervalUnit.SECONDS)

if (!rateLimiter.tryAcquire()) {
    throw new TooManyRequestsException("Vượt giới hạn tốc độ");
}
sendSms(phone, content);
```

### RateType

| | `OVERALL` | `PER_CLIENT` |
|---|---|---|
| Quota | Tổng tất cả instance | Mỗi Redisson client riêng |
| Dùng khi | Giới hạn hệ thống (SMS gateway, API third-party) | Giới hạn per-user/per-pod |

### `trySetRate` vs `setRate`

| | `trySetRate` | `setRate` |
|---|---|---|
| Nếu key đã có | Không làm gì, trả `false` | Ghi đè + reset state |
| Dùng khi | Khởi tạo lần đầu | Muốn đổi rate |

> **Bẫy:** Nếu Redis còn key từ lần chạy trước, `trySetRate` im lặng, rate cũ còn đó. Test nhiều lần: dùng tên key riêng mỗi test, hoặc dùng `setRate`.

### Khác RLock

| | `RLock` | `RRateLimiter` |
|---|---|---|
| Câu hỏi | Ai đang làm? | Bao nhiêu lần đã làm? |
| Giới hạn | 1 người cùng lúc | N lần trong T giây |
| Dùng khi | Tránh race condition | Throttle tần suất |
| Ví dụ | Thanh toán đơn hàng | SMS/API quota |

### Thực tế dùng ở đâu

- Giới hạn gọi SMS/email gateway (OVERALL quota)
- Giới hạn request API public per-user
- Bảo vệ tài nguyên đắt (heavy report, AI inference)
- **Không dùng cho login fail limit** → dùng `INCR` + `EXPIRE` Redis thuần (cần đếm + reset + biết count còn lại)

---

## 11. Multi-pod — lock/rate limit hoạt động thế nào?

Pod không "biết nhau". Tất cả cùng kết nối **một Redis** (hoặc cluster) → Redis là điểm đồng thuận.

```text
Pod A ─┐
Pod B ─┼──→ Redis → lock key "anylock" → chỉ 1 pod giữ được
Pod C ─┘
```

Topology thay đổi chỉ ở `application.yml` (single → sentinel → cluster). Code lock/rate limiter **không đổi**.

| Topology | Config |
|---|---|
| Single node | `spring.data.redis.host/port` |
| Sentinel | `redisson.useSentinelServers()` |
| Cluster | `redisson.useClusterServers()` — key hash vào master cụ thể |

---

## 12. Redisson — cơ chế `getLock` / `getRateLimiter`

```text
getLock("anylock")          → Java proxy trong JVM, Redis chưa có gì
lock.lock()                 → LÚC NÀY mới tạo key trên Redis

getRateLimiter("r")         → Java proxy trong JVM, Redis chưa có gì
trySetRate(...)             → LÚC NÀY tạo HASH key trên Redis
tryAcquire()                → Đọc/ghi bucket trên Redis
```

**Lazy pattern** — `get*()` chỉ khai báo tên key, không đụng Redis. Không cần seed key trước.

---

## 13. Xem Javadoc Redisson

Mặc định IDE chỉ tải JAR (bytecode), không có sources → hover không thấy doc, decompiled không có comment.

**Cách xem:**

```bash
# Tải sources JAR về local Maven repo
mvn dependency:sources

# Hoặc chỉ Redisson
mvn dependency:get -Dartifact=org.redisson:redisson:4.4.0:jar:sources
```

Hoặc trong IDE: chuột phải `pom.xml` → **Maven → Download Sources**.

Hoặc xem online: `https://www.javadoc.io/doc/org.redisson/redisson/4.4.0`

---

## 14. HDL modules đã tạo

| Module | Nội dung |
|---|---|
| `lab-11-spring-data-redis-with-lettuce` | RedisTemplate, Jackson serializer, UserCacheDao, Lua CAS, Pub/Sub (Channel + Pattern) |
| `lab-11-spring-data-redis-with-redisson` | RedissonClient, RLock, RRateLimiter |

**Skip (đọc overview):** Pipeline, Redis Transaction (`MULTI/EXEC`), Redisson collections/services.

---

## 15. Checklist 2.x → 3.5 (Redis)

- `spring.redis.*` → `spring.data.redis.*`
- Không exclude Lettuce (Boot 3 default)
- Thêm `spring-boot-starter-json` khi dùng `GenericJackson2JsonRedisSerializer`
- Không copy Fastjson / `JSONUtil` của yudao
- `commons-io` → `ClassPathResource.getContentAsString()`
- JUnit 5 (`@Test` từ `org.junit.jupiter`), không `@RunWith(SpringRunner.class)`
- Redisson starter: chọn version khớp Boot 3.5 (không dùng 3.11.3 của yudao)
- `RateIntervalUnit` deprecated → dùng `Duration`
