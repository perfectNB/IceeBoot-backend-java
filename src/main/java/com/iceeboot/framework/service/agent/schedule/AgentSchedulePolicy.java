package com.iceeboot.framework.service.agent.schedule;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.AgentScheduleDO;
import com.iceeboot.framework.enums.agent.AgentScheduleType;
import org.quartz.CronExpression;
import org.springframework.stereotype.Component;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

/** 只负责时间规则；周期任务按分钟触发，错过的周期不批量补跑。 */
@Component
public class AgentSchedulePolicy {
    public Long next(AgentScheduleDO row, long now, boolean validateFuture) {
        try {
            ZoneId zone = ZoneId.of(row.getZoneId());
            if (row.getScheduleType() == AgentScheduleType.ONCE) {
                if (row.getExecuteAt() == null || (validateFuture && row.getExecuteAt() <= now))
                    throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_TIME_INVALID);
                return row.getExecuteAt();
            }
            if (row.getScheduleType() != AgentScheduleType.CRON || row.getCronExpression() == null
                    || !row.getCronExpression().trim().matches("0\\s+.+"))
                throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_CRON_INVALID);
            CronExpression cron = new CronExpression(row.getCronExpression().trim());
            cron.setTimeZone(TimeZone.getTimeZone(zone));
            Date next = cron.getNextValidTimeAfter(new Date(now));
            if (next == null && validateFuture)
                throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_TIME_INVALID);
            return next == null ? null : next.getTime();
        } catch (IceeBootException error) {
            throw error;
        } catch (Exception error) {
            throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_CRON_INVALID);
        }
    }

    public Long afterFire(AgentScheduleDO row, long now) {
        return row.getScheduleType() == AgentScheduleType.ONCE ? null : next(row, now, false);
    }
}
