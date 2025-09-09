package com.zboot.framework.service.oss;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.create.oss.OssBucketDTO;
import com.zboot.framework.dto.update.oss.OssBucketUpdateDTO;
import com.zboot.framework.entity.oss.OssBucketDO;
import com.zboot.framework.query.oss.OssBucketQuery;
import com.zboot.framework.vo.oss.OssBucketVO;

import java.util.List;

/**
 * OSS存储桶服务接口
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
public interface IOssBucketService extends IService<OssBucketDO> {

    /**
     * 分页查询OSS存储桶列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<OssBucketVO> getBucketPage(OssBucketQuery query);

    /**
     * 根据ID获取OSS存储桶详情
     * 
     * @param id 桶ID
     * @return 桶详情
     */
    OssBucketVO getBucketById(Long id);

    /**
     * 新增OSS存储桶
     * 
     * @param dto 桶信息
     * @return 是否成功
     */
    Boolean addBucket(OssBucketDTO dto);

    /**
     * 更新OSS存储桶
     * 
     * @param dto 桶信息
     * @return 是否成功
     */
    Boolean updateBucket(OssBucketUpdateDTO dto);

    /**
     * 删除OSS存储桶
     * 
     * @param ids 桶ID列表
     * @return 是否成功
     */
    Boolean deleteBucket(List<Long> ids);

    /**
     * 根据供应商和桶名查询桶
     * 
     * @param provider 供应商
     * @param bucketName 桶名
     * @return 桶信息
     */
    OssBucketDO getBucketByProviderAndName(String provider, String bucketName);

    /**
     * 检查桶是否存在文件
     * 
     * @param bucketName 桶名
     * @return 是否存在文件
     */
    Boolean hasBucketFiles(String bucketName);
}