package cc.chuchiangdata.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 联系表单数据传输对象
 */
public class ContactFormDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱长度不能超过200个字符")
    private String email;

    @Size(max = 200, message = "组织名称长度不能超过200个字符")
    private String organization;

    @NotBlank(message = "需求描述不能为空")
    @Size(max = 2000, message = "需求描述长度不能超过2000个字符")
    private String requirements;

    @NotBlank(message = "人机验证不能为空")
    private String turnstileToken;

    // 构造函数
    public ContactFormDTO() {
    }

    public ContactFormDTO(String name, String email, String organization, String requirements, String turnstileToken) {
        this.name = name;
        this.email = email;
        this.organization = organization;
        this.requirements = requirements;
        this.turnstileToken = turnstileToken;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getTurnstileToken() {
        return turnstileToken;
    }

    public void setTurnstileToken(String turnstileToken) {
        this.turnstileToken = turnstileToken;
    }

    @Override
    public String toString() {
        return "ContactFormDTO{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", organization='" + organization + '\'' +
                ", requirements='" + requirements + '\'' +
                '}';
    }
}
