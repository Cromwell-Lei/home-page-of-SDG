package cc.chuchiangdata.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cloudflare Turnstile 人机验证服务
 */
@Service
public class TurnstileService {

    private static final Logger logger = LoggerFactory.getLogger(TurnstileService.class);

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    @Value("${cloudflare.turnstile.verify-url}")
    private String verifyUrl;

    private final RestTemplate restTemplate;

    public TurnstileService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 验证 Turnstile token
     *
     * @param token 前端传来的 cf-turnstile-response token
     * @return true 表示验证通过，false 表示验证失败
     */
    @SuppressWarnings("unchecked")
    public boolean verifyToken(String token) {
        if (token == null || token.isBlank()) {
            logger.warn("Turnstile token 为空");
            return false;
        }

        try {
            // 构建表单请求体
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("secret", secretKey);
            requestBody.add("response", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            // 调用 Cloudflare 验证接口
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.postForEntity(
                    verifyUrl, request, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody == null) {
                logger.error("Turnstile 验证响应体为空");
                return false;
            }

            boolean success = Boolean.TRUE.equals(responseBody.get("success"));

            if (success) {
                logger.info("Turnstile 验证通过");
            } else {
                logger.warn("Turnstile 验证失败, 错误码: {}", responseBody.get("error-codes"));
            }

            return success;

        } catch (Exception e) {
            logger.error("调用 Turnstile 验证接口异常", e);
            return false;
        }
    }
}
