package com.zboot.generator;

import com.zboot.common.exception.ZbootException;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Map<String, String> backupFiles = new HashMap<>(); // 备份文件内容
    private Scanner scanner = new Scanner(System.in);
    
    public CodeGenerator() {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("templates/");
            Configuration cfg = Configuration.defaultConfiguration();
            groupTemplate = new GroupTemplate(resourceLoader, cfg);
        } catch (Exception e) {
            throw new ZbootException("初始化模板引擎失败", e);
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
            generateCreateDTO(packageName, entityName, variables);
            generateUpdateDTO(packageName, entityName, variables);
            generateVO(packageName, entityName, variables);
            generateQuery(packageName, entityName, variables);
            generateMapperXml(packageName, entityName, variables);
            
            System.out.println("代码生成完成！");
        } catch (Exception e) {
            throw new ZbootException("代码生成失败", e);
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
            // 检测实体类字段数量
            String entityPath = BASE_PATH + "/entity/" + packageName + "/" + entityName + "DO.java";
            int fieldCount = countEntityFields(entityPath);
            
            // 根据字段数量给出不同提示
            boolean shouldDelete = false;
            if (fieldCount == 0) {
                System.out.println("检测到实体类 " + entityName + "DO 没有业务字段（不算继承字段）。");
                System.out.print("是否删除？(Y/N): ");
                String input = scanner.nextLine().trim();
                shouldDelete = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
            } else {
                System.out.println("警告：被删除的 " + entityName + " 很可能包含需要的业务逻辑！");
                System.out.println("检测到实体类有 " + fieldCount + " 个业务字段。");
                System.out.print("确定要删除？(Y/N): ");
                String input = scanner.nextLine().trim();
                shouldDelete = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
            }
            
            if (!shouldDelete) {
                System.out.println("删除操作已取消。");
                return;
            }
            
            // 备份文件内容
            backupFiles.clear();
            List<String> filesToDelete = getFilesToDelete(packageName, entityName);
            
            for (String filePath : filesToDelete) {
                backupFile(filePath);
            }
            
            // 执行删除
            for (String filePath : filesToDelete) {
                deleteFile(filePath);
            }
            
            // 删除空目录
            deleteEmptyDirectories(packageName, entityName);
            
            System.out.println("代码删除完成！");
            
            // 询问是否恢复
            if (fieldCount > 0) {
                System.out.print("是否恢复刚才删除的文件？(Y/N): ");
                String restoreInput = scanner.nextLine().trim();
                if (restoreInput.equalsIgnoreCase("y") || restoreInput.equalsIgnoreCase("yes")) {
                    restoreFiles();
                    System.out.println("文件已恢复！");
                } else {
                    System.out.println("删除操作已完成。");
                }
            }
            
        } catch (Exception e) {
            throw new ZbootException("代码删除失败", e);
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
            "dto/create",
            "dto/update",
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
     * 生成创建DTO
     */
    private void generateCreateDTO(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("createDto.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/dto/create/" + entityName + "DTO.java";
        writeFile(filePath, content);
    }
    
    /**
     * 生成更新DTO
     */
    private void generateUpdateDTO(String packageName, String entityName, Map<String, Object> variables) throws IOException {
        Template template = groupTemplate.getTemplate("updateDto.btl");
        template.binding(variables);
        String content = template.render();
        
        String filePath = BASE_PATH + "/dto/update/" + entityName + "UpdateDTO.java";
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
     * 统计实体类字段数量（不包括继承字段）
     */
    private int countEntityFields(String entityPath) {
        File file = new File(entityPath);
        if (!file.exists()) {
            return 0;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int fieldCount = 0;
            boolean inClassBody = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 检测是否进入类体
                if (line.contains("public class") && line.contains("{")) {
                    inClassBody = true;
                    continue;
                }
                
                if (inClassBody) {
                    // 匹配字段定义（private/protected/public + 类型 + 字段名）
                    Pattern fieldPattern = Pattern.compile("^(private|protected|public)\\s+\\w+.*\\s+\\w+\\s*;");
                    Matcher matcher = fieldPattern.matcher(line);
                    
                    if (matcher.find() && 
                        !line.contains("static") && 
                        !line.contains("final") &&
                        !line.startsWith("//") &&
                        !line.startsWith("*")) {
                        fieldCount++;
                    }
                }
            }
            
            return fieldCount;
        } catch (IOException e) {
            System.err.println("读取实体类文件失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取要删除的文件列表
     */
    private List<String> getFilesToDelete(String packageName, String entityName) {
        List<String> files = new ArrayList<>();
        
        // Java文件
        files.add(BASE_PATH + "/controller/" + packageName + "/" + entityName + "Controller.java");
        files.add(BASE_PATH + "/service/" + packageName + "/I" + entityName + "Service.java");
        files.add(BASE_PATH + "/service/" + packageName + "/impl/" + entityName + "ServiceImpl.java");
        files.add(BASE_PATH + "/mapper/" + packageName + "/" + entityName + "Mapper.java");
        files.add(BASE_PATH + "/entity/" + packageName + "/" + entityName + "DO.java");
        files.add(BASE_PATH + "/dto/create/" + entityName + "DTO.java");
        files.add(BASE_PATH + "/dto/update/" + entityName + "UpdateDTO.java");
        files.add(BASE_PATH + "/vo/" + packageName + "/" + entityName + "VO.java");
        files.add(BASE_PATH + "/query/" + packageName + "/" + entityName + "Query.java");
        
        // XML文件
        files.add(RESOURCE_PATH + "/mapper/" + packageName + "/" + entityName + "Mapper.xml");
        
        return files;
    }
    
    /**
     * 备份文件内容
     */
    private void backupFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                backupFiles.put(filePath, content.toString());
            } catch (IOException e) {
                System.err.println("备份文件失败: " + filePath + ", " + e.getMessage());
            }
        }
    }
    
    /**
      * 恢复文件
      */
     private void restoreFiles() {
         for (Map.Entry<String, String> entry : backupFiles.entrySet()) {
             String filePath = entry.getKey();
             String content = entry.getValue();
             
             try {
                 writeFile(filePath, content);
                 System.out.println("恢复文件: " + filePath);
             } catch (IOException e) {
                 System.err.println("恢复文件失败: " + filePath + ", " + e.getMessage());
             }
         }
     }
     
     /**
      * 删除空目录
      */
     private void deleteEmptyDirectories(String packageName, String entityName) {
         List<String> directoriesToCheck = new ArrayList<>();
         
         // 添加可能变空的目录
         directoriesToCheck.add(BASE_PATH + "/controller/" + packageName);
         directoriesToCheck.add(BASE_PATH + "/service/" + packageName + "/impl");
         directoriesToCheck.add(BASE_PATH + "/service/" + packageName);
         directoriesToCheck.add(BASE_PATH + "/mapper/" + packageName);
         directoriesToCheck.add(BASE_PATH + "/entity/" + packageName);
         directoriesToCheck.add(BASE_PATH + "/vo/" + packageName);
         directoriesToCheck.add(BASE_PATH + "/query/" + packageName);
         directoriesToCheck.add(RESOURCE_PATH + "/mapper/" + packageName);
         
         // 检查dto目录是否还有其他文件
         checkAndDeleteDtoDirectories();
         
         // 删除空的包目录（从最深层开始）
         for (String dirPath : directoriesToCheck) {
             deleteDirectoryIfEmpty(dirPath);
         }
     }
     
     /**
      * 检查并删除DTO相关的空目录
      */
     private void checkAndDeleteDtoDirectories() {
         // 检查 /dto/create 目录
         String createDtoDir = BASE_PATH + "/dto/create";
         deleteDirectoryIfEmpty(createDtoDir);
         
         // 检查 /dto/update 目录
         String updateDtoDir = BASE_PATH + "/dto/update";
         deleteDirectoryIfEmpty(updateDtoDir);
         
         // 检查 /dto 根目录
         String dtoDir = BASE_PATH + "/dto";
         deleteDirectoryIfEmpty(dtoDir);
     }
     
     /**
      * 删除目录（如果为空）
      */
     private void deleteDirectoryIfEmpty(String dirPath) {
         File dir = new File(dirPath);
         if (dir.exists() && dir.isDirectory()) {
             String[] files = dir.list();
             if (files != null && files.length == 0) {
                 if (dir.delete()) {
                     System.out.println("删除空目录: " + dirPath);
                 }
             }
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