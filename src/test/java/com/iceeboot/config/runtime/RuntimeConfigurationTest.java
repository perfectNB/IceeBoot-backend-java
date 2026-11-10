package com.iceeboot.config.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import java.time.Duration;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class RuntimeConfigurationTest {
    @Test void tomcatOverridesBindToTheActualSpringBootProperties() throws Exception {
        var env = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(env.getPropertySources()::addLast);
        env.getPropertySources().addFirst(new MapPropertySource("fixture", Map.of(
                "SERVER_TOMCAT_THREADS_MAX", "123", "SERVER_TOMCAT_THREADS_MIN_SPARE", "12")));
        var server = Binder.get(env).bind("server", Bindable.of(ServerProperties.class)).get();
        assertThat(server.getTomcat().getThreads().getMax()).isEqualTo(123);
        assertThat(server.getTomcat().getThreads().getMinSpare()).isEqualTo(12);
        var scheduling = Binder.get(env).bind("spring.task.scheduling", Bindable.of(TaskSchedulingProperties.class)).get();
        assertThat(scheduling.getPool().getSize()).isEqualTo(4);
        assertThat(scheduling.getThreadNamePrefix()).isEqualTo("IceeBoot-Scheduler-");
        assertThat(scheduling.getShutdown().isAwaitTermination()).isTrue();
        assertThat(scheduling.getShutdown().getAwaitTerminationPeriod()).isEqualTo(Duration.ofSeconds(30));
    }
}
