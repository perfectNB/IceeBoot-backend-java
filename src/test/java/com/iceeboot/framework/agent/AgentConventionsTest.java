package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.controller.agent.*;
import com.iceeboot.framework.service.agent.impl.AgentPersistenceServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.*;
import java.util.*;

class AgentConventionsTest {
    @Test
    void allControllerContractsUseRequestDtosResponseVosAndDynamicPermission() throws Exception {
        int endpoints = 0;
        for (Class<?> type : AgentControllerCatalog.controllers())
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(GetMapping.class)
                        && !method.isAnnotationPresent(PostMapping.class)
                        && !method.isAnnotationPresent(PutMapping.class)
                        && !method.isAnnotationPresent(DeleteMapping.class)) continue;
                endpoints++;
                assertThat(method.getReturnType()).isEqualTo(R.class);
                assertThat(method.getAnnotation(RequirePermission.class).dynamic()).isTrue();
                assertThat(method.getAnnotation(RequirePermission.class).code()).isEmpty();
                String signature = method.getGenericReturnType().getTypeName();
                assertThat(signature)
                        .doesNotContain("framework.entity", "framework.model", "io.agentscope");
                for (Parameter parameter : method.getParameters())
                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        assertThat(parameter.getType().getSimpleName()).endsWith("DTO");
                        assertThat(parameter.getType().getPackageName())
                                .startsWith("com.iceeboot.framework.dto.");
                    }
            }
        assertThat(endpoints).isEqualTo(53);
    }

    @Test
    void compatibilityFacadeCannotAccumulateMapperOrEntityDependencies() {
        for (Field field : AgentPersistenceServiceImpl.class.getDeclaredFields())
            assertThat(field.getType().getPackageName())
                    .doesNotStartWith("com.iceeboot.framework.mapper")
                    .doesNotStartWith("com.iceeboot.framework.entity");
        assertThat(
                        java.nio.file.Files.exists(
                                java.nio.file.Path.of("src/main/java/com/iceeboot/agent")))
                .isFalse();
    }
}
