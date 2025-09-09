package com.zboot.framework.mapper.oss;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.oss.OssBucketDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * OSS存储桶Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Mapper
public interface OssBucketMapper extends BaseMapper<OssBucketDO> {

    /**
     * 根据供应商和桶名查询桶
     * 
     * @param provider 供应商
     * @param bucketName 桶名
     * @return 桶信息
     */
    OssBucketDO selectByProviderAndBucketName(@Param("provider") String provider, @Param("bucketName") String bucketName);

    /**
     * 统计桶下的文件数量
     * 
     * @param bucketName 桶名
     * @return 文件数量
     */
    Long countFilesByBucketName(@Param("bucketName") String bucketName);
}