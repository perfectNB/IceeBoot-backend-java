/**
 * 支付配置服务接口
 *
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.pay.PayConfigDTO;
import com.iceeboot.framework.dto.update.pay.PayConfigUpdateDTO;
import com.iceeboot.framework.entity.pay.PayConfigDO;
import com.iceeboot.framework.query.pay.PayConfigQuery;
import com.iceeboot.framework.vo.pay.PayConfigVO;

import java.util.List;

/**
 * 支付配置服务接口
 *
 * @author CodeIcee
 * @date 2025-11-19
 */
public interface IPayConfigService extends IService<PayConfigDO> {

    /**
     * 分页查询支付配置
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<PayConfigVO> getPayConfigPage(PayConfigQuery query);


    /**
     * 新增支付配置
     *
     * @param configDTO 支付配置信息
     * @return 是否成功
     */
    boolean addPayConfig(PayConfigDTO configDTO);

    /**
     * 更新支付配置
     *
     * @param configDTO 支付配置信息
     * @return 是否成功
     */
    boolean updatePayConfig(PayConfigUpdateDTO configDTO);

    /**
     * 删除支付配置
     *
     * @param ids 配置ID列表
     * @return 是否成功
     */
    boolean deletePayConfigs(List<Long> ids);

    /**
     * 修改支付配置状态
     *
     * @param id 配置ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);
}

