package com.iceeboot.common.service.email.template;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 邮件模板类
 * 提供常用的邮件模板，包括入职恭喜、合同审批、中奖通知等
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Accessors(chain = true)
public class EmailTemplate {
    
    /**
     * 模板ID
     */
    private String templateId;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * HTML内容
     */
    private String htmlContent;
    
    /**
     * 纯文本内容
     */
    private String textContent;
    
    /**
     * 模板类型
     */
    private String templateType;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 模板常量类
     */
    public static final class Templates {
        public static final String WELCOME_ONBOARD = "welcome_onboard";
        public static final String CONTRACT_APPROVAL = "contract_approval";
        public static final String LOTTERY_WINNER = "lottery_winner";
    }
    
    /**
     * 恭喜入职模板
     * 
     * @return 入职恭喜邮件模板
     */
    public static EmailTemplate createWelcomeOnboardTemplate() {
        String subject = "🎉 欢迎加入${companyName}大家庭！";
        
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang='zh-CN'>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <title>欢迎入职</title>\n" +
                "    <style>\n" +
                "        body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f5f5f5; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); overflow: hidden; }\n" +
                "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }\n" +
                "        .header h1 { margin: 0; font-size: 28px; font-weight: bold; }\n" +
                "        .content { padding: 30px; }\n" +
                "        .welcome-message { font-size: 18px; color: #2c3e50; margin-bottom: 20px; }\n" +
                "        .info-box { background-color: #f8f9fa; border-left: 4px solid #007bff; padding: 15px; margin: 20px 0; border-radius: 5px; }\n" +
                "        .info-item { margin: 10px 0; }\n" +
                "        .info-label { font-weight: bold; color: #495057; display: inline-block; width: 100px; }\n" +
                "        .highlight { color: #007bff; font-weight: bold; }\n" +
                "        .tips { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }\n" +
                "        .tips h3 { color: #856404; margin-top: 0; }\n" +
                "        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 14px; }\n" +
                "        .emoji { font-size: 24px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <h1><span class='emoji'>🎉</span> 欢迎加入${companyName}！</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                "            <div class='welcome-message'>\n" +
                "                亲爱的 <span class='highlight'>${employeeName}</span>，\n" +
                "            </div>\n" +
                "            <p>恭喜您正式成为${companyName}的一员！我们非常高兴能够与您一起开启这段精彩的职业旅程。</p>\n" +
                "            \n" +
                "            <div class='info-box'>\n" +
                "                <h3><span class='emoji'>📋</span> 您的入职信息</h3>\n" +
                "                <div class='info-item'><span class='info-label'>姓名：</span>${employeeName}</div>\n" +
                "                <div class='info-item'><span class='info-label'>部门：</span>${department}</div>\n" +
                "                <div class='info-item'><span class='info-label'>职位：</span>${position}</div>\n" +
                "                <div class='info-item'><span class='info-label'>入职日期：</span>${startDate}</div>\n" +
                "                <div class='info-item'><span class='info-label'>工号：</span>${employeeId}</div>\n" +
                "                <div class='info-item'><span class='info-label'>直属上级：</span>${manager}</div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class='tips'>\n" +
                "                <h3><span class='emoji'>💡</span> 入职小贴士</h3>\n" +
                "                <ul>\n" +
                "                    <li>请于入职当天上午9:00准时到达公司前台报到</li>\n" +
                "                    <li>请携带身份证原件及复印件、学历证书等相关材料</li>\n" +
                "                    <li>建议提前了解公司文化和部门职责</li>\n" +
                "                    <li>如有任何疑问，请随时联系HR部门</li>\n" +
                "                </ul>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p>我们相信，凭借您的才华和努力，一定能够在${companyName}这个大家庭中发光发热，实现个人价值与公司发展的双赢！</p>\n" +
                "            \n" +
                "            <p>再次欢迎您的加入，期待与您共同创造美好的未来！</p>\n" +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p>此邮件由${companyName}人力资源部自动发送</p>\n" +
                "            <p>如有疑问，请联系：${hrEmail} | ${hrPhone}</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        String textContent = "亲爱的${employeeName}，\n\n" +
                "恭喜您正式成为${companyName}的一员！\n\n" +
                "您的入职信息：\n" +
                "姓名：${employeeName}\n" +
                "部门：${department}\n" +
                "职位：${position}\n" +
                "入职日期：${startDate}\n" +
                "工号：${employeeId}\n" +
                "直属上级：${manager}\n\n" +
                "入职小贴士：\n" +
                "1. 请于入职当天上午9:00准时到达公司前台报到\n" +
                "2. 请携带身份证原件及复印件、学历证书等相关材料\n" +
                "3. 建议提前了解公司文化和部门职责\n" +
                "4. 如有任何疑问，请随时联系HR部门\n\n" +
                "我们相信，凭借您的才华和努力，一定能够在${companyName}这个大家庭中发光发热！\n\n" +
                "再次欢迎您的加入！\n\n" +
                "${companyName}人力资源部\n" +
                "联系方式：${hrEmail} | ${hrPhone}";
        
        return new EmailTemplate()
                .setTemplateId(Templates.WELCOME_ONBOARD)
                .setTemplateName("恭喜入职模板")
                .setSubject(subject)
                .setHtmlContent(htmlContent)
                .setTextContent(textContent)
                .setTemplateType("notification")
                .setDescription("新员工入职欢迎邮件模板，包含入职信息和注意事项");
    }
    
    /**
     * 合同审批模板
     * 
     * @return 合同审批邮件模板
     */
    public static EmailTemplate createContractApprovalTemplate() {
        String subject = "📋 合同审批通知 - ${contractTitle}";
        
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang='zh-CN'>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <title>合同审批通知</title>\n" +
                "    <style>\n" +
                "        body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f5f5f5; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); overflow: hidden; }\n" +
                "        .header { background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 30px; text-align: center; }\n" +
                "        .header h1 { margin: 0; font-size: 26px; font-weight: bold; }\n" +
                "        .content { padding: 30px; }\n" +
                "        .status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: bold; font-size: 14px; }\n" +
                "        .status-pending { background-color: #fff3cd; color: #856404; }\n" +
                "        .status-approved { background-color: #d4edda; color: #155724; }\n" +
                "        .status-rejected { background-color: #f8d7da; color: #721c24; }\n" +
                "        .contract-info { background-color: #f8f9fa; border: 1px solid #dee2e6; padding: 20px; border-radius: 8px; margin: 20px 0; }\n" +
                "        .info-row { display: flex; margin: 10px 0; }\n" +
                "        .info-label { font-weight: bold; color: #495057; min-width: 120px; }\n" +
                "        .info-value { color: #212529; }\n" +
                "        .action-section { background-color: #e3f2fd; border: 1px solid #bbdefb; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center; }\n" +
                "        .btn { display: inline-block; padding: 12px 24px; margin: 5px; text-decoration: none; border-radius: 5px; font-weight: bold; }\n" +
                "        .btn-approve { background-color: #28a745; color: white; }\n" +
                "        .btn-reject { background-color: #dc3545; color: white; }\n" +
                "        .btn-view { background-color: #007bff; color: white; }\n" +
                "        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 14px; }\n" +
                "        .emoji { font-size: 20px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <h1><span class='emoji'>📋</span> 合同审批通知</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                "            <p>尊敬的 <strong>${approverName}</strong>，</p>\n" +
                "            \n" +
                "            <p>您有一份合同需要审批，详细信息如下：</p>\n" +
                "            \n" +
                "            <div class='contract-info'>\n" +
                "                <h3><span class='emoji'>📄</span> 合同信息</h3>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>合同标题：</div>\n" +
                "                    <div class='info-value'>${contractTitle}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>合同编号：</div>\n" +
                "                    <div class='info-value'>${contractNumber}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>申请人：</div>\n" +
                "                    <div class='info-value'>${applicantName}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>申请部门：</div>\n" +
                "                    <div class='info-value'>${department}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>合同金额：</div>\n" +
                "                    <div class='info-value'>${contractAmount}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>合同期限：</div>\n" +
                "                    <div class='info-value'>${contractPeriod}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>申请时间：</div>\n" +
                "                    <div class='info-value'>${applyTime}</div>\n" +
                "                </div>\n" +
                "                <div class='info-row'>\n" +
                "                    <div class='info-label'>当前状态：</div>\n" +
                "                    <div class='info-value'><span class='status-badge status-pending'>待审批</span></div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class='action-section'>\n" +
                "                <h3><span class='emoji'>⚡</span> 快速操作</h3>\n" +
                "                <p>请点击以下按钮进行审批操作：</p>\n" +
                "                <a href='${approveUrl}' class='btn btn-approve'>✅ 同意</a>\n" +
                "                <a href='${rejectUrl}' class='btn btn-reject'>❌ 拒绝</a>\n" +
                "                <a href='${viewUrl}' class='btn btn-view'>👁️ 查看详情</a>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p><strong>备注：</strong>${remarks}</p>\n" +
                "            \n" +
                "            <p>请您在收到此邮件后尽快处理，如有疑问请联系申请人或相关部门。</p>\n" +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p>此邮件由${companyName}合同管理系统自动发送</p>\n" +
                "            <p>系统时间：${systemTime}</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        String textContent = "尊敬的${approverName}，\n\n" +
                "您有一份合同需要审批：\n\n" +
                "合同信息：\n" +
                "合同标题：${contractTitle}\n" +
                "合同编号：${contractNumber}\n" +
                "申请人：${applicantName}\n" +
                "申请部门：${department}\n" +
                "合同金额：${contractAmount}\n" +
                "合同期限：${contractPeriod}\n" +
                "申请时间：${applyTime}\n" +
                "当前状态：待审批\n\n" +
                "操作链接：\n" +
                "同意：${approveUrl}\n" +
                "拒绝：${rejectUrl}\n" +
                "查看详情：${viewUrl}\n\n" +
                "备注：${remarks}\n\n" +
                "请您在收到此邮件后尽快处理，如有疑问请联系申请人或相关部门。\n\n" +
                "${companyName}合同管理系统\n" +
                "系统时间：${systemTime}";
        
        return new EmailTemplate()
                .setTemplateId(Templates.CONTRACT_APPROVAL)
                .setTemplateName("合同审批模板")
                .setSubject(subject)
                .setHtmlContent(htmlContent)
                .setTextContent(textContent)
                .setTemplateType("system")
                .setDescription("合同审批流程邮件模板，包含合同信息和审批操作");
    }
    
    /**
     * 恭喜中奖模板
     * 
     * @return 中奖通知邮件模板
     */
    public static EmailTemplate createLotteryWinnerTemplate() {
        String subject = "🎊 恭喜您中奖了！奖金${prizeAmount}元等您领取！";
        
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang='zh-CN'>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <title>恭喜中奖</title>\n" +
                "    <style>\n" +
                "        body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }\n" +
                "        .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 15px; box-shadow: 0 8px 25px rgba(0,0,0,0.15); overflow: hidden; }\n" +
                "        .header { background: linear-gradient(135deg, #ff6b6b 0%, #feca57 100%); color: white; padding: 40px; text-align: center; position: relative; }\n" +
                "        .header::before { content: '🎉'; position: absolute; top: 10px; left: 20px; font-size: 30px; animation: bounce 2s infinite; }\n" +
                "        .header::after { content: '🎊'; position: absolute; top: 10px; right: 20px; font-size: 30px; animation: bounce 2s infinite 0.5s; }\n" +
                "        @keyframes bounce { 0%, 20%, 50%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-10px); } 60% { transform: translateY(-5px); } }\n" +
                "        .header h1 { margin: 0; font-size: 32px; font-weight: bold; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }\n" +
                "        .content { padding: 40px; }\n" +
                "        .congratulations { font-size: 24px; color: #e74c3c; text-align: center; margin-bottom: 30px; font-weight: bold; }\n" +
                "        .prize-box { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); border: 3px solid #f39c12; padding: 25px; border-radius: 15px; margin: 25px 0; text-align: center; box-shadow: 0 4px 15px rgba(243,156,18,0.3); }\n" +
                "        .prize-amount { font-size: 48px; color: #e74c3c; font-weight: bold; text-shadow: 2px 2px 4px rgba(0,0,0,0.1); margin: 10px 0; }\n" +
                "        .prize-title { font-size: 20px; color: #d35400; font-weight: bold; }\n" +
                "        .info-section { background-color: #f8f9fa; border-left: 5px solid #3498db; padding: 20px; margin: 25px 0; border-radius: 8px; }\n" +
                "        .info-item { margin: 12px 0; display: flex; }\n" +
                "        .info-label { font-weight: bold; color: #2c3e50; min-width: 120px; }\n" +
                "        .info-value { color: #34495e; }\n" +
                "        .claim-section { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); border: 2px solid #1abc9c; padding: 25px; border-radius: 12px; margin: 25px 0; text-align: center; }\n" +
                "        .claim-btn { display: inline-block; background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 18px; box-shadow: 0 4px 15px rgba(231,76,60,0.4); transition: transform 0.3s; }\n" +
                "        .claim-btn:hover { transform: translateY(-2px); }\n" +
                "        .warning { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 8px; margin: 20px 0; }\n" +
                "        .warning h4 { color: #856404; margin-top: 0; }\n" +
                "        .footer { background-color: #2c3e50; color: white; padding: 25px; text-align: center; }\n" +
                "        .emoji { font-size: 28px; }\n" +
                "        .sparkle { color: #f39c12; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <h1><span class='emoji'>🏆</span> 恭喜中奖！</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                "            <div class='congratulations'>\n" +
                "                <span class='sparkle'>✨</span> 恭喜 ${winnerName}，您中奖了！<span class='sparkle'>✨</span>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class='prize-box'>\n" +
                "                <div class='prize-title'>${lotteryName}</div>\n" +
                "                <div class='prize-amount'>${prizeAmount}元</div>\n" +
                "                <div style='color: #e67e22; font-size: 16px; margin-top: 10px;'>\n" +
                "                    <span class='emoji'>💰</span> ${prizeName} <span class='emoji'>💰</span>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class='info-section'>\n" +
                "                <h3><span class='emoji'>📋</span> 中奖信息</h3>\n" +
                "                <div class='info-item'>\n" +
                "                    <div class='info-label'>中奖人：</div>\n" +
                "                    <div class='info-value'>${winnerName}</div>\n" +
                "                </div>\n" +
                "                <div class='info-item'>\n" +
                "                    <div class='info-label'>手机号码：</div>\n" +
                "                    <div class='info-value'>${phoneNumber}</div>\n" +
                "                </div>\n" +
                "                <div class='info-item'>\n" +
                "                    <div class='info-label'>中奖号码：</div>\n" +
                "                    <div class='info-value'>${winningNumber}</div>\n" +
                "                </div>\n" +
                "                <div class='info-item'>\n" +
                "                    <div class='info-label'>开奖时间：</div>\n" +
                "                    <div class='info-value'>${drawTime}</div>\n" +
                "                </div>\n" +
                "                <div class='info-item'>\n" +
                "                    <div class='info-label'>奖项等级：</div>\n" +
                "                    <div class='info-value'>${prizeLevel}</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class='claim-section'>\n" +
                "                <h3><span class='emoji'>🎁</span> 立即领奖</h3>\n" +
                "                <p>恭喜您获得 <strong>${prizeName}</strong>，奖金 <strong>${prizeAmount}元</strong>！</p>\n" +
                "                <a href='${claimUrl}' class='claim-btn'>\n" +
                "                    <span class='emoji'>🚀</span> 立即领取奖金\n" +
                "                </a>\n" +
                "                <p style='margin-top: 15px; font-size: 14px; color: #7f8c8d;'>\n" +
                "                    领奖截止时间：${claimDeadline}\n" +
                "                </p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class='warning'>\n" +
                "                <h4><span class='emoji'>⚠️</span> 重要提醒</h4>\n" +
                "                <ul>\n" +
                "                    <li>请在规定时间内完成领奖，逾期将视为自动放弃</li>\n" +
                "                    <li>领奖时请准备好有效身份证件</li>\n" +
                "                    <li>奖金将在审核通过后3-5个工作日内到账</li>\n" +
                "                    <li>如有疑问，请联系客服：${customerService}</li>\n" +
                "                </ul>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p style='text-align: center; font-size: 18px; color: #e74c3c; font-weight: bold;'>\n" +
                "                再次恭喜您！祝您好运连连！<span class='emoji'>🍀</span>\n" +
                "            </p>\n" +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p><strong>${organizerName}</strong></p>\n" +
                "            <p>此邮件由系统自动发送，请勿回复</p>\n" +
                "            <p>客服热线：${customerService} | 官网：${website}</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        String textContent = "🎊 恭喜您中奖了！\n\n" +
                "恭喜 ${winnerName}，您在${lotteryName}中获得了${prizeName}！\n\n" +
                "中奖信息：\n" +
                "中奖人：${winnerName}\n" +
                "手机号码：${phoneNumber}\n" +
                "中奖号码：${winningNumber}\n" +
                "开奖时间：${drawTime}\n" +
                "奖项等级：${prizeLevel}\n" +
                "奖金金额：${prizeAmount}元\n\n" +
                "领奖信息：\n" +
                "领奖链接：${claimUrl}\n" +
                "领奖截止时间：${claimDeadline}\n\n" +
                "重要提醒：\n" +
                "1. 请在规定时间内完成领奖，逾期将视为自动放弃\n" +
                "2. 领奖时请准备好有效身份证件\n" +
                "3. 奖金将在审核通过后3-5个工作日内到账\n" +
                "4. 如有疑问，请联系客服：${customerService}\n\n" +
                "再次恭喜您！祝您好运连连！\n\n" +
                "${organizerName}\n" +
                "客服热线：${customerService}\n" +
                "官网：${website}";
        
        return new EmailTemplate()
                .setTemplateId(Templates.LOTTERY_WINNER)
                .setTemplateName("恭喜中奖模板")
                .setSubject(subject)
                .setHtmlContent(htmlContent)
                .setTextContent(textContent)
                .setTemplateType("marketing")
                .setDescription("中奖通知邮件模板，包含中奖信息和领奖流程");
    }
    
    /**
     * 根据模板ID获取模板
     * 
     * @param templateId 模板ID
     * @return 邮件模板
     */
    public static EmailTemplate getTemplate(String templateId) {
        if (templateId == null) {
            return null;
        }
        
        switch (templateId) {
            case Templates.WELCOME_ONBOARD:
                return createWelcomeOnboardTemplate();
            case Templates.CONTRACT_APPROVAL:
                return createContractApprovalTemplate();
            case Templates.LOTTERY_WINNER:
                return createLotteryWinnerTemplate();
            default:
                return null;
        }
    }
    
    /**
     * 处理模板参数替换
     * 
     * @param template 模板内容
     * @param params 参数Map
     * @return 处理后的内容
     */
    public static String processTemplate(String template, Map<String, Object> params) {
        if (template == null || params == null) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    /**
     * 获取所有可用的模板列表
     * 
     * @return 模板列表
     */
    public static EmailTemplate[] getAllTemplates() {
        return new EmailTemplate[]{
                createWelcomeOnboardTemplate(),
                createContractApprovalTemplate(),
                createLotteryWinnerTemplate()
        };
    }
}