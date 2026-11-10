package com.iceeboot.config.task;

import com.iceeboot.framework.job.PaymentBusinessCompensationJob;
import com.iceeboot.framework.mapper.pay.PayRecordMapper;
import com.iceeboot.framework.service.pay.IPayRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentCompensationConfigTest {
    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PaymentCompensationConfig.class, PaymentBusinessCompensationJob.class)
            .withBean(PayRecordMapper.class, () -> mock(PayRecordMapper.class))
            .withBean(IPayRecordService.class, () -> mock(IPayRecordService.class));

    @Test void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(PaymentBusinessCompensationJob.class);
            assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
        });
    }
    @Test void enabledRegistersCompensationSchedule() {
        runner.withPropertyValues("iceeboot.pay.business-compensation.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(PaymentBusinessCompensationJob.class);
                    assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(1);
                });
    }
}
