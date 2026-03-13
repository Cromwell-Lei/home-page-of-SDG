/**
 * 前端 API 配置文件（服务器部署用）
 * 
 * 此文件统一管理所有 API 基础 URL，方便在不同环境（开发/生产）之间切换
 * 
 * 使用方法：
 * 1. 在 HTML 文件的 <head> 中引入此文件：<script src="config.js"></script>
 * 2. 在 JavaScript 代码中使用：window.AppConfig.API_BASE 或 window.AppConfig.API_AUTH_BASE
 * 
 * 环境切换：
 * - 开发环境：使用 localhost:8080
 * - 生产环境：修改为实际的服务器地址，例如 'https://your-domain.com/api'
 */

(function () {
    // 开发环境配置
    const DEV_CONFIG = {
        // 主 API 基础路径
        API_BASE: '/api',
        // 认证相关 API 基础路径
        API_AUTH_BASE: '/api/auth',
        // 管理员 API 基础路径（与 API_BASE 相同，为保持语义清晰）
        API_ADMIN_BASE: '/api'
    };

    // 生产环境配置（示例）
    const PROD_CONFIG = {
        API_BASE: '/api',
        API_AUTH_BASE: '/api/auth',
        API_ADMIN_BASE: '/api'
    };

    // 自动检测环境（通过 hostname 判断）
    // 如果是 localhost 或 127.0.0.1，使用开发配置；否则使用生产配置
    const isLocalhost = window.location.hostname === 'localhost' ||
        window.location.hostname === '127.0.0.1';

    // 也可以手动切换环境：将下面这行的注释取消，强制使用某个配置
    // const config = DEV_CONFIG;  // 强制使用开发环境
    // const config = PROD_CONFIG; // 强制使用生产环境

    // 默认根据 hostname 自动选择
    const config = isLocalhost ? DEV_CONFIG : PROD_CONFIG;

    // 暴露到全局作用域
    window.AppConfig = config;

    // 开发环境下在控制台输出配置信息
    if (isLocalhost) {
        console.log('[AppConfig] 当前使用开发环境配置:', config);
    }
})();
