package com.zboot.framework.service.oss;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.oss.OssKeyDTO;
import com.zboot.framework.entity.oss.OssKeyDO;
import com.zboot.framework.query.oss.OssKeyQuery;
import com.zboot.framework.vo.oss.OssKeyVO;
import com.zboot.framework.vo.oss.OssProviderVO;

import java.util.List;

/**
 * OSS密钥服务接口
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
public interface IOssKeyService extends IService<OssKeyDO> {

    /**
     * 分页查询OSS密钥列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<OssKeyVO> getOssKeyPage(OssKeyQuery query);

    /**
     * 根据ID获取OSS密钥详情
     * 
     * @param id 密钥ID
     * @return 密钥详情
     */
    OssKeyVO getOssKeyById(Long id);

    /**
     * 新增OSS密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean addOssKey(OssKeyDTO dto);

    /**
     * 更新OSS密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean updateOssKey(OssKeyDTO dto);

    /**
     * 删除OSS密钥
     * 
     * @param ids 密钥ID列表
     * @return 是否成功
     */
    Boolean deleteOssKey(List<Long> ids);

    /**
     * 修改OSS密钥状态
     * 
     * @param id 密钥ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateOssKeyStatus(Long id, String status);

    /**
     * 根据厂商获取激活的密钥列表
     * 
     * @param ossProvider OSS厂商
     * @return 密钥列表
     */
    List<OssKeyVO> getActiveOssKeysByProvider(String ossProvider);

    /**
     * 获取所有支持的OSS厂商列表
     * 
     * @return 厂商列表
     */
    List<String> getSupportedProviders();

    /**
     * 根据厂商获取可用的密钥（轮询获取）
     * 
     * @param ossProvider OSS厂商
     * @return 可用密钥
     */
    OssKeyVO getAvailableKeyByProvider(String ossProvider);

    /**
     * 验证密钥是否有效
     * 
     * @param id 密钥ID
     * @return 是否有效
     */
    Boolean validateOssKey(Long id);

    /**
     * 批量导入密钥
     * 
     * @param keys 密钥列表
     * @return 导入成功数量
     */
    Integer batchImportKeys(List<OssKeyDTO> keys);

    /**
     * 获取密钥使用统计
     * 
     * @param ossProvider OSS厂商
     * @return 统计信息
     */
    java.util.Map<String, Object> getKeyUsageStats(String ossProvider);

    /**
     * 获取所有厂商的密钥列表（按厂商分组）
     * 
     * @return 厂商密钥列表
     */
    List<OssProviderVO> getAllProviderKeys();
}