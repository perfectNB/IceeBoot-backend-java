package com.zboot.framework.job;

import cn.hutool.http.HttpUtil;
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
public class HeatTreatmentJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println(HttpUtil.get("http://serverless.znbiscool.icu/zboot/Api/serverless"));
    }
}