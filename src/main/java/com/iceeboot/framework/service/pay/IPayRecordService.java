/**
 * 支付记录服务接口
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import com.iceeboot.framework.query.pay.PayRecordQuery;
import com.iceeboot.framework.vo.pay.PayRecordVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 支付记录服务接口
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
public interface IPayRecordService extends IService<PayRecordDO> {

    /**
     * 分页查询支付记录
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<PayRecordVO> getPayRecordPage(PayRecordQuery query);

    /**
     * 根据ID获取支付记录详情
     *
     * @param id 记录ID
     * @return 支付记录VO
     */
    PayRecordVO getPayRecordById(Long id);

    /**
     * 根据商户订单号获取支付记录
     *
     * @param outTradeNo 商户订单号
     * @return 支付记录
     */
    PayRecordDO getByOutTradeNo(String outTradeNo);

    /**
     * 创建支付二维码
     *
     * @param configId 支付配置ID
     * @return 支付二维码URL
     */
    String createPaymentQR(Long configId);

    /**
     * 处理支付回调
     *
     * @param request HTTP请求
     * @return 处理结果
     */
    String handlePaymentCallback(HttpServletRequest request);

    boolean retryPaymentBusiness(Long payRecordId);

    /**
     * 删除支付记录
     *
     * @param ids ID列表
     * @return 是否成功
     */
    boolean deletePayRecords(List<Long> ids);
}
