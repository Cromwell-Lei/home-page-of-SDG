package cc.chuchiangdata.backend.controller;

import cc.chuchiangdata.backend.dto.ContactFormDTO;
import cc.chuchiangdata.backend.service.EmailService;
import cc.chuchiangdata.backend.service.TurnstileService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 联系表单控制器
 */
@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://127.0.0.1}", allowCredentials = "true")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    private final EmailService emailService;
    private final TurnstileService turnstileService;

    public ContactController(EmailService emailService, TurnstileService turnstileService) {
        this.emailService = emailService;
        this.turnstileService = turnstileService;
    }

    /**
     * 提交联系表单
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitContactForm(
            @Valid @RequestBody ContactFormDTO formData,
            BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        // 验证失败
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));

            logger.warn("表单验证失败: {}", sanitizeForLog(errorMessage));
            response.put("success", false);
            response.put("message", "表单验证失败: " + errorMessage);
            return ResponseEntity.badRequest().body(response);
        }

        // Cloudflare Turnstile 人机验证
        if (!turnstileService.verifyToken(formData.getTurnstileToken())) {
            logger.warn("Turnstile 验证失败: 邮箱={}", sanitizeForLog(formData.getEmail()));
            response.put("success", false);
            response.put("message", "人机验证失败，请重新验证后再提交。");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 发送邮件
            emailService.sendContactFormEmail(formData);

            logger.info("成功处理联系表单: 姓名={}, 邮箱={}", sanitizeForLog(formData.getName()),
                    sanitizeForLog(formData.getEmail()));

            response.put("success", true);
            response.put("message", "感谢您的咨询!我们已收到您的信息,将在 1-3 个工作日内回复您。");
            return ResponseEntity.ok(response);

        } catch (MessagingException e) {
            logger.error("发送邮件失败", e);
            response.put("success", false);
            response.put("message", "抱歉,提交失败。请稍后重试或直接联系我们。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "contact-form");
        return ResponseEntity.ok(response);
    }

    /**
     * 过滤日志中的控制字符，防止日志伪造
     */
    private String sanitizeForLog(String input) {
        if (input == null)
            return "null";
        return input.replaceAll("[\\r\\n\\t]", "_");
    }
}
