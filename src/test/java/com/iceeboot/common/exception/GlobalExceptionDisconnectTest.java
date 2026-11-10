package com.iceeboot.common.exception;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import static org.assertj.core.api.Assertions.*;
class GlobalExceptionDisconnectTest {
    @Test void ordinaryHttpDisconnectIsNeverMislabelledAsSse() {
        var logger=(Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender=new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            var request=new MockHttpServletRequest("GET","/v1/system/example");
            new GlobalExceptionHandler().handleAsyncRequestNotUsableException(new AsyncRequestNotUsableException("Broken pipe"),request);
            assertThat(appender.list).hasSize(1);assertThat(appender.list.getFirst().getFormattedMessage()).contains("HTTP连接").doesNotContain("SSE连接");
            request.addHeader("Accept","text/event-stream");
            new GlobalExceptionHandler().handleAsyncRequestNotUsableException(new AsyncRequestNotUsableException("Connection reset by peer"),request);
            assertThat(appender.list.getLast().getFormattedMessage()).contains("SSE连接");
        } finally { logger.detachAppender(appender);appender.stop(); }
    }
}
