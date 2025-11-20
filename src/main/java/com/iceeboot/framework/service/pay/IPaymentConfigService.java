/**
 * 支付配置服务接口
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.pay.PaymentConfigDTO;
import com.iceeboot.framework.dto.update.pay.PaymentConfigUpdateDTO;
import com.iceeboot.framework.entity.pay.PaymentConfigDO;
import com.iceeboot.framework.query.pay.PaymentConfigQuery;
import com.iceeboot.framework.vo.pay.PaymentConfigVO;

import java.util.List;

/**
 * 支付配置服务接口
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
public interface IPaymentConfigService extends IService<PaymentConfigDO> {

    /**
     * 分页查询支付配置
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<PaymentConfigVO> getPaymentConfigPage(PaymentConfigQuery query);

    /**
     * 获取所有启用的支付配置
     *
     * @return 支付配置列表
     */
    List<PaymentConfigVO> getAllActiveConfigs();

    /**
     * 根据ID获取支付配置详情
     *
     * @param id 配置ID
     * @return 支付配置VO
     */
    PaymentConfigVO getPaymentConfigById(Long id);

    /**
     * 新增支付配置
     *
     * @param configDTO 支付配置信息
     * @return 是否成功
     */
    boolean addPaymentConfig(PaymentConfigDTO configDTO);

    /**
     * 更新支付配置
     *
     * @param configDTO 支付配置信息
     * @return 是否成功
     */
    boolean updatePaymentConfig(PaymentConfigUpdateDTO configDTO);

    /**
     * 删除支付配置
     *
     * @param ids 配置ID列表
     * @return 是否成功
     */
    boolean deletePaymentConfigs(List<Long> ids);

    /**
     * 修改支付配置状态
     *
     * @param id 配置ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);
}

