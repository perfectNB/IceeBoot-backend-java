package com.iceeboot.framework.service.ai.impl;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.factory.LLMFactory;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.common.utils.tool.*;
import com.iceeboot.framework.dto.create.ai.AiChatMessageDTO;
import com.iceeboot.framework.dto.create.ai.AiChatSessionDTO;
import com.iceeboot.framework.dto.create.ai.StreamChatRequest;
import com.iceeboot.framework.entity.ai.AiChatMessageDO;
import com.iceeboot.framework.entity.ai.AiChatSessionDO;
import com.iceeboot.framework.entity.ai.AiLLMKeyDO;
import com.iceeboot.framework.mapper.ai.AiChatMessageMapper;
import com.iceeboot.framework.mapper.ai.AiChatSessionMapper;
import com.iceeboot.framework.query.ai.AiChatMessageQuery;
import com.iceeboot.framework.query.ai.AiChatSessionQuery;
import com.iceeboot.framework.service.ai.IAiChatService;
import com.iceeboot.framework.service.ai.IAiLLMKeyService;
import com.iceeboot.framework.vo.ai.AiChatMessageVO;
import com.iceeboot.framework.vo.ai.AiChatSessionVO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * AI对话服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSessionDO> implements IAiChatService {

    private final AiChatMessageMapper aiChatMessageMapper;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final IAiLLMKeyService aiLLMKeyService;
    private final LLMFactory llmFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JapanMoviePathTool japanMoviePathTool;
    private final JapanMovieActorTool japanMovieActorTool;
    private final RoleUserTool roleUserTool;
    private final UserRoleTool userRoleTool;
    private final UserMenuAndPermissionTool userMenuAndPermissionTool;


    private static final int CACHE_EXPIRE_MINUTES = 30;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private DashScopeApi dashScopeApi;


    @Override
    public IPage<AiChatSessionVO> getSessionPage(AiChatSessionQuery query) {
        Page<AiChatSessionDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AiChatSessionDO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(AiChatSessionDO::getTitle, query.getKeyword());
        }

        if (StringUtils.hasText(query.getModelName())) {
            wrapper.like(AiChatSessionDO::getModelName, query.getModelName());
        }

        if (query.getType()==null){
            wrapper.eq(AiChatSessionDO::getType, SystemConstants.chat.LLMCHAT);
        }else{
            wrapper.eq(AiChatSessionDO::getType, query.getType());
        }

        Long userId = TokenUtils.getCurrentUserId();
        wrapper.eq(AiChatSessionDO::getCreateBy, userId);

        wrapper.orderByDesc(AiChatSessionDO::getCreateTime);

        IPage<AiChatSessionDO> result = this.page(page, wrapper);

        // 转换为VO
        Page<AiChatSessionVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AiChatSessionVO> voList = result.getRecords().stream().map(this::convertSessionToVO).collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public AiChatSessionVO getSessionById(Long id) {
        AiChatSessionDO session = this.getById(id);
        if (session == null) {
            return null;
        }

        AiChatSessionVO vo = convertSessionToVO(session);

        // 获取会话消息列表
        List<AiChatMessageVO> messages = getSessionMessages(id);

        return vo;
    }

    @Override
    public Boolean addSession(AiChatSessionDTO dto) {
        AiChatSessionDO session = new AiChatSessionDO();
        BeanUtils.copyProperties(dto, session);
        if (session.getType() == null){
            session.setType(SystemConstants.chat.LLMCHAT);
        }
        return this.save(session);
    }

    @Override
    public Boolean updateSession(AiChatSessionDTO dto) {
        AiChatSessionDO session = new AiChatSessionDO();
        BeanUtils.copyProperties(dto, session);
        return this.updateById(session);
    }

    @Override
    public Boolean deleteSession(List<Long> ids) {
        // 删除会话时同时删除相关消息
        for (Long id : ids) {
            clearSessionMessages(id);
        }
        return this.removeByIds(ids);
    }

    @Override
    public List<AiChatSessionVO> getSessionsByUserId(Long userId) {
        QueryWrapper<AiChatSessionDO> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("update_time");

        List<AiChatSessionDO> sessions = this.list(wrapper);
        return sessions.stream().map(this::convertSessionToVO).collect(Collectors.toList());
    }


    @Override
    public List<AiChatMessageVO> getSessionMessages(Long sessionId) {
        QueryWrapper<AiChatMessageDO> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .orderByAsc("timestamp");

        List<AiChatMessageDO> messages = aiChatMessageMapper.selectList(wrapper);
        return messages.stream().map(this::convertMessageToVO).collect(Collectors.toList());
    }

    @Override
    public Boolean clearSessionMessages(Long sessionId) {
        QueryWrapper<AiChatMessageDO> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);

        return aiChatMessageMapper.delete(wrapper) >= 0;
    }

    @Override
    public PageResult<AiChatMessageVO> getMessage(AiChatMessageQuery query) {
        // 获取当前用户ID
        Long userId = TokenUtils.getCurrentUserId();

        // 构建查询条件
        LambdaQueryWrapper<AiChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessageDO::getSessionId, query.getSessionId())
                .eq(AiChatMessageDO::getCreateBy, userId);

        // 添加关键词搜索
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AiChatMessageDO::getContent, query.getKeyword());
        }

        // 排序
        wrapper.orderByDesc(AiChatMessageDO::getCreateTime);

        // 分页查询
        Page<AiChatMessageDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<AiChatMessageDO> result = aiChatMessageMapper.selectPage(page, wrapper);

        // 转换为VO列表
        List<AiChatMessageVO> voList = result.getRecords().stream()
                .map(this::convertMessageToVO)
                .collect(Collectors.toList());

        // 返回分页结果
        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }


    @Override
    public Flux<LLMResponse> streamChat(StreamChatRequest request) {
        return Flux.defer(() -> {
            try {
                // 1. 保存用户消息
                Long userId = TokenUtils.getCurrentUserId();
                saveUserMessage(request.getSessionId(), request.getContent(), userId);

                // 2. 获取会话信息（优先从缓存获取）
                AiChatSessionDO session = getSessionFromCache(request.getSessionId());
                if (session == null) {
                    log.error("会话不存在: {}", request.getSessionId());
                    return Flux.just(createErrorResponse("会话不存在"));
                }

                // 3. 获取密钥信息（优先从缓存获取）
                AiLLMKeyDO llmKey = getKeyFromCache(session.getKeyId());
                if (llmKey == null) {
                    log.error("密钥不存在: {}", session.getKeyId());
                    return Flux.just(createErrorResponse("密钥不存在"));
                }

                // 4. 获取LLM服务
                LLMService llmService;
                try {
                    llmService = llmFactory.getService(llmKey.getLlmProvider());
                } catch (Exception e) {
                    log.error("不支持的LLM厂商: {}", llmKey.getLlmProvider(), e);
                    // 保存错误响应
                    saveAssistantMessage(request.getSessionId(), "网络错误", userId);
                    return Flux.just(createErrorResponse("不支持的LLM厂商: " + llmKey.getLlmProvider()));
                }

                // 5. 构建LLM请求
                LLMRequest llmRequest = buildLLMRequest(request, session, llmKey);

                // 6. 用于收集完整响应内容
                AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());

                // 7. 调用流式接口
                return llmService.chatStream(llmRequest)
                        .timeout(Duration.ofSeconds(60))
                        .doOnNext(response -> {
                            log.debug("接收到流式响应: {}", response);
                            // 收集响应内容
                            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                var choice = response.getChoices().get(0);
                                if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                    fullContent.get().append(choice.getDelta().getContent());
                                }
                            }
                        })
                        .doOnComplete(() -> {
                            // 流式响应完成，保存完整的助手回复
                            String content = fullContent.get().toString();
                            if (!content.isEmpty()) {
                                saveAssistantMessage(request.getSessionId(), content, userId);
                                log.info("保存助手回复消息，长度: {}", content.length());
                            }
                        })
                        .doOnError(error -> {
                            log.error("流式对话发生错误: {}", error.getMessage(), error);
                            // 保存错误信息
                            saveAssistantMessage(request.getSessionId(), "网络错误", userId);
                        })
                        .onErrorReturn(createErrorResponse("流式对话调用失败"));

            } catch (Exception e) {
                log.error("流式对话处理异常: {}", e.getMessage(), e);
                // 保存系统异常信息
                try {
                    Long userId = TokenUtils.getCurrentUserId();
                    saveAssistantMessage(request.getSessionId(), "网络错误", userId);
                } catch (Exception ex) {
                    log.error("保存异常消息失败: {}", ex.getMessage());
                }
                return Flux.just(createErrorResponse("系统异常: " + e.getMessage()));
            }
        });
    }

    @Override
    public Flux<String> toolStreamChat(StreamChatRequest request) {
        // 保存用户消息
        AiChatMessageDTO userMessageDto = new AiChatMessageDTO();
        userMessageDto.setSessionId(request.getSessionId());
        userMessageDto.setRole("user");
        userMessageDto.setContent(request.getContent());
        AiChatMessageDO message = new AiChatMessageDO();
        BeanUtils.copyProperties(userMessageDto, message);
        Long userId = TokenUtils.getCurrentUserId();
        aiChatMessageMapper.insert(message);


        // 用于收集完整响应内容
        StringBuilder fullContent = new StringBuilder();
        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                .build();

        return qwenChatClient.prompt()
                .user(request.getContent())
                .tools(japanMoviePathTool,
                        japanMovieActorTool,
                        roleUserTool,
                        userRoleTool,
                        userMenuAndPermissionTool
                )
                .advisors(advisor)
                .stream()
                .content()
                .doOnNext(content -> {
                    // 收集响应内容
                    if (content != null) {
                        fullContent.append(content);
                    }
                })
                .doOnComplete(() -> {
                    // 流式响应完成，保存完整的助手回复
                    String content = fullContent.toString();
                    if (!content.trim().isEmpty()) {
                        AiChatMessageDTO aiMessageDto = new AiChatMessageDTO();

                        aiMessageDto.setSessionId(request.getSessionId());
                        log.info("保存助手回复消息，长度: {}", content.length());
                        aiMessageDto.setRole("assistant");
                        aiMessageDto.setContent(content);
                        aiMessageDto.setUserId(userId);
                        convertMessage(aiMessageDto);
                    }
                })
                .doOnError(error -> {
                    // 保存错误信息
                    AiChatMessageDTO errorMessageDto = new AiChatMessageDTO();
                    errorMessageDto.setSessionId(request.getSessionId());
                    errorMessageDto.setRole("assistant");
                    errorMessageDto.setContent("网络错误");
                    convertMessage(errorMessageDto);
                });
    }

    @Override
    public Flux<String> ragStreamChat(StreamChatRequest request) {
        // 保存用户消息
        AiChatMessageDTO userMessageDto = new AiChatMessageDTO();
        userMessageDto.setSessionId(request.getSessionId());
        userMessageDto.setRole("user");
        userMessageDto.setContent(request.getContent());
        AiChatMessageDO message = new AiChatMessageDO();
        BeanUtils.copyProperties(userMessageDto, message);
        Long userId = TokenUtils.getCurrentUserId();
        aiChatMessageMapper.insert(message);

        // 用于收集完整响应内容
        StringBuilder fullContent = new StringBuilder();

        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName("剧本杀天眼") // 知识库名称
                        .build()
        );

        return qwenChatClient.prompt()
                .user(request.getContent())
                .system("你是一个剧本杀的DM，我会询问你有关剧本杀里的内容，你必须详细阅读知识库，最后根据知识库里的内容来回答，不要出现类似“则需要玩家通过游戏中的线索和互动来进一步推理和发现。”的话，如果没有对应消息，则回复，没有查询到对应的消息😠")
                .advisors(new DocumentRetrievalAdvisor(retriever))
                .stream()
                .content()
                .doOnNext(content -> {
                    // 收集响应内容
                    if (content != null) {
                        fullContent.append(content);
                    }
                })
                .doOnComplete(() -> {
                    // 流式响应完成，保存完整的助手回复
                    String content = fullContent.toString();
                    if (!content.trim().isEmpty()) {
                        AiChatMessageDTO aiMessageDto = new AiChatMessageDTO();
                        aiMessageDto.setSessionId(request.getSessionId());
                        log.info("保存助手回复消息，长度: {}", content.length());
                        aiMessageDto.setRole("assistant");
                        aiMessageDto.setContent(content);
                        aiMessageDto.setUserId(userId);
                        convertMessage(aiMessageDto);
                    }
                })
                .doOnError(error -> {
                    // 保存错误信息
                    AiChatMessageDTO errorMessageDto = new AiChatMessageDTO();
                    errorMessageDto.setSessionId(request.getSessionId());
                    errorMessageDto.setRole("assistant");
                    errorMessageDto.setContent("网络错误");
                    convertMessage(errorMessageDto);
                });
    }

    /**
     * 从缓存获取会话信息
     */
    public AiChatSessionDO getSessionFromCache(Long sessionId) {
        String cacheKey = CacheConstants.Auth.SESSION_CACHE_KEY + sessionId;

        // 先从缓存获取
        AiChatSessionDO session = (AiChatSessionDO) redisTemplate.opsForValue().get(cacheKey);
        if (session != null) {
            log.debug("从缓存获取会话信息: {}", sessionId);
            return session;
        }

        // 缓存未命中，从数据库获取
        session = aiChatSessionMapper.selectById(sessionId);
        if (session != null) {
            // 存入缓存
            redisTemplate.opsForValue().set(cacheKey, session, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("从数据库获取会话信息并缓存: {}", sessionId);
        }

        return session;
    }

    /**
     * 从缓存获取密钥信息
     */
    public AiLLMKeyDO getKeyFromCache(Long keyId) {
        String cacheKey = CacheConstants.Auth.KEY_CACHE_KEY + keyId;

        // 先从缓存获取
        AiLLMKeyDO llmKey = (AiLLMKeyDO) redisTemplate.opsForValue().get(cacheKey);
        if (llmKey != null) {
            log.debug("从缓存获取密钥信息: {}", keyId);
            return llmKey;
        }

        // 缓存未命中，从数据库获取
        llmKey = aiLLMKeyService.getById(keyId);
        if (llmKey != null) {
            // 存入缓存
            redisTemplate.opsForValue().set(cacheKey, llmKey, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("从数据库获取密钥信息并缓存: {}", keyId);
        }

        return llmKey;
    }

    /**
     * 构建LLM请求
     */
    public LLMRequest buildLLMRequest(StreamChatRequest request, AiChatSessionDO session, AiLLMKeyDO llmKey) {
        String baseUrl = StringUtils.hasText(llmKey.getUrl()) ? llmKey.getUrl().trim() : null;
        return new LLMRequest()
                .setApiKey(llmKey.getApiKey())
                .setBaseUrl(baseUrl)
                .setSecretKey(llmKey.getSecretKey())
                .setModel(session.getModelName())
                .setUserMessage(request.getContent())
                .setMaxTokens(1024)
                .setTemperature(0.7)
                .setStream(true)
                .setTimeout(60);
    }

    /**
     * 保存用户消息
     */
    public void saveUserMessage(Long sessionId, String content, Long userId) {
        try {
            AiChatMessageDO userMessage = new AiChatMessageDO();
            userMessage.setSessionId(sessionId);
            userMessage.setRole("user");
            userMessage.setContent(content);
            aiChatMessageMapper.insert(userMessage);
            log.debug("保存用户消息成功，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存助手消息
     */
    public void saveAssistantMessage(Long sessionId, String content, Long userId) {
        try {
            AiChatMessageDO assistantMessage = new AiChatMessageDO();
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(content);
            assistantMessage.setCreateBy(String.valueOf(userId));
            assistantMessage.setUpdateBy(String.valueOf(userId));
            aiChatMessageMapper.insert(assistantMessage);
            log.debug("保存助手消息成功，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("保存助手消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建错误响应
     */
    public LLMResponse createErrorResponse(String errorMessage) {
        return new LLMResponse()
                .setError(errorMessage)
                .setFinished(true)
                .setProvider("system")
                .setResponseTime(System.currentTimeMillis());
    }

    /**
     * 转换会话为VO对象
     */
    private AiChatSessionVO convertSessionToVO(AiChatSessionDO session) {
        AiChatSessionVO vo = new AiChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    /**
     * 转换消息为VO对象
     */
    private AiChatMessageVO convertMessageToVO(AiChatMessageDO message) {
        AiChatMessageVO vo = new AiChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        // 设置时间戳，使用创建时间
        vo.setTimestamp(message.getCreateTime());
        return vo;
    }

    public AiChatMessageVO convertMessage(AiChatMessageDTO dto) {
        AiChatMessageDO message = new AiChatMessageDO();
        BeanUtils.copyProperties(dto, message);
        message.setCreateBy(dto.getUserId().toString());
        message.setUpdateBy(dto.getUserId().toString());
        aiChatMessageMapper.insert(message);
        return convertMessageToVO(message);
    }
}