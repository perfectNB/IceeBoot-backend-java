package com.iceeboot.framework.job;

import com.iceeboot.common.constant.PaymentBusinessStatuses;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import com.iceeboot.framework.mapper.pay.PayRecordMapper;
import com.iceeboot.framework.service.pay.IPayRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "iceeboot.pay.business-compensation", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentBusinessCompensationJob {

    private static final int PAY_STATUS_SUCCESS = 2;

    private final PayRecordMapper payRecordMapper;
    private final IPayRecordService payRecordService;

    @Value("${iceeboot.pay.business-compensation.max-retry-count:8}")
    private Integer maxRetryCount;

    @Value("${iceeboot.pay.business-compensation.batch-size:20}")
    private Integer batchSize;

    @Scheduled(fixedDelayString = "${iceeboot.pay.business-compensation.fixed-delay-millis:60000}")
    public void compensate() {
        payRecordMapper.resetStaleBusinessProcessing(
                PaymentBusinessStatuses.PROCESSING,
                PaymentBusinessStatuses.FAILED,
                LocalDateTime.now().minusMinutes(10));

        List<PayRecordDO> records = payRecordMapper.selectBusinessCompensationRecords(
                PAY_STATUS_SUCCESS,
                PaymentBusinessStatuses.FAILED,
                maxRetryCount,
                batchSize);
        for (PayRecordDO record : records) {
            try {
                payRecordService.retryPaymentBusiness(record.getId());
            } catch (Exception e) {
                log.error("支付业务补偿执行异常，payRecordId={}", record.getId(), e);
            }
        }
    }
}
