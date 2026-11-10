package com.iceeboot.config.jackson;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class FlexibleLocalDateTimeDeserializerTest {
    private com.fasterxml.jackson.databind.ObjectMapper mapper() {
        var builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().jsonCustomizer().customize(builder);
        return builder.build();
    }
    @Test void acceptsExistingAndIsoDateFormatsWithoutChangingOutput() throws Exception {
        var expected = LocalDateTime.of(2026,11,10,12,34,56);
        for(String value : new String[]{"2026-11-10 12:34:56", "2026-11-10T12:34:56"}) {
            assertThat(mapper().readValue("\"" + value + "\"", LocalDateTime.class)).isEqualTo(expected);
        }
        assertThat(mapper().writeValueAsString(expected)).isEqualTo("\"2026-11-10 12:34:56\"");
    }
    @Test void acceptsFractionalSecondsAndRejectsInvalidDates() throws Exception {
        assertThat(mapper().readValue("\"2026-11-10T12:34:56.123\"", LocalDateTime.class).getNano()).isEqualTo(123000000);
        assertThatThrownBy(() -> mapper().readValue("\"invalid\"", LocalDateTime.class)).isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
    }
}
