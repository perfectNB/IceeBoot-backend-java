package com.iceeboot.framework.service.agent.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.IceeBootDO;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.model.agent.Capability;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.vo.agent.AgentResourceVO;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/** 资源 CRUD 的公共事务、类型校验和审计字段处理，具体服务只提供类型转换。 */
public abstract class AbstractAgentResourceService<
                M extends BaseMapper<T>,
                T extends IceeBootDO,
                D,
                U extends D,
                V extends AgentResourceVO>
        extends ServiceImpl<M, T> {
    private final IAgentConfigurationService configs;
    private final IAgentPersistenceService store;
    private final CapabilityKind kind;
    private final BiFunction<D, Long, Capability> requestConverter;
    private final Function<Capability, V> responseConverter;
    private final Function<U, Long> updateId;

    protected AbstractAgentResourceService(
            IAgentConfigurationService configs,
            IAgentPersistenceService store,
            CapabilityKind kind,
            BiFunction<D, Long, Capability> requestConverter,
            Function<Capability, V> responseConverter,
            Function<U, Long> updateId) {
        this.configs = configs;
        this.store = store;
        this.kind = kind;
        this.requestConverter = requestConverter;
        this.responseConverter = responseConverter;
        this.updateId = updateId;
    }

    public List<V> getResourceList() {
        List<Capability> values = configs.capabilities(kind);
        if (values.isEmpty()) return List.of();
        Map<Long, T> rows =
                listByIds(values.stream().map(Capability::id).toList()).stream()
                        .collect(Collectors.toMap(IceeBootDO::getId, Function.identity()));
        return values.stream().map(value -> response(value, rows.get(value.id()))).toList();
    }

    private Capability resource(long id) {
        Capability value = configs.capability(id);
        if (value.kind() != kind)
            throw new IceeBootException(ResponseMessage.Agent.RESOURCE_TYPE_MISMATCH);
        return value;
    }

    private V response(Capability value, T row) {
        if (row == null)
            throw new IceeBootException(ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND);
        V result = responseConverter.apply(value);
        result.setCreateTime(row.getCreateTime());
        result.setUpdateTime(row.getUpdateTime());
        return result;
    }

    public V getResourceById(long id) {
        return response(resource(id), getById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public V addResource(D dto) {
        Capability saved = configs.saveCapability(requestConverter.apply(dto, null));
        return response(saved, getById(saved.id()));
    }

    @Transactional(rollbackFor = Exception.class)
    public V updateResource(U dto) {
        Long id = updateId.apply(dto);
        resource(id);
        Capability saved = configs.saveCapability(requestConverter.apply(dto, id));
        return response(saved, getById(saved.id()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(long id) {
        resource(id);
        store.deleteCapability(id);
    }
}
