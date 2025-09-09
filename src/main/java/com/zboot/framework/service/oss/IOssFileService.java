package com.zboot.framework.service.oss;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.create.oss.OssFileUploadDTO;
import com.zboot.framework.entity.oss.OssFileDO;
import com.zboot.framework.query.oss.OssFileQuery;
import com.zboot.framework.vo.oss.OssFileVO;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * OSS文件服务接口
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
public interface IOssFileService extends IService<OssFileDO> {

    /**
     * 分页查询OSS文件列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<OssFileVO> getFilePage(OssFileQuery query);

    /**
     * 根据ID获取OSS文件详情
     * 
     * @param id 文件ID
     * @return 文件详情
     */
    OssFileVO getFileById(Long id);

    /**
     * 上传文件
     * 
     * @param dto 上传信息
     * @return 文件信息
     */
    OssFileVO uploadFile(OssFileUploadDTO dto);

    /**
     * 下载文件
     * 
     * @param id 文件ID
     * @return 文件资源
     */
    Resource downloadFile(Long id);

    /**
     * 删除文件
     * 
     * @param ids 文件ID列表
     * @return 是否成功
     */
    Boolean deleteFile(List<Long> ids);

    /**
     * 根据桶名统计文件数量
     * 
     * @param bucketName 桶名
     * @return 文件数量
     */
    Long countFilesByBucketName(String bucketName);

    /**
     * 根据文件名和桶名查询文件
     * 
     * @param fileName 文件名
     * @param bucketName 桶名
     * @return 文件信息
     */
    OssFileDO getFileByNameAndBucket(String fileName, String bucketName);
}