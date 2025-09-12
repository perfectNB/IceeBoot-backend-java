/**
 * 消息管理控制器
 *
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.zboot.framework.controller.message;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.message.MessageDTO;
import com.zboot.framework.dto.update.message.MessageUpdateDTO;
import com.zboot.framework.query.message.MessageQuery;
import com.zboot.framework.service.message.IMessageService;
import com.zboot.framework.vo.message.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息管理控制器
 *
 * @author CodeIcee
 * @date 2025-09-12
 */
@Tag(name = "消息管理", description = "消息增删改查、状态管理等接口")
@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final IMessageService messageService;

    @Operation(summary = "分页查询当前用户消息列表", description = "根据条件分页查询当前用户的消息列表")
    @GetMapping()
    @RequirePermission(dynamic = true)
    public R<PageResult<MessageVO>> getMessagePage(MessageQuery query) {
        PageResult<MessageVO> pageResult = messageService.getMessagePage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "新增消息", description = "创建新消息")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addMessage(@Validated @RequestBody MessageDTO messageDTO) {
        boolean success = messageService.addMessage(messageDTO);
        return success ? R.ok("消息创建成功") : R.fail("消息创建失败");
    }

    @Operation(summary = "更新消息", description = "更新消息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateMessage(@RequestBody List<Long> ids) {
        boolean success = messageService.updateMessage(ids);
        return success ? R.ok("消息更新成功") : R.fail("消息更新失败");
    }


    @Operation(summary = "删除消息", description = "批量删除消息")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteMessages(@RequestBody List<Long> ids) {
        boolean success = messageService.deleteMessages(ids);
        return success ? R.ok("消息删除成功") : R.fail("消息删除失败");
    }

    @Operation(summary = "获取消息统计信息", description = "获取当前用户的未读、已读、总消息数量")
    @GetMapping("/stats")
    @RequirePermission(dynamic = true)
    public R<Map<String, Long>> getMessageStats() {
        Map<String, Long> stats = messageService.getMessageStats();
        return R.ok(stats);
    }
}