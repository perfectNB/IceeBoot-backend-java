# ZBoot超级脚手架 - 项目包结构文档

## 项目概述

**项目名称**: ZBoot Backend Java  
**项目描述**: ZBoot超级脚手架后端项目  
**技术栈**: Spring Boot 3.1.5 + MyBatis Plus + Sa-Token + Redis + MySQL  
**架构模式**: 前后端分离架构  
**Java版本**: JDK 17  

## 项目根目录结构

```
zboot-backend-java/
├── src/main/java/com/zboot/          # Java源码根目录
├── src/main/resources/               # 资源文件目录
├── pom.xml                          # Maven配置文件
├── README.md                        # 项目说明文档
└── logs/                           # 日志文件目录
```

## 核心包结构详解

### 1. 根包 (com.zboot)

```
com.zboot/
├── ZbootApplication.java           # Spring Boot启动类
├── common/                         # 公共组件包
├── config/                         # 全局配置包
├── framework/                      # 业务框架包
├── generator/                      # 代码生成器包
└── project/                        # 项目模块包
```

**作用**: 项目的根包，包含启动类和各个功能模块

---

### 2. 公共组件包 (com.zboot.common)

#### 2.1 注解包 (common.annotation)
```
annotation/
└── RequirePermission.java          # 权限验证注解
```
**作用**: 自定义注解，用于权限控制、日志记录等横切关注点

#### 2.2 常量包 (common.constant)
```
constant/
├── CacheConstants.java             # 缓存常量类
└── ResponseMessage.java            # 响应消息常量
```
**作用**: 定义系统中使用的各种常量，包括缓存键、响应消息等

#### 2.3 核心领域包 (common.core)
```
core/
├── domain/
│   ├── BaseQuery.java              # 基础查询对象
│   ├── PageResult.java             # 分页结果对象
│   ├── R.java                      # 统一响应结果封装
│   └── ZbootDO.java               # 基础实体类
└── page/
    └── PageResult.java             # 分页结果类
```
**作用**: 核心领域对象，包括基础实体类、响应封装、分页对象等

#### 2.4 枚举包 (common.enums)
```
enums/
└── ResultCodeEnum.java             # 结果状态码枚举
```
**作用**: 定义系统中使用的枚举类型，如状态码、业务类型等

#### 2.5 异常包 (common.exception)
```
exception/
├── GlobalExceptionHandler.java     # 全局异常处理器
├── ServiceException.java           # 业务异常类
└── ZbootException.java             # 自定义异常类
```
**作用**: 异常处理机制，包括全局异常处理器和自定义异常类

#### 2.6 工具包 (common.utils)
```
utils/
├── DesensitizeUtils.java           # 数据脱敏工具
├── HttpUtils.java                  # HTTP请求工具
├── IpUtils.java                    # IP地址工具
├── Md5Utils.java                   # MD5加密工具
├── PasswordUtil.java               # 密码处理工具
├── RedisUtils.java                 # Redis操作工具
└── TokenUtils.java                 # Token处理工具
```
**作用**: 通用工具类，提供加密、网络、缓存等常用功能

---

### 3. 全局配置包 (com.zboot.config)

```
config/
├── CorsConfig.java                 # 跨域配置
├── Knife4jConfig.java              # API文档配置
├── MybatisPlusConfig.java          # MyBatis Plus配置
├── RedisConfig.java                # Redis配置
├── SaTokenConfig.java              # Sa-Token权限配置
├── SaTokenPermissionImpl.java      # Sa-Token权限实现
└── ThreadPoolConfig.java           # 线程池配置
```
**作用**: 系统全局配置类，包括数据库、缓存、权限、文档等配置

---

### 4. 业务框架包 (com.zboot.framework)

#### 4.1 框架配置包 (framework.config)
```
config/
├── ActivitiConfig.java             # 工作流引擎配置
└── QuartzConfig.java               # 定时任务配置
```
**作用**: 业务框架相关的配置，如工作流、定时任务等

#### 4.2 控制器包 (framework.controller)
```
controller/
├── ai/                             # AI模块控制器
│   ├── AiAgentController.java      # AI代理控制器
│   ├── AiChatController.java       # AI聊天控制器
│   └── AiLLMController.java        # 大语言模型控制器
├── dashboard/                      # 仪表盘模块
│   └── DashboardController.java    # 仪表盘控制器
├── log/                           # 日志模块
│   └── LogController.java          # 日志控制器
├── redis/                         # Redis模块
│   └── RedisController.java        # Redis控制器
├── system/                        # 系统管理模块
│   ├── AuthController.java         # 认证控制器
│   ├── PermissionTestController.java # 权限测试控制器
│   ├── SysMenuController.java      # 菜单管理控制器
│   ├── SysPermissionController.java # 权限管理控制器
│   ├── SysRoleController.java      # 角色管理控制器
│   ├── SysRoleMenuController.java  # 角色菜单关联控制器
│   ├── SysRolePermissionController.java # 角色权限关联控制器
│   ├── SysUserController.java      # 用户管理控制器
│   ├── SysUserRoleController.java  # 用户角色关联控制器
│   └── UrlPermissionController.java # URL权限管理控制器
├── task/                          # 任务模块
│   └── TaskController.java         # 任务控制器
└── workflow/                      # 工作流模块
    └── WorkflowController.java     # 工作流控制器
```
**作用**: MVC架构中的控制层，处理HTTP请求，调用业务逻辑，返回响应结果

#### 4.3 数据传输对象包 (framework.dto)
```
dto/
├── ai/                            # AI模块DTO
│   ├── AiAgentDTO.java            # AI代理数据传输对象
│   ├── AiChatMessageDTO.java      # AI聊天消息DTO
│   ├── AiChatSessionDTO.java      # AI聊天会话DTO
│   ├── AiLLMDTO.java              # 大语言模型DTO
│   └── AiLLMKeyDTO.java           # LLM密钥DTO
├── log/                           # 日志模块DTO
│   ├── LogBatchDeleteDTO.java     # 日志批量删除DTO
│   ├── LogClearDTO.java           # 日志清理DTO
│   └── LogQueryDTO.java           # 日志查询DTO
├── redis/                         # Redis模块DTO
│   └── RedisQueryDTO.java         # Redis查询DTO
└── system/                        # 系统模块DTO
    ├── LoginDTO.java              # 登录数据传输对象
    ├── MenuDTO.java               # 菜单DTO
    ├── PermissionDTO.java         # 权限DTO
    ├── RoleDTO.java               # 角色DTO
    ├── RoleMenuDTO.java           # 角色菜单关联DTO
    ├── RolePermissionDTO.java     # 角色权限关联DTO
    ├── SysMenuQueryDTO.java       # 系统菜单查询DTO
    ├── SysPermissionQueryDTO.java # 系统权限查询DTO
    ├── UserDTO.java               # 用户DTO
    └── UserRoleDTO.java           # 用户角色关联DTO
```
**作用**: 数据传输对象，用于前后端数据交互，封装请求参数和业务数据

#### 4.4 实体包 (framework.entity)
```
entity/
├── activity/                      # 活动模块实体
│   └── ActivityDO.java            # 活动实体
├── ai/                           # AI模块实体
│   ├── AiAgentDO.java            # AI代理实体
│   ├── AiChatMessageDO.java      # AI聊天消息实体
│   ├── AiChatSessionDO.java      # AI聊天会话实体
│   ├── AiLLMDO.java              # 大语言模型实体
│   └── AiLLMKeyDO.java           # LLM密钥实体
├── job/                          # 任务模块实体
│   └── JobDO.java                # 任务实体
├── log/                          # 日志模块实体
│   └── LogDO.java                # 日志实体
├── system/                       # 系统模块实体
│   ├── MenuDO.java               # 菜单实体
│   ├── PermissionDO.java         # 权限实体
│   ├── RoleDO.java               # 角色实体
│   ├── RoleMenuDO.java           # 角色菜单关联实体
│   ├── RolePermissionDO.java     # 角色权限关联实体
│   ├── UserDO.java               # 用户实体
│   └── UserRoleDO.java           # 用户角色关联实体
└── task/                         # 任务模块实体
    └── TaskDO.java               # 任务实体
```
**作用**: 数据库实体类，对应数据库表结构，使用MyBatis Plus进行ORM映射

#### 4.5 拦截器包 (framework.interceptor)
```
interceptor/
└── PermissionInterceptor.java     # 权限拦截器
```
**作用**: 请求拦截器，实现权限验证、日志记录等横切功能

#### 4.6 定时任务包 (framework.job)
```
job/
└── HeatTreatmentJob.java          # 热处理定时任务
```
**作用**: 定时任务类，使用Quartz框架实现定时调度

#### 4.7 数据访问包 (framework.mapper)
```
mapper/
├── TaskMapper.java                # 任务数据访问接口
├── ai/                           # AI模块Mapper
│   ├── AiAgentMapper.java        # AI代理Mapper
│   ├── AiChatMessageMapper.java  # AI聊天消息Mapper
│   ├── AiChatSessionMapper.java  # AI聊天会话Mapper
│   ├── AiLLMKeyMapper.java       # LLM密钥Mapper
│   └── AiLLMMapper.java          # 大语言模型Mapper
├── log/                          # 日志模块Mapper
│   └── LogMapper.java            # 日志Mapper
└── system/                       # 系统模块Mapper
    ├── SysMenuMapper.java        # 系统菜单Mapper
    ├── SysPermissionMapper.java  # 系统权限Mapper
    ├── SysRoleMapper.java        # 系统角色Mapper
    ├── SysRoleMenuMapper.java    # 角色菜单关联Mapper
    ├── SysRolePermissionMapper.java # 角色权限关联Mapper
    ├── SysUserMapper.java        # 系统用户Mapper
    └── SysUserRoleMapper.java    # 用户角色关联Mapper
```
**作用**: 数据访问层接口，继承MyBatis Plus的BaseMapper，提供CRUD操作

#### 4.8 查询对象包 (framework.query)
```
query/
├── ai/                           # AI模块查询对象
│   ├── AiAgentQuery.java         # AI代理查询对象
│   ├── AiChatSessionQuery.java   # AI聊天会话查询对象
│   └── AiLLMQuery.java           # 大语言模型查询对象
└── system/                       # 系统模块查询对象
    ├── RoleMenuQry.java          # 角色菜单查询对象
    ├── RolePermissionQry.java    # 角色权限查询对象
    ├── SysMenuQuery.java         # 系统菜单查询对象
    ├── SysPermissionQuery.java   # 系统权限查询对象
    ├── SysRoleQuery.java         # 系统角色查询对象
    ├── SysUserQuery.java         # 系统用户查询对象
    ├── TaskQuery.java            # 任务查询对象
    └── UserRoleQry.java          # 用户角色查询对象
```
**作用**: 查询条件封装对象，继承BaseQuery，用于复杂查询和分页查询

#### 4.9 业务服务包 (framework.service)
```
service/
├── ai/                           # AI模块服务
│   ├── IAiAgentService.java      # AI代理服务接口
│   ├── IAiChatService.java       # AI聊天服务接口
│   ├── IAiLLMService.java        # 大语言模型服务接口
│   └── impl/                     # 服务实现类
│       ├── AiAgentServiceImpl.java
│       ├── AiChatServiceImpl.java
│       └── AiLLMServiceImpl.java
├── dashboard/                    # 仪表盘模块服务
│   ├── DashboardService.java     # 仪表盘服务接口
│   └── impl/
│       └── DashboardServiceImpl.java
├── database/                     # 数据库模块服务
│   └── DatabaseInitService.java  # 数据库初始化服务
├── log/                         # 日志模块服务
│   ├── LogService.java          # 日志服务接口
│   └── impl/
│       └── LogServiceImpl.java
├── redis/                       # Redis模块服务
│   ├── RedisService.java        # Redis服务接口
│   └── impl/
│       └── RedisServiceImpl.java
├── system/                      # 系统模块服务
│   ├── IAuthCacheService.java   # 认证缓存服务接口
│   ├── IAuthService.java        # 认证服务接口
│   ├── ISysMenuService.java     # 系统菜单服务接口
│   ├── ISysPermissionService.java # 系统权限服务接口
│   ├── ISysRoleMenuService.java # 角色菜单服务接口
│   ├── ISysRolePermissionService.java # 角色权限服务接口
│   ├── ISysRoleService.java     # 系统角色服务接口
│   ├── ISysUserRoleService.java # 用户角色服务接口
│   ├── ISysUserService.java     # 系统用户服务接口
│   └── impl/                    # 服务实现类
│       ├── AuthCacheServiceImpl.java
│       ├── AuthServiceImpl.java
│       ├── SysMenuServiceImpl.java
│       ├── SysPermissionServiceImpl.java
│       ├── SysRoleMenuServiceImpl.java
│       ├── SysRolePermissionServiceImpl.java
│       ├── SysRoleServiceImpl.java
│       ├── SysUserRoleServiceImpl.java
│       └── SysUserServiceImpl.java
├── task/                        # 任务模块服务
│   ├── ITaskService.java        # 任务服务接口
│   └── impl/
│       └── TaskServiceImpl.java
└── workflow/                    # 工作流模块服务
    ├── IWorkflowService.java    # 工作流服务接口
    └── impl/
        └── WorkflowServiceImpl.java
```
**作用**: 业务逻辑层，包含接口定义和实现类，处理具体的业务逻辑

#### 4.10 视图对象包 (framework.vo)
```
vo/
├── ai/                          # AI模块视图对象
│   ├── AiAgentVO.java           # AI代理视图对象
│   ├── AiChatMessageVO.java     # AI聊天消息VO
│   ├── AiChatSessionVO.java     # AI聊天会话VO
│   ├── AiLLMKeyVO.java          # LLM密钥VO
│   └── AiLLMVO.java             # 大语言模型VO
├── dashboard/                   # 仪表盘模块VO
│   ├── DashboardStatsVO.java    # 仪表盘统计VO
│   ├── UserDistributionVO.java  # 用户分布VO
│   └── VisitTrendVO.java        # 访问趋势VO
├── log/                        # 日志模块VO
│   └── LogVO.java              # 日志视图对象
├── redis/                      # Redis模块VO
│   └── RedisKeyVO.java         # Redis键VO
└── system/                     # 系统模块VO
    ├── LoginVO.java            # 登录响应VO
    ├── RoleMenuVO.java         # 角色菜单VO
    ├── RolePermissionVO.java   # 角色权限VO
    ├── SysMenuVO.java          # 系统菜单VO
    ├── SysPermissionVO.java    # 系统权限VO
    ├── SysRoleVO.java          # 系统角色VO
    ├── SysUserVO.java          # 系统用户VO
    └── UserRoleVO.java         # 用户角色VO
```
**作用**: 视图对象，用于向前端返回数据，可以包含关联查询结果和计算字段

---

### 5. 代码生成器包 (com.zboot.generator)

```
generator/
├── CodeGenerator.java              # 代码生成器主类
├── CodeGeneratorTest.java          # 代码生成器测试类
└── README.md                       # 生成器使用说明
```
**作用**: 基于MyBatis Plus的代码生成器，自动生成Entity、Mapper、Service、Controller等代码

---

### 6. 项目模块包 (com.zboot.project)

```
project/
└── zboot_test/                     # 测试项目模块
    ├── controller/                 # 控制器
    ├── dto/                       # 数据传输对象
    ├── entity/                    # 实体类
    ├── mapper/                    # 数据访问层
    ├── query/                     # 查询对象
    ├── service/                   # 业务服务层
    └── vo/                        # 视图对象
```
**作用**: 具体的业务项目模块，按照标准的MVC分层结构组织

---

## 资源文件结构 (src/main/resources)

### 配置文件
```
resources/
├── application.yml                 # 主配置文件
├── application-dev.yml             # 开发环境配置
├── application-test.yml            # 测试环境配置
├── application-prod.yml            # 生产环境配置
└── banner.txt                      # 启动横幅
```

### MyBatis映射文件
```
mapper/
├── ai/                            # AI模块映射文件
│   ├── AiAgentMapper.xml
│   ├── AiChatMessageMapper.xml
│   ├── AiChatSessionMapper.xml
│   ├── AiLLMKeyMapper.xml
│   └── AiLLMMapper.xml
├── log/                           # 日志模块映射文件
│   └── LogMapper.xml
├── system/                        # 系统模块映射文件
│   ├── SysMenuMapper.xml
│   ├── SysPermissionMapper.xml
│   ├── SysRoleMapper.xml
│   ├── SysRoleMenuMapper.xml
│   ├── SysRolePermissionMapper.xml
│   ├── SysUserMapper.xml
│   └── SysUserRoleMapper.xml
└── user/                          # 用户模块映射文件
    └── UserMapper.xml
```

### SQL脚本
```
sql/
└── quartz_tables_mysql.sql        # Quartz定时任务表结构
```

### 代码模板
```
templates/
├── controller.btl                  # 控制器模板
├── dto.btl                        # DTO模板
├── entity.btl                     # 实体类模板
├── mapper.btl                     # Mapper接口模板
├── mapperXml.btl                  # Mapper XML模板
├── query.btl                      # 查询对象模板
├── service.btl                    # 服务接口模板
├── serviceImpl.btl                # 服务实现模板
└── vo.btl                         # VO模板
```

---

## 技术架构特点

### 1. 分层架构
- **Controller层**: 处理HTTP请求，参数验证，调用Service
- **Service层**: 业务逻辑处理，事务管理
- **Mapper层**: 数据访问，SQL映射
- **Entity层**: 数据库实体映射

### 2. 数据传输对象
- **DTO**: 前端到后端的数据传输
- **VO**: 后端到前端的数据传输
- **Query**: 查询条件封装
- **DO**: 数据库实体对象

### 3. 权限控制
- **Sa-Token**: 轻量级权限认证框架
- **注解式权限**: @RequirePermission注解
- **动态权限**: 基于Redis的URL权限映射
- **RBAC模型**: 用户-角色-权限-菜单四级权限体系

### 4. 缓存机制
- **Redis缓存**: 用户权限、菜单、角色信息缓存
- **多级缓存**: 本地缓存 + Redis分布式缓存
- **缓存更新**: 自动过期 + 手动清理

### 5. 代码生成
- **模板引擎**: Beetl模板引擎
- **自动生成**: Entity、Mapper、Service、Controller全套代码
- **标准化**: 统一的代码结构和命名规范

---

## 开发规范

### 1. 命名规范
- **包名**: 全小写，使用点分隔
- **类名**: 大驼峰命名法
- **方法名**: 小驼峰命名法
- **常量**: 全大写，下划线分隔

### 2. 分层规范
- **Controller**: 只处理请求响应，不包含业务逻辑
- **Service**: 包含业务逻辑，可以调用多个Mapper
- **Mapper**: 只负责数据访问，不包含业务逻辑

### 3. 异常处理
- **全局异常处理**: GlobalExceptionHandler统一处理
- **自定义异常**: 继承RuntimeException
- **异常分类**: 业务异常、系统异常、参数异常

### 4. 日志规范
- **日志级别**: ERROR、WARN、INFO、DEBUG
- **日志格式**: 统一的日志格式和内容
- **敏感信息**: 避免记录密码等敏感信息

---

## 总结

ZBoot超级脚手架采用了标准的Spring Boot分层架构，结合MyBatis Plus、Sa-Token等主流技术栈，实现了一个功能完整、结构清晰的后端项目框架。项目具有以下特点：

1. **模块化设计**: 按功能模块组织代码，便于维护和扩展
2. **标准化架构**: 遵循MVC分层架构，职责分离明确
3. **完善的权限体系**: 基于RBAC模型的细粒度权限控制
4. **高效的缓存机制**: Redis缓存提升系统性能
5. **自动化代码生成**: 提高开发效率，保证代码质量
6. **丰富的功能模块**: AI、工作流、定时任务等企业级功能

该项目可以作为企业级应用开发的基础框架，支持快速开发和部署。