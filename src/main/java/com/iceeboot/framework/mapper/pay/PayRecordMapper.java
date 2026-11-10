package com.iceeboot.framework.mapper.pay;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 支付记录Mapper接口
 *
 * @author CodeIcee
 * @date 2025-11-19
 */
@Mapper
public interface PayRecordMapper extends BaseMapper<PayRecordDO> {


    /**
     * 根据订单号查询支付记录
     */
    PayRecordDO selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /** 使用当前读获取订单，避免并发回调读取旧快照。 */
    @Select("SELECT * FROM t_pay_record WHERE out_trade_no=#{outTradeNo} LIMIT 1 FOR UPDATE")
    PayRecordDO selectByOutTradeNoForUpdate(@Param("outTradeNo") String outTradeNo);

    /**
     * 仅当订单处于“支付中”时更新为“支付成功”。
     * 用于支付回调并发场景下的原子幂等控制。
     */
    int updateToSuccessIfPaying(@Param("outTradeNo") String outTradeNo,
                                @Param("tradeNo") String tradeNo,
                                @Param("payAmount") Integer payAmount,
                                @Param("successStatus") Integer successStatus,
                                @Param("expectedStatus") Integer expectedStatus);

    int claimBusinessProcessing(@Param("id") Long id,
                                @Param("processingStatus") String processingStatus,
                                @Param("pendingStatus") String pendingStatus,
                                @Param("failedStatus") String failedStatus);

    int updateBusinessSuccess(@Param("id") Long id,
                              @Param("successStatus") String successStatus);

    int updateBusinessFailed(@Param("id") Long id,
                             @Param("failedStatus") String failedStatus,
                             @Param("errorMessage") String errorMessage);

    List<PayRecordDO> selectBusinessCompensationRecords(@Param("payStatus") Integer payStatus,
                                                        @Param("failedStatus") String failedStatus,
                                                        @Param("maxRetryCount") Integer maxRetryCount,
                                                        @Param("limit") Integer limit);

    int resetStaleBusinessProcessing(@Param("processingStatus") String processingStatus,
                                     @Param("failedStatus") String failedStatus,
                                     @Param("staleBefore") LocalDateTime staleBefore);
}
