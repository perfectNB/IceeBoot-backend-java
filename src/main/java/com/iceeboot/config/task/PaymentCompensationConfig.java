package com.iceeboot.config.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 仅在显式开启补偿后注册 Spring 定时任务，Quartz 审批任务保持原有配置。 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "iceeboot.pay.business-compensation", name = "enabled", havingValue = "true")
public class PaymentCompensationConfig {
}
