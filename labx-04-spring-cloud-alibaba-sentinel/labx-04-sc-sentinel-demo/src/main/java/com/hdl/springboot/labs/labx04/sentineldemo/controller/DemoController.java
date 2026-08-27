package com.hdl.springboot.labs.labx04.sentineldemo.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Yudao demo01. Proof chính: GET /demo/echo sau khi Dashboard gán Flow QPS=1.
 * Resource URL (filter MVC) ≠ resource annotation. Dashboard phải chọn đúng tên (thường GET:/demo/echo).
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    /** Filter Sentinel chặn theo URL — chưa cần @SentinelResource. */
    @GetMapping("/echo")
    public String echo() {
        return "echo";
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    /** Degrade: thread.sleep để Dashboard rule RT/chậm. Không dùng cho proof QPS. */
    @GetMapping("/sleep")
    public String sleep() throws InterruptedException {
        Thread.sleep(100L);
        return "sleep";
    }

    /**
     * Hot param: resource tên demo_product_info_hot (annotation), không phải GET:/demo/product_info.
     * Rule hotspot trên Dashboard gắn param id.
     */
    @GetMapping("/product_info")
    @SentinelResource("demo_product_info_hot")
    public String productInfo(Integer id) {
        return "product id=" + id;
    }

    /**
     * API thủ công SphU: tự entry/exit. BlockException bắt tại đây, không qua ControllerAdvice
     * nếu return trong catch (không ném tiếp).
     */
    @GetMapping("/entry_demo")
    public String entryDemo() {
        Entry entry = null;
        try {
            entry = SphU.entry("entry_demo");
            return "ok";
        } catch (BlockException ex) {
            return "blocked:" + ex.getClass().getSimpleName();
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * blockHandler = hết rule Sentinel (QPS…). fallback = exception nghiệp vụ (id null), không phải flow.
     * Hai method phải public, cùng class; blockHandler thêm BlockException ở cuối.
     */
    @GetMapping("/annotations_demo")
    @SentinelResource(value = "annotations_demo_resource",
            blockHandler = "blockHandler",
            fallback = "fallback")
    public String annotationsDemo(@RequestParam(required = false) Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("id required");
        }
        return "success";
    }

    public String blockHandler(Integer id, BlockException ex) {
        return "block:" + ex.getClass().getSimpleName();
    }
    public String fallback(Integer id, Throwable throwable) {
        return "fallback:" + throwable.getMessage();
    }

}
