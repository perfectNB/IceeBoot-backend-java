package com.zboot.generator;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成器
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
public class CodeGenerator {
    
    private static final String BASE_PACKAGE = "com.zboot.project.zboot_test";
    private static final String BASE_PATH = "src/main/java/com/zboot/project/zboot_test";
    private static final String RESOURCE_PATH = "src/main/resources";
    
    private GroupTemplate groupTemplate;
    
    public CodeGenerator() {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("templates/");
            Configuration cfg = Configuration.defaultConfiguration();
            groupTemplate = new GroupTemplate(resourceLoader, cfg);
        } catch (Exception e) {
            throw new RuntimeException("初始化模板引擎失败", e);
        }
    }
    
    /**
     * 生成代码
     * 
     * @param packageName 包名（如：user、order等）
     * @param entityName 实体名称（如：User、Order等）
     */
    public void generateCode(String packageName, String entityName) {
        try {
            // 创建包结构
            createPackageStructure(packageName);
            
            // 准备模板变量
            Map<String, Object> variables = prepareVariables(packageName, entityName);
            
            // 生成各种文件
            generateController(packageName, entityName, variables);
            generateService(packageName, entityName, variables);
            generateServiceImpl(packageName, entityName, variables);
            generateMapper(packageName, entityName, variables);
            generateEntity(packageName, entityName, variables);
            generateDTO(packageName, entityName, variables);
            generateVO(packageName, entityName, variables);
            generateQuery(packageName, entityName, variables);
            generateMapperXml(packageName, entityName, variables);
            
            System.out.println("代码生成完成！");
        } catch (Exception e) {
            throw new RuntimeException("代码生成失败", e);
        }
    }
    
    /**
     * 删除生成的代码
     * 
     * @param packageName 包名
     * @param entityName 实体名称
     */
    public void deleteCode(String packageName, String entityName) {
        try {
            String lowerEntityName = toLowerCamelCase(entityName);
            
            // 删除Java文件
            deleteFile(BASE_PATH + "/controller/" + packageName + "/" + entityName + "Controller.java");
            deleteFile(BASE_PATH + "/service/" + packageName + "/I" + entityName + "Service.java");
            deleteFile(BASE_PATH + "/service/" + packageName + "/impl/" + entityName + "ServiceImpl.java");
            deleteFile(BASE_PATH + "/mapper/" + packageName + "/" + entityName + "Mapper.java");
            deleteFile(BASE_PATH + "/entity/" + packageName + "/" + entityName + "DO.java");
            deleteFile(BASE_PATH + "/dto/" + packageName + "/" + entityName + "DTO.java");
            deleteFile(BASE_PATH + "/vo/" + packageName + "/" + entityName + "VO.java");
            deleteFile(BASE_PATH + "/query/" + packageName + "/" + entityName + "Query.java");
            
            // 删除XML文件
            deleteFile(RESOURCE_PATH + "/mapper/" + packageName + "/" + entityName + "Mapper.xml");
            
            System.out.println("代码删除完成！");
        } catch (Exception e) {
            throw new RuntimeException("代码删除失败", e);
        }
    }
    
    /**
     * 创建包结构
     */
    private void createPackageStructure(String packageName) throws IOException {
        String[] packages = {
            "controller/" + packageName,
            "service/" + packageName,
            "service/" + packageName + "/impl",
            "mapper/" + packageName,
            "entity/" + packageName,
            "dto/" + packageName,
            "vo/" + packageName,
            "query/" + packageName
        };
        
        for (String pkg : packages) {
            Path path = Paths.get(BASE_PATH, pkg);
            Files.createDirectories(path);
        }
        
        // 创建mapper xml目录
        Path mapperXmlPath = Paths.get(RESOURCE_PATH, "mapper", packageName);
        Files.createDirectories(mapperXmlPath);
    }
    
    /**
     * 准备模板变量
     */
    private Map<String, Object> prepareVariables(String packageName, String entityName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("packageName", packageName);
        variables.put("entityName", entityName);
        variables.put("lowerEntityName", toLowerCamelCase(entityName));
        variables.put("basePackage", BASE_PACKAGE);
        variables.put("author", "CodeIcee");
        variables.put("date", java.time.LocalDate.now().toString());
        return variables;
    }
    
    /**
     * 生成Controller
     */
    private void generateController(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("controller.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/controller/" + packageName + "/" + entityName + "Controller.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成Service接口
     */
    private void generateService(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("service.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/service/" + packageName + "/I" + entityName + "Service.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成Service实现类
     */
    private void generateServiceImpl(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("serviceImpl.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/service/" + packageName + "/impl/" + entityName + "ServiceImpl.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成Mapper
     */
    private void generateMapper(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("mapper.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/mapper/" + packageName + "/" + entityName + "Mapper.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成Entity
     */
    private void generateEntity(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("entity.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/entity/" + packageName + "/" + entityName + "DO.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成DTO
     */
    private void generateDTO(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("dto.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/dto/" + packageName + "/" + entityName + "DTO.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成VO
     */
    private void generateVO(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("vo.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/vo/" + packageName + "/" + entityName + "VO.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成Query
     */
    private void generateQuery(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("query.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/query/" + packageName + "/" + entityName + "Query.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成Mapper XML
     */
    private void generateMapperXml(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("mapperXml.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = RESOURCE_PATH + "/mapper/" + packageName + "/" + entityName + "Mapper.xml";
        writeFile(filePath, content);
    }
    
    /**
     * 写入文件
     */
    private void writeFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
    
    /**
     * 删除文件
     */
    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            System.out.println("删除文件: " + filePath);
        }
    }
    
    /**
     * 转换为小驼峰命名
     */
    private String toLowerCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
    
    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
        CodeGenerator generator = new CodeGenerator();
        
//         生成代码示例
         generator.generateCode("user", "User");
        
        // 删除代码示例
//         generator.deleteCode("user", "User");
    }
}