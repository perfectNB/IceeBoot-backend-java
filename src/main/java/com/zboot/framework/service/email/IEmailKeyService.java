package com.zboot.framework.service.email;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.create.email.EmailKeyDTO;
import com.zboot.framework.dto.update.email.EmailKeyUpdateDTO;
import com.zboot.framework.entity.email.EmailKeyDO;
import com.zboot.framework.query.email.EmailKeyQuery;
import com.zboot.framework.vo.email.EmailKeyVO;
import com.zboot.framework.vo.email.EmailProviderVO;

import java.util.List;

/**
 * Email密钥服务接口
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
public interface IEmailKeyService extends IService<EmailKeyDO> {

    /**
     * 分页查询Email密钥列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<EmailKeyVO> getEmailKeyPage(EmailKeyQuery query);

    /**
     * 根据ID获取Email密钥详情
     * 
     * @param id 密钥ID
     * @return 密钥详情
     */
    EmailKeyVO getEmailKeyById(Long id);

    /**
     * 新增Email密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean addEmailKey(EmailKeyDTO dto);

    /**
     * 更新Email密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean updateEmailKey(EmailKeyUpdateDTO dto);

    /**
     * 删除Email密钥
     * 
     * @param ids 密钥ID列表
     * @return 是否成功
     */
    Boolean deleteEmailKey(List<Long> ids);

    /**
     * 修改Email密钥状态
     * 
     * @param id 密钥ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateEmailKeyStatus(Long id, String status);

    /**
     * 根据厂商获取激活的密钥列表
     * 
     * @param emailProvider Email厂商
     * @return 密钥列表
     */
    List<EmailKeyVO> getActiveEmailKeysByProvider(String emailProvider);

    /**
     * 获取所有支持的Email厂商列表
     * 
     * @return 厂商列表
     */
    List<String> getSupportedProviders();

    /**
     * 根据厂商获取可用的密钥（轮询获取）
     * 
     * @param emailProvider Email厂商
     * @return 可用密钥
     */
    EmailKeyVO getAvailableKeyByProvider(String emailProvider);

    /**
     * 验证密钥是否有效
     * 
     * @param id 密钥ID
     * @return 是否有效
     */
    Boolean validateEmailKey(Long id);

    /**
     * 批量导入密钥
     * 
     * @param keys 密钥列表
     * @return 导入成功数量
     */
    Integer batchImportKeys(List<EmailKeyDTO> keys);

    /**
     * 获取密钥使用统计
     * 
     * @param emailProvider Email厂商
     * @return 统计信息
     */
    java.util.Map<String, Object> getKeyUsageStats(String emailProvider);

    /**
     * 获取所有厂商的密钥列表（按厂商分组）
     * 
     * @return 厂商密钥列表
     */
    List<EmailProviderVO> getAllProviderKeys();
}