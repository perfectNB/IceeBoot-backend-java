package com.iceeboot.common.service.pay.service.impl;

import com.iceeboot.common.service.pay.service.AbstractPayService;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import com.iceeboot.framework.mapper.pay.PayTermsMapper;
import lombok.RequiredArgsConstructor;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
public class MemberEntitlementService extends AbstractPayService {
    private final String strategyCode;
    private final String description;
    private final PayTermsMapper mapper;

    @Override
    protected void doExecute(Long userId, PayRecordDO order) {
        mapper.grant(order.getId(), userId, strategyCode);
    }
}
