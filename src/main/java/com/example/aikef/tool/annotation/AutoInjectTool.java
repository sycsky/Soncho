package com.example.aikef.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记工具为自动注入
 * 被此注解标记的工具（类或方法）将自动注入到 AdvancedAgentNode 的工具列表中，无需 LLM 选择。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AutoInjectTool {
}
