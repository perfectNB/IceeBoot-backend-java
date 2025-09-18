package com.iceeboot.common.service.oss.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * OSS容量信息DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class OSSCapacityInfo {
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 已使用容量（字节）
     */
    private Long usedCapacity;
    
    /**
     * 总容量（字节）
     */
    private Long totalCapacity;
    
    /**
     * 可用容量（字节）
     */
    private Long availableCapacity;
    
    /**
     * 对象数量
     */
    private Long objectCount;
    
    /**
     * 容量使用率（百分比）
     */
    private Double usagePercentage;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 统计时间
     */
    private LocalDateTime statisticsTime;
    
    /**
     * 计算容量使用率
     */
    public void calculateUsagePercentage() {
        if (totalCapacity != null && totalCapacity > 0 && usedCapacity != null) {
            this.usagePercentage = (double) usedCapacity / totalCapacity * 100;
        }
    }
    
    /**
     * 计算可用容量
     */
    public void calculateAvailableCapacity() {
        if (totalCapacity != null && usedCapacity != null) {
            this.availableCapacity = totalCapacity - usedCapacity;
        }
    }
    
    /**
     * 格式化容量大小
     */
    public static String formatCapacity(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * 获取格式化的已使用容量
     */
    public String getFormattedUsedCapacity() {
        return formatCapacity(usedCapacity);
    }
    
    /**
     * 获取格式化的总容量
     */
    public String getFormattedTotalCapacity() {
        return formatCapacity(totalCapacity);
    }
    
    /**
     * 获取格式化的可用容量
     */
    public String getFormattedAvailableCapacity() {
        return formatCapacity(availableCapacity);
    }
}