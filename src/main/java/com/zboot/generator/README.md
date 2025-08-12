# 代码生成器使用说明

## 概述

这是一个基于Beetl模板引擎的Java代码生成器，可以快速生成标准的CRUD代码结构，包括Controller、Service、Mapper、Entity、DTO、VO、Query等文件。

## 功能特性

- 🚀 **快速生成**: 一键生成完整的CRUD代码结构
- 📁 **智能包管理**: 自动检查并创建缺失的包结构
- 🎨 **模板化**: 基于Beetl模板引擎，易于定制
- 🔄 **支持删除**: 提供代码删除功能
- 📝 **规范命名**: 遵循驼峰命名法和Java编码规范
- 🏗️ **标准架构**: 生成的代码符合项目架构规范

## 生成的文件结构

```
src/main/java/com/zboot/framework/
├── controller/{packageName}/     # 控制器层
│   └── {EntityName}Controller.java
├── service/{packageName}/        # 服务层
│   ├── I{EntityName}Service.java
│   └── impl/
│       └── {EntityName}ServiceImpl.java
├── mapper/{packageName}/         # 数据访问层
│   └── {EntityName}Mapper.java
├── entity/{packageName}/         # 实体层
│   └── {EntityName}DO.java
├── dto/{packageName}/            # 数据传输对象
│   └── {EntityName}DTO.java
├── vo/{packageName}/             # 视图对象
│   └── {EntityName}VO.java
└── query/{packageName}/          # 查询对象
    └── {EntityName}Query.java

src/main/resources/
└── mapper/{packageName}/         # MyBatis XML映射文件
    └── {EntityName}Mapper.xml
```

## 使用方法

### 1. 基本使用

```java
// 创建代码生成器实例
CodeGenerator generator = new CodeGenerator();

// 生成代码
// 参数1: 包名（如：user、order、product）
// 参数2: 实体名称（如：User、Order、Product）
generator.generateCode("user", "User");
```

### 2. 批量生成

```java
CodeGenerator generator = new CodeGenerator();

// 生成多个模块
generator.generateCode("user", "User");       // 用户管理
generator.generateCode("order", "Order");     // 订单管理
generator.generateCode("product", "Product"); // 产品管理
```

### 3. 删除代码

```java
CodeGenerator generator = new CodeGenerator();

// 删除指定模块的代码
generator.deleteCode("user", "User");
```

### 4. 运行测试

直接运行 `CodeGeneratorTest.java` 中的main方法，查看生成效果。

## 模板定制

模板文件位于 `src/main/resources/templates/` 目录下：

- `controller.btl` - Controller模板
- `service.btl` - Service接口模板
- `serviceImpl.btl` - Service实现类模板
- `mapper.btl` - Mapper接口模板
- `entity.btl` - Entity实体类模板
- `dto.btl` - DTO模板
- `vo.btl` - VO模板
- `query.btl` - Query模板
- `mapperXml.btl` - MyBatis XML模板

### 模板变量

模板中可使用的变量：

- `${packageName}` - 包名（如：user）
- `${entityName}` - 实体名称（如：User）
- `${lowerEntityName}` - 小驼峰实体名称（如：user）
- `${basePackage}` - 基础包名（com.zboot.framework）
- `${author}` - 作者名称
- `${date}` - 当前日期

## 注意事项

1. **包名规范**: 包名应使用小写字母，如：user、order、product
2. **实体名规范**: 实体名应使用大驼峰命名，如：User、Order、Product
3. **字段补充**: 生成的代码中实体类、DTO、VO等需要手动添加具体字段
4. **业务逻辑**: Service实现类中的业务逻辑需要根据实际需求补充
5. **数据库表**: 需要手动创建对应的数据库表

## 依赖要求

确保项目中已添加Beetl模板引擎依赖：

```xml
<dependency>
    <groupId>com.ibeetl</groupId>
    <artifactId>beetl</artifactId>
    <version>3.15.10.RELEASE</version>
</dependency>
```

## 示例

生成用户管理模块：

```java
CodeGenerator generator = new CodeGenerator();
generator.generateCode("user", "User");
```

将会生成：
- UserController.java
- IUserService.java
- UserServiceImpl.java
- UserMapper.java
- UserDO.java
- UserDTO.java
- UserVO.java
- UserQuery.java
- UserMapper.xml

## 扩展说明

如需扩展功能，可以：

1. 修改模板文件以适应特定需求
2. 在CodeGenerator类中添加新的生成方法
3. 增加新的模板类型
4. 自定义变量和生成逻辑

---

**作者**: CodeIcee  
**日期**: 2025-08-12  
**版本**: 1.0.0