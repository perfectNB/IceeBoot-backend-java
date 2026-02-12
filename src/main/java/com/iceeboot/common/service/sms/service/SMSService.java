/**
 * SMS服务抽象接口
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.sms.service;

import com.iceeboot.common.service.sms.dto.SMSRequest;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * SMS服务抽象接口
 * 定义了短信发送的基本操作：单发、群发、模板短信等
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
public interface SMSService {
    
    /**
     * 获取厂商名称
     * @return 厂商名称
     */
    String getProvider();
    
    /**
     * 发送单条短信
     * @param request 短信请求
     * @return 发送响应
     */
    Mono<SMSResponse> sendSMS(SMSRequest request);
    
    /**
     * 发送模板短信
     * @param request 短信请求
     * @return 发送响应
     */
    Mono<SMSResponse> sendTemplateSMS(SMSRequest request);
    
    /**
     * 批量发送短信
     * @param request 短信请求（包含多个手机号）
     * @return 发送响应
     */
    Mono<SMSResponse> sendBatchSMS(SMSRequest request);
    
    /**
     * 发送验证码短信
     * @param request 短信请求
     * @return 发送响应
     */
    Mono<SMSResponse> sendVerificationCode(SMSRequest request);
    
    /**
     * 发送通知短信
     * @param request 短信请求
     * @return 发送响应
     */
    Mono<SMSResponse> sendNotification(SMSRequest request);
    
    /**
     * 发送营销短信
     * @param request 短信请求
     * @return 发送响应
     */
    Mono<SMSResponse> sendMarketing(SMSRequest request);
    
    /**
     * 查询短信发送状态
     * @param messageId 消息ID
     * @param request 请求参数（包含认证信息）
     * @return 查询响应
     */
    Mono<SMSResponse> querySMSStatus(String messageId, SMSRequest request);
    
    /**
     * 查询账户余额
     * @param request 请求参数（包含认证信息）
     * @return 余额信息
     */
    Mono<SMSResponse> queryBalance(SMSRequest request);
    
    /**
     * 验证配置
     * @param request 配置请求
     * @return 验证结果
     */
    Mono<Boolean> validateConfig(SMSRequest request);
    
    /**
     * 服务是否可用
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 获取支持的短信类型
     * @return 支持的短信类型列表
     */
    default List<String> getSupportedSMSTypes() {
        return List.of("verification", "notification", "marketing");
    }
    
    /**
     * 获取支持的功能列表
     * @return 支持的功能
     */
    default List<String> getSupportedFeatures() {
        return List.of("single", "batch", "template", "status_query", "balance_query");
    }
    
    /**
     * 获取默认配置
     * @return 默认配置
     */
    default SMSRequest getDefaultConfig() {
        return new SMSRequest()
                .setTimeout(30)
                .setUseHttps(true)
                .setSmsType("notification");
    }
    
    /**
     * 检查手机号格式
     * @param phoneNumber 手机号
     * @return 是否有效
     */
    default boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        // 简单的手机号验证（支持国内11位和国际格式）
        String cleanPhone = phoneNumber.replaceAll("[^0-9+]", "");
        return cleanPhone.matches("^(\\+?[1-9]\\d{1,14}|1[3-9]\\d{9})$");
    }
    
    /**
     * 检查短信内容长度
     * @param content 短信内容
     * @return 是否在允许范围内
     */
    default boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        // 一般短信长度限制在70个字符（英文）或35个字符（中文）
        return content.length() <= 500; // 设置一个较宽松的限制
    }
    
    /**
     * 获取短信条数（按70个字符计算）
     * @param content 短信内容
     * @return 短信条数
     */
    default int getSMSCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // 简化计算：每70个字符算一条短信
        return (int) Math.ceil((double) content.length() / 70);
    }
}