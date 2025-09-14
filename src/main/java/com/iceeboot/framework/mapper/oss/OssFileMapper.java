package com.iceeboot.framework.mapper.oss;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.oss.OssFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * OSS文件Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Mapper
public interface OssFileMapper extends BaseMapper<OssFileDO> {

    /**
     * 根据桶名统计文件数量
     * 
     * @param bucketName 桶名
     * @return 文件数量
     */
    Long countByBucketName(@Param("bucketName") String bucketName);

    /**
     * 根据文件名和桶名查询文件
     * 
     * @param fileName 文件名
     * @param bucketName 桶名
     * @return 文件信息
     */
    OssFileDO selectByFileNameAndBucketName(@Param("fileName") String fileName, @Param("bucketName") String bucketName);

    /**
     * 根据MD5值查询文件
     * 
     * @param fileMd5 文件MD5值
     * @return 文件信息
     */
    OssFileDO selectByFileMd5(@Param("fileMd5") String fileMd5);
}