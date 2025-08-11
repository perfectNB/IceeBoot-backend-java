package com.zboot.framework.service.impl.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.framework.dto.ai.AiLLMDTO;
import com.zboot.framework.entity.ai.AiLLMDO;
import com.zboot.framework.mapper.ai.AiLLMMapper;
import com.zboot.framework.query.ai.AiLLMQuery;
import com.zboot.framework.service.ai.IAiLLMService;
import com.zboot.framework.vo.ai.AiLLMVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI大模型服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@RequiredArgsConstructor
public class AiLLMServiceImpl extends ServiceImpl<AiLLMMapper, AiLLMDO> implements IAiLLMService {

    @Override
    public IPage<AiLLMVO> getLLMPage(AiLLMQuery query) {
        Page<AiLLMDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AiLLMDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AiLLMDO::getName, query.getKeyword())
                   .or()
                   .like(AiLLMDO::getRemark, query.getKeyword());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AiLLMDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(AiLLMDO::getCreateTime);
        
        IPage<AiLLMDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<AiLLMVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AiLLMVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public AiLLMVO getLLMById(Long id) {
        AiLLMDO llm = this.getById(id);
        return llm != null ? convertToVO(llm) : null;
    }

    @Override
    public Boolean addLLM(AiLLMDTO dto) {
        AiLLMDO llm = new AiLLMDO();
        BeanUtils.copyProperties(dto, llm);
        return this.save(llm);
    }

    @Override
    public Boolean updateLLM(AiLLMDTO dto) {
        AiLLMDO llm = new AiLLMDO();
        BeanUtils.copyProperties(dto, llm);
        return this.updateById(llm);
    }

    @Override
    public Boolean deleteLLM(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Boolean updateLLMStatus(Long id, String status) {
        AiLLMDO llm = new AiLLMDO();
        llm.setId(id);
        llm.setStatus(status);
        return this.updateById(llm);
    }

    @Override
    public List<AiLLMVO> getActiveLLMList() {
        LambdaQueryWrapper<AiLLMDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiLLMDO::getStatus, "active")
               .orderByDesc(AiLLMDO::getCreateTime);
        
        List<AiLLMDO> llms = this.list(wrapper);
        return llms.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 转换为VO对象
     */
    private AiLLMVO convertToVO(AiLLMDO llm) {
        AiLLMVO vo = new AiLLMVO();
        BeanUtils.copyProperties(llm, vo);
        return vo;
    }
}