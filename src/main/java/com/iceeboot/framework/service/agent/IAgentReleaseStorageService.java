package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.model.agent.*;

import java.util.List;

/** 发布版本持久化边界。 */
public interface IAgentReleaseStorageService {
    int nextVersion(long id);

    void publish(PublishedAgent p, String user);

    PublishedAgent release(String id);

    List<ReleaseView> releases(long agent);
}
