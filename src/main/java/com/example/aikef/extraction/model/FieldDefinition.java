package com.example.aikef.extraction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字段定义
 * 描述结构化输出中的一个字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {

    /**
     * 字段名（英文，用于JSON key）
     */
    private String name;

    /**
     * 字段显示名（中文，用于提示用户）
     */
    private String displayName;

    /**
     * 字段类型
     */
    private FieldType type;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 字段描述（帮助AI理解字段含义）
     */
    private String description;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 枚举值（当type为ENUM时）
     */
    private List<String> enumValues;

    /**
     * 验证规则（正则表达式）
     */
    private String validationPattern;

    /**
     * 验证错误提示
     */
    private String validationMessage;

    /**
     * 追问提示（当字段缺失时的提问方式）
     */
    private String followupQuestion;

    /**
     * 示例值
     */
    private List<String> examples;

    /**
     * 字段顺序
     */
    private int order;

    /**
     * 嵌套属性（当type为OBJECT时）
     * 定义对象内部的字段
     */
    private List<FieldDefinition> properties;

    /**
     * 数组元素定义（当type为ARRAY时）
     * 定义数组元素的类型和结构
     */
    private FieldDefinition items;

    /**
     * 字段类型枚举
     */
    public enum FieldType {
        STRING,     // 字符串
        NUMBER,     // 数字
        INTEGER,    // 整数
        BOOLEAN,    // 布尔
        DATE,       // 日期 (yyyy-MM-dd)
        DATETIME,   // 日期时间 (yyyy-MM-dd HH:mm:ss)
        EMAIL,      // 邮箱
        PHONE,      // 电话
        ENUM,       // 枚举
        ARRAY,      // 数组
        OBJECT      // 嵌套对象
    }
}

