package com.iceeboot.framework.service.sms;

import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.framework.dto.create.sms.SmsBalanceQueryDTO;
import com.iceeboot.framework.dto.create.sms.SmsSendDTO;
import com.iceeboot.framework.dto.create.sms.SmsStatusQueryDTO;

public interface ISmsSendService {
    SMSResponse sendSms(SmsSendDTO dto);

    SMSResponse queryStatus(SmsStatusQueryDTO dto);

    SMSResponse queryBalance(SmsBalanceQueryDTO dto);
}
