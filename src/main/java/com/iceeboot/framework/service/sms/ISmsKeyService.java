package com.iceeboot.framework.service.sms;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.sms.SmsKeyDTO;
import com.iceeboot.framework.dto.update.sms.SmsKeyUpdateDTO;
import com.iceeboot.framework.entity.sms.SmsKeyDO;
import com.iceeboot.framework.query.sms.SmsKeyQuery;
import com.iceeboot.framework.vo.sms.SmsKeyVO;
import com.iceeboot.framework.vo.sms.SmsProviderVO;

import java.util.List;

/**
 * SMS密钥服务接口
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
public interface ISmsKeyService extends IService<SmsKeyDO> {

    /**
     * 分页查询SMS密钥列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SmsKeyVO> getSmsKeyPage(SmsKeyQuery query);

    /**
     * 根据ID获取SMS密钥详情
     * 
     * @param id 密钥ID
     * @return 密钥详情
     */
    SmsKeyVO getSmsKeyById(Long id);

    /**
     * 新增SMS密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean addSmsKey(SmsKeyDTO dto);

    /**
     * 更新SMS密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean updateSmsKey(SmsKeyUpdateDTO dto);

    /**
     * 删除SMS密钥
     * 
     * @param ids 密钥ID列表
     * @return 是否成功
     */
    Boolean deleteSmsKey(List<Long> ids);

    /**
     * 修改SMS密钥状态
     * 
     * @param id 密钥ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateSmsKeyStatus(Long id, String status);

    /**
     * 根据厂商获取激活的密钥列表
     * 
     * @param smsProvider SMS厂商
     * @return 密钥列表
     */
    List<SmsKeyVO> getActiveSmsKeysByProvider(String smsProvider);

    /**
     * 获取所有支持的SMS厂商列表
     * 
     * @return 厂商列表
     */
    List<String> getSupportedProviders();

    /**
     * 根据厂商获取可用的密钥（轮询获取）
     * 
     * @param smsProvider SMS厂商
     * @return 可用密钥
     */
    SmsKeyVO getAvailableKeyByProvider(String smsProvider);

    /**
     * 验证密钥是否有效
     * 
     * @param id 密钥ID
     * @return 是否有效
     */
    Boolean validateSmsKey(Long id);

    /**
     * 批量导入密钥
     * 
     * @param keys 密钥列表
     * @return 导入成功数量
     */
    Integer batchImportKeys(List<SmsKeyDTO> keys);

    /**
     * 获取密钥使用统计
     * 
     * @param smsProvider SMS厂商
     * @return 统计信息
     */
    java.util.Map<String, Object> getKeyUsageStats(String smsProvider);

    /**
     * 获取所有厂商的密钥列表（按厂商分组）
     * 
     * @return 厂商密钥列表
     */
    List<SmsProviderVO> getAllProviderKeys();
}