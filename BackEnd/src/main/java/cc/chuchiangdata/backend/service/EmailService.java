package cc.chuchiangdata.backend.service;

import cc.chuchiangdata.backend.dto.ContactFormDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件服务
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.recipient}")
    private String recipientEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送联系表单邮件
     * 
     * @param formData 表单数据
     * @throws MessagingException 邮件发送异常
     */
    public void sendContactFormEmail(ContactFormDTO formData) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            // 设置邮件基本信息
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("【联系我们】来自 " + HtmlUtils.htmlEscape(formData.getName()) + " 的咨询");

            // 设置 HTML 格式的邮件内容
            String htmlContent = buildHtmlEmailContent(formData);
            helper.setText(htmlContent, true);

            // 设置回复地址为提交者的邮箱
            if (formData.getEmail() != null && !formData.getEmail().isEmpty()) {
                helper.setReplyTo(formData.getEmail());
            }

            // 发送邮件
            mailSender.send(message);
            logger.info("成功发送联系表单邮件: {}", formData.getEmail());

        } catch (MessagingException e) {
            logger.error("发送邮件失败: {}", formData.getEmail(), e);
            throw e;
        }
    }

    /**
     * 构建 HTML 格式的邮件内容
     */
    private String buildHtmlEmailContent(ContactFormDTO formData) {
        // 转义用户输入,防止 XSS
        String name = HtmlUtils.htmlEscape(formData.getName());
        String email = HtmlUtils.htmlEscape(formData.getEmail());
        String organization = formData.getOrganization() != null ? HtmlUtils.htmlEscape(formData.getOrganization())
                : "未填写";
        String requirements = HtmlUtils.htmlEscape(formData.getRequirements())
                .replace("\n", "<br>");

        // 当前时间
        String submitTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f5f7fa;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #2a5cff 0%%, #2dd4bf 100%%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 30px;
                        }
                        .info-table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin: 20px 0;
                        }
                        .info-table tr {
                            border-bottom: 1px solid #e5e7eb;
                        }
                        .info-table tr:last-child {
                            border-bottom: none;
                        }
                        .info-table td {
                            padding: 15px 0;
                            vertical-align: top;
                        }
                        .info-table .label {
                            width: 100px;
                            font-weight: 600;
                            color: #374151;
                        }
                        .info-table .value {
                            color: #6b7280;
                            line-height: 1.6;
                        }
                        .footer {
                            background-color: #f9fafb;
                            padding: 20px 30px;
                            text-align: center;
                            color: #9ca3af;
                            font-size: 12px;
                            border-top: 1px solid #e5e7eb;
                        }
                        .badge {
                            display: inline-block;
                            padding: 4px 12px;
                            background-color: #dbeafe;
                            color: #1e40af;
                            border-radius: 12px;
                            font-size: 12px;
                            font-weight: 600;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📬 新的联系表单提交</h1>
                        </div>
                        <div class="content">
                            <p style="color: #6b7280; margin-top: 0;">您收到了一条来自网站"联系我们"的新消息:</p>

                            <table class="info-table">
                                <tr>
                                    <td class="label"> 姓名:</td>
                                    <td class="value">%s</td>
                                </tr>
                                <tr>
                                    <td class="label"> 邮箱:</td>
                                    <td class="value"><a href="mailto:%s" style="color: #2a5cff; text-decoration: none;">%s</a></td>
                                </tr>
                                <tr>
                                    <td class="label"> 组织:</td>
                                    <td class="value">%s</td>
                                </tr>
                                <tr>
                                    <td class="label"> 需求:</td>
                                    <td class="value">%s</td>
                                </tr>
                                <tr>
                                    <td class="label"> 提交时间:</td>
                                    <td class="value">%s</td>
                                </tr>
                            </table>

                            <div style="margin-top: 25px; padding: 15px; background-color: #f0f9ff; border-left: 4px solid #2a5cff; border-radius: 4px;">
                                <p style="margin: 0; color: #1e40af; font-size: 14px;">
                                    <strong>💡 提示:</strong> 您可以直接回复此邮件与提交者联系。
                                </p>
                            </div>
                        </div>
                        <div class="footer">
                            <p style="margin: 0;">此邮件由网站联系表单自动发送</p>
                            <p style="margin: 5px 0 0 0;">ChuChiang Data - Synthetic Data Solutions</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name, email, email, organization, requirements, submitTime);
    }
}
