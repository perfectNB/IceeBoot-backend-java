package com.iceeboot.framework.service.email;

import com.iceeboot.common.service.email.dto.EmailResponse;
import com.iceeboot.framework.dto.create.email.EmailSendDTO;
import com.iceeboot.framework.query.email.EmailStatusQuery;

public interface IEmailSendService {
    EmailResponse sendEmail(EmailSendDTO dto);

    EmailResponse queryStatus(EmailStatusQuery dto);
}
