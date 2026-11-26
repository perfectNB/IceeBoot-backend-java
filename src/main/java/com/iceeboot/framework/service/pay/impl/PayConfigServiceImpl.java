/**
 * 支付配置服务实现类
 *
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.pay.factory.PayFactory;
import org.springframework.transaction.annotation.Transactional;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.pay.PayConfigDTO;
import com.iceeboot.framework.dto.update.pay.PayConfigUpdateDTO;
import com.iceeboot.framework.entity.pay.PayConfigDO;
import com.iceeboot.framework.mapper.pay.PayConfigMapper;
import com.iceeboot.framework.query.pay.PayConfigQuery;
import com.iceeboot.framework.service.pay.IPayConfigService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.pay.PayConfigVO;
import com.iceeboot.framework.vo.system.SysUserVO;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PayConfigServiceImpl extends ServiceImpl<PayConfigMapper, PayConfigDO> implements IPayConfigService {


    private final PayFactory payFactory;
    private final PayConfigMapper payConfigMapper;
    private final ISysUserService sysUserService;
    @Override
    public PageResult<PayConfigVO> getPayConfigPage(PayConfigQuery query) {
        log.info("分页查询支付配置，查询条件：{}", query);

        // 构建查询条件
        LambdaQueryWrapper<PayConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getConfigName()), PayConfigDO::getConfigName, query.getConfigName())
                .like(StringUtils.hasText(query.getPayCode()), PayConfigDO::getPayCode, query.getPayCode())
                .eq(StringUtils.hasText(query.getStatus()), PayConfigDO::getStatus, query.getStatus())
                .eq(PayConfigDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                .ge(query.getMinAmount() != null, PayConfigDO::getAmount, query.getMinAmount())
                .le(query.getMaxAmount() != null, PayConfigDO::getAmount, query.getMaxAmount())
                .orderByDesc(PayConfigDO::getCreateTime);

        // 分页查询
        IPage<PayConfigDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PayConfigDO> result = this.page(page, wrapper);

        // 转换为VO
        List<PayConfigVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }


    @Override
    public boolean addPayConfig(PayConfigDTO configDTO) {
        log.info("新增支付配置：{}", configDTO);
        payFactory.getStrategy(configDTO.getPayCode());

        PayConfigDO config = new PayConfigDO();
        BeanUtils.copyProperties(configDTO, config);

        return this.save(config);
    }

    @Override
    public boolean updatePayConfig(PayConfigUpdateDTO configDTO) {
        log.info("更新支付配置：{}", configDTO);
        if (configDTO.getPayCode() != null) {
            payFactory.getStrategy(configDTO.getPayCode());
        }

        // 检查配置是否存在
        PayConfigDO existingConfig = this.getById(configDTO.getId());
        if (existingConfig == null) {
            throw new IceeBootException("支付配置不存在");
        }

        PayConfigDO config = new PayConfigDO();
        BeanUtils.copyProperties(configDTO, config);

        return this.updateById(config);
    }

    @Override
    @Transactional
    public boolean deletePayConfigs(List<Long> ids) {
        log.info("删除支付配置，IDs：{}", ids);
        for (Long id : ids) {
            PayConfigDO existingConfig = this.getById(id);
            if (existingConfig == null) {
                throw new IceeBootException("支付配置不存在");
            }
            if (SystemConstants.Status.ACTIVE.equals(existingConfig.getStatus())) {
                throw new IceeBootException("支付配置正在使用中，无法删除");
            }
            existingConfig.setDelFlag(SystemConstants.DeleteFlag.DELETED);
            payConfigMapper.updateById(existingConfig);
        }
        return true;
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        log.info("修改支付配置状态，ID：{}，状态：{}", id, status);

        PayConfigDO config = new PayConfigDO();
        config.setId(id);
        if (!"active".equals(status) && !"inactive".equals(status)) throw new IceeBootException("无效的支付配置状态");
        config.setStatus(status);

        return this.updateById(config);
    }

    /**
     * 转换为VO对象
     */
    private PayConfigVO convertToVO(PayConfigDO config) {
        PayConfigVO vo = new PayConfigVO();
        BeanUtils.copyProperties(config, vo);

        // 计算元单位的金额（分转元，保留2位小数）
        if (config.getAmount() != null) {
            BigDecimal amountYuan = new BigDecimal(config.getAmount())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            vo.setAmountYuan(amountYuan.toString());
        }

        if (StrUtil.isNotBlank(config.getCreateBy())) {
            try {
                Long userId = Long.valueOf(config.getCreateBy());
                SysUserVO user = sysUserService.getUserById(userId);
                if (user != null) {
                    vo.setCreateBy(user.getUsername());
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", config.getCreateBy());
            }
        }
        return vo;
    }
}

