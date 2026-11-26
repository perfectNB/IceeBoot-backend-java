package com.iceeboot.framework.mapper.pay;

import org.apache.ibatis.annotations.*;
import com.iceeboot.framework.enums.pay.PaymentChannel;

/** 下单时冻结渠道及权益，旧订单继续走原有收银台。 */
@Mapper
public interface PayTermsMapper {
    @Insert("INSERT INTO t_pay_order_terms (record_id, channel, pay_code) VALUES (#{id}, #{channel}, #{code})")
    void save(@Param("id") long id, @Param("channel") PaymentChannel channel, @Param("code") String code);

    @Select("SELECT channel FROM t_pay_order_terms WHERE record_id=#{id}")
    PaymentChannel channel(long id);

    @Select("SELECT pay_code FROM t_pay_order_terms WHERE record_id=#{id}")
    String payCode(long id);

    @Select("SELECT COUNT(*) FROM t_pay_entitlement WHERE record_id=#{id}")
    int countGrants(long id);

    @Insert("""
            INSERT INTO t_pay_entitlement (record_id, user_id, pay_code)
            VALUES (#{id}, #{userId}, #{code})
            ON DUPLICATE KEY UPDATE record_id=VALUES(record_id)
            """)
    void grant(@Param("id") long id, @Param("userId") long userId, @Param("code") String code);
}
