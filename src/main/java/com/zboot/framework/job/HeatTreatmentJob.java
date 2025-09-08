package com.zboot.framework.job;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * 热处理定时任务
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Component
@Slf4j
public class HeatTreatmentJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("迎面走来的你让我如此蠢蠢欲动，这种感觉我曾未有");
    }
}