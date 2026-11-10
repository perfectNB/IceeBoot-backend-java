package com.iceeboot.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.system.*;
import com.iceeboot.framework.service.system.impl.SysMenuServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

class AgentMenuPermissionTest {
  @Test
  void preservesMenuPermissionsThroughCachedTreeConversion() {
    var cache = mock(IAuthCacheService.class);
    when(cache.getUserMenus(1L))
        .thenReturn(
            List.of(
                Map.of(
                    "id",
                    40L,
                    "permissions",
                    List.of("ai:agent:read"),
                    "children",
                    List.of(
                        Map.of(
                            "id",
                            41L,
                            "parentId",
                            40L,
                            "permissions",
                            List.of("ai:model:read"))))));
    var service =
        new SysMenuServiceImpl(cache, mock(ISysRoleMenuService.class), mock(RedisTemplate.class));
    try (var tokens = mockStatic(TokenUtils.class)) {
      tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
      var result = service.getUserMenu();
      assertThat(result.getFirst().getPermissions()).isEqualTo("ai:agent:read");
      assertThat(result.getFirst().getChildren().getFirst().getPermissions())
          .isEqualTo("ai:model:read");
    }
  }
}
