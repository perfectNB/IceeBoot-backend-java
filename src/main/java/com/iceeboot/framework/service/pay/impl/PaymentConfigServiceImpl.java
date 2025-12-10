/**
 * 支付配置服务实现类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.pay.PaymentConfigDTO;
import com.iceeboot.framework.dto.update.pay.PaymentConfigUpdateDTO;
import com.iceeboot.framework.entity.pay.PaymentConfigDO;
import com.iceeboot.framework.mapper.pay.PaymentConfigMapper;
import com.iceeboot.framework.query.pay.PaymentConfigQuery;
import com.iceeboot.framework.service.pay.IPaymentConfigService;
import com.iceeboot.framework.vo.pay.PaymentConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 支付配置服务实现类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Service
@Slf4j
public class PaymentConfigServiceImpl extends ServiceImpl<PaymentConfigMapper, PaymentConfigDO> implements IPaymentConfigService {

    @Override
    public PageResult<PaymentConfigVO> getPaymentConfigPage(PaymentConfigQuery query) {
        log.info("分页查询支付配置，查询条件：{}", query);
        
        // 构建查询条件
        LambdaQueryWrapper<PaymentConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getConfigName()), PaymentConfigDO::getConfigName, query.getConfigName())
                .eq(StringUtils.hasText(query.getStatus()), PaymentConfigDO::getStatus, query.getStatus())
                .ge(query.getMinAmount() != null, PaymentConfigDO::getAmount, query.getMinAmount())
                .le(query.getMaxAmount() != null, PaymentConfigDO::getAmount, query.getMaxAmount())
                .orderByDesc(PaymentConfigDO::getCreateTime);

        // 分页查询
        IPage<PaymentConfigDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PaymentConfigDO> result = this.page(page, wrapper);

        // 转换为VO
        List<PaymentConfigVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public List<PaymentConfigVO> getAllActiveConfigs() {
        log.info("获取所有启用的支付配置");
        
        List<PaymentConfigDO> configs = this.list(
                new LambdaQueryWrapper<PaymentConfigDO>()
                        .eq(PaymentConfigDO::getStatus, SystemConstants.Status.ACTIVE)

        );

        return configs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentConfigVO getPaymentConfigById(Long id) {
        log.info("根据ID获取支付配置详情，ID：{}", id);
        
        PaymentConfigDO config = this.getById(id);
        if (config == null) {
            throw new IceeBootException("支付配置不存在");
        }

        return convertToVO(config);
    }

    @Override
    public boolean addPaymentConfig(PaymentConfigDTO configDTO) {
        log.info("新增支付配置：{}", configDTO);
        
        PaymentConfigDO config = new PaymentConfigDO();
        BeanUtils.copyProperties(configDTO, config);
        
        // 设置默认状态
        if (!StringUtils.hasText(config.getStatus())) {
            config.setStatus(SystemConstants.Status.ACTIVE);
        }

        return this.save(config);
    }

    @Override
    public boolean updatePaymentConfig(PaymentConfigUpdateDTO configDTO) {
        log.info("更新支付配置：{}", configDTO);
        
        // 检查配置是否存在
        PaymentConfigDO existingConfig = this.getById(configDTO.getId());
        if (existingConfig == null) {
            throw new IceeBootException("支付配置不存在");
        }

        PaymentConfigDO config = new PaymentConfigDO();
        BeanUtils.copyProperties(configDTO, config);

        return this.updateById(config);
    }

    @Override
    public boolean deletePaymentConfigs(List<Long> ids) {
        log.info("删除支付配置，IDs：{}", ids);
        
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        log.info("修改支付配置状态，ID：{}，状态：{}", id, status);
        
        PaymentConfigDO config = new PaymentConfigDO();
        config.setId(id);
        config.setStatus(status);
        
        return this.updateById(config);
    }

    /**
     * 转换为VO对象
     */
    private PaymentConfigVO convertToVO(PaymentConfigDO config) {
        PaymentConfigVO vo = new PaymentConfigVO();
        BeanUtils.copyProperties(config, vo);
        
        // 计算元单位的金额（分转元，保留2位小数）
        if (config.getAmount() != null) {
            BigDecimal amountYuan = new BigDecimal(config.getAmount())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            vo.setAmountYuan(amountYuan.toString());
        }
        
        return vo;
    }
}

