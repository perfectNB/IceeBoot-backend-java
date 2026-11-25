package com.iceeboot.framework.dto.update.agent;

import com.iceeboot.framework.model.agent.memory.MemoryData.FieldType;
import jakarta.validation.constraints.*;
import java.util.*;

public final class MemoryRequest {
    private MemoryRequest() {}

    public record PolicyDTO(@PositiveOrZero long revision, boolean enabled, boolean reviewRequired,
                         @NotBlank @Size(max = 4000) String prompt,
                         @NotEmpty @Size(max = 32) Map<@Pattern(regexp = "[a-z][a-z0-9_]{0,47}") String, @NotNull FieldType> fields) {}
    public record FieldDTO(@PositiveOrZero long revision, @NotBlank @Size(max = 240) String value) {}
    public record GrantDTO(@PositiveOrZero long revision, @NotNull @Size(max = 32) Set<String> readFields,
                        @NotNull @Size(max = 32) Set<String> writeFields) {}
    public record ReviewDTO(boolean approve) {}
}
