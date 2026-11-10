package com.iceeboot.framework.agent;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.util.*;

final class AgentControllerCatalog {
    private AgentControllerCatalog() {}

    static List<Class<?>> controllers() throws Exception {
        var reader = new CachingMetadataReaderFactory();
        var result = new ArrayList<Class<?>>();
        for (var resource :
                new PathMatchingResourcePatternResolver()
                        .getResources(
                                "classpath*:com/iceeboot/framework/controller/agent/*Controller.class"))
            result.add(
                    Class.forName(
                            reader.getMetadataReader(resource).getClassMetadata().getClassName()));
        return result;
    }
}
