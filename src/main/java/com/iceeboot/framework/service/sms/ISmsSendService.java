package com.iceeboot.framework.service.sms;

import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.framework.query.sms.SmsBalanceQuery;
import com.iceeboot.framework.dto.create.sms.SmsSendDTO;
import com.iceeboot.framework.query.sms.SmsStatusQuery;

public interface ISmsSendService {
    SMSResponse sendSms(SmsSendDTO dto);

    SMSResponse queryStatus(SmsStatusQuery dto);

    SMSResponse queryBalance(SmsBalanceQuery dto);
}
