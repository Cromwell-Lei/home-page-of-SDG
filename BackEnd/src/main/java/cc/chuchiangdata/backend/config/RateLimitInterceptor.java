package cc.chuchiangdata.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求速率限制拦截器
 * 防止恶意滥用联系表单
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    // 每分钟最大请求次数
    private static final int MAX_REQUESTS_PER_MINUTE = 3;

    // 时间窗口(毫秒)
    private static final long TIME_WINDOW_MS = 60000; // 1分钟

    // 存储每个 IP 的请求计数和时间戳
    private final Map<String, RequestRecord> requestMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String clientIp = getClientIp(request);
        long currentTime = System.currentTimeMillis();

        // 清理过期记录(定期清理,避免内存泄漏)
        cleanupExpiredRecords(currentTime);

        // 获取或创建请求记录
        RequestRecord record = requestMap.computeIfAbsent(clientIp, k -> new RequestRecord(currentTime));

        // 检查是否在时间窗口内
        if (currentTime - record.windowStart > TIME_WINDOW_MS) {
            // 重置时间窗口
            record.windowStart = currentTime;
            record.requestCount.set(1);
            return true;
        }

        // 检查请求次数
        int count = record.requestCount.incrementAndGet();
        if (count > MAX_REQUESTS_PER_MINUTE) {
            logger.warn("请求频率超限: IP={}, 次数={}", clientIp, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁,请稍后再试\"}");
            return false;
        }

        return true;
    }

    /**
     * 获取客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个代理,取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 清理过期的请求记录
     */
    private void cleanupExpiredRecords(long currentTime) {
        requestMap.entrySet().removeIf(entry -> currentTime - entry.getValue().windowStart > TIME_WINDOW_MS * 2);
    }

    /**
     * 请求记录内部类
     */
    private static class RequestRecord {
        volatile long windowStart;
        final AtomicInteger requestCount;

        RequestRecord(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(1);
        }
    }
}
