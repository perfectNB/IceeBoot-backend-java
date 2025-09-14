package com.iceeboot.generator;

/**
 * 代码生成器测试类
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
public class CodeGeneratorTest {
    
    public static void main(String[] args) {
        CodeGenerator generator = new CodeGenerator();
        
        // 示例1: 生成用户管理相关代码
        System.out.println("=== 生成用户管理代码 ===");
        generator.generateCode("user", "User");
        
        // 示例2: 生成订单管理相关代码
        System.out.println("\n=== 生成订单管理代码 ===");
        generator.generateCode("order", "Order");
        
        // 示例3: 生成产品管理相关代码
        System.out.println("\n=== 生成产品管理代码 ===");
        generator.generateCode("product", "Product");
        
        System.out.println("\n=== 代码生成完成 ===");
        
        // 如果需要删除生成的代码，可以使用以下方法：
        // System.out.println("\n=== 删除用户管理代码 ===");
        // generator.deleteCode("user", "User");
    }
}