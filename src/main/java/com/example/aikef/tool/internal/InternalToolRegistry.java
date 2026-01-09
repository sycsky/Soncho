package com.example.aikef.tool.internal;

import com.example.aikef.extraction.model.ExtractionSchema;
import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.extraction.repository.ExtractionSchemaRepository;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import com.example.aikef.tool.repository.ToolExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalToolRegistry {

    private final ApplicationContext applicationContext;
    private final AiToolRepository toolRepository;
    private final ExtractionSchemaRepository schemaRepository;
    private final ToolExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    // Cache: ToolName -> Bean Instance & Method
    private final Map<String, ToolMethod> toolMethods = new ConcurrentHashMap<>();

    private record ToolMethod(Object bean, Method method) {}

    private record ToolMethodDefinition(ToolSpecification spec, Object bean, Method method) {}

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        log.info("Scanning for internal tools...");
        
        // 1. Find all beans with @Tool annotated methods
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Map<String, ToolMethodDefinition> foundTools = new HashMap<>();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> beanClass = AopUtils.getTargetClass(bean); // Handle proxies
                
                // Scan methods
                for (Method method : beanClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Tool.class)) {
                         try {
                            ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                            // Avoid duplicate names (last one wins or log error)
                            if (foundTools.containsKey(spec.name())) {
                                log.warn("Duplicate tool name found: {}. Overwriting.", spec.name());
                            }
                            foundTools.put(spec.name(), new ToolMethodDefinition(spec, bean, method));
                            log.debug("Found internal tool: {} -> {}.{}", spec.name(), beanClass.getSimpleName(), method.getName());
                        } catch (Exception e) {
                            log.error("Failed to parse tool from method: {}.{}", beanClass.getSimpleName(), method.getName(), e);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore beans that can't be initialized or inspected
            }
        }

        if (foundTools.isEmpty()) {
            log.info("No internal tools found.");
            return;
        }

        // 2. Register or Update tools
        for (ToolMethodDefinition def : foundTools.values()) {
            registerTool(def);
        }
        
        // 3. (Optional) Cleanup tools that no longer exist in code?
        // Current logic deletes ALL old internal tools first, which is destructive for execution logs if not careful.
        // User asked to "reuse old tool ID and bodyTemplate".
        // So we should NOT delete all old tools blindly.
        
        log.info("Registered {} internal tools.", foundTools.values().size());
    }

    private void registerTool(ToolMethodDefinition def) {
        ToolSpecification spec = def.spec;
        
        // Cache execution info
        toolMethods.put(spec.name(), new ToolMethod(def.bean, def.method));

        // Find existing tool by name
        AiTool tool = toolRepository.findByNameWithSchema(spec.name()).orElse(null);
        
        if (tool == null) {
            // Create new
            tool = new AiTool();
            tool.setId(UUID.randomUUID());
            tool.setName(spec.name());
            tool.setDisplayName(spec.name());
            tool.setDescription(spec.description());
            tool.setToolType(AiTool.ToolType.INTERNAL);
            tool.setEnabled(true);
        } else {
            // Update existing (preserve ID, bodyTemplate, apiUrl)
            // Note: toolType should be INTERNAL. If it was API, we might be overwriting it?
            // Assuming name collision means same tool.
            tool.setDescription(spec.description());
            tool.setToolType(AiTool.ToolType.INTERNAL); // Ensure it's marked as INTERNAL
        }
        
        // Update Schema
        if (spec.parameters() != null) {
            JsonObjectSchema paramsSchema = spec.parameters();
            // 传入 method 用于检查参数注解
            List<FieldDefinition> fields = convertToFieldDefinitions(paramsSchema, def.method);
            
            // Filter fields: Only include those with @P annotation
            // User requirement: "If no parameter has @P annotation, do not put it into tool variable settings"
            // Wait, logic interpretation: 
            // "if no use parameter no '@P' annotation then do not put to tool variable setting"
            // -> Only parameters annotated with @P should be exposed in the schema?
            // Let's refine convertToFieldDefinitions to filter.
            
            ExtractionSchema schema = tool.getSchema();
            if (schema == null) {
                schema = new ExtractionSchema();
                schema.setName("tool_" + spec.name() + "_params");
                schema.setDescription("Internal tool parameters for " + spec.name());
            }
            
            try {
                schema.setFieldsJson(objectMapper.writeValueAsString(fields));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize fields for tool: {}", spec.name(), e);
                schema.setFieldsJson("[]");
            }
            tool.setSchema(schema);
        }

        toolRepository.save(tool);
    }

    public Object execute(String toolName, Map<String, Object> params, String body) throws Exception {
        ToolMethod toolMethod = toolMethods.get(toolName);
        if (toolMethod == null) {
            throw new IllegalArgumentException("Internal tool not found in registry: " + toolName);
        }
        
        // If body is provided (from bodyTemplate), use it to determine arguments
        if (body != null && !body.isBlank()) {
             // If method has exactly one parameter, try to convert the body to that parameter
             if (toolMethod.method.getParameterCount() == 1) {
                 Object arg = body;
                 Class<?> paramType = toolMethod.method.getParameterTypes()[0];
                 
                 // If parameter is not String, try to parse JSON
                 if (paramType != String.class) {
                     try {
                        arg = objectMapper.readValue(body, paramType);
                     } catch (Exception e) {
                         // If not JSON or parse fails, warn but proceed (might fail at invoke if type mismatch)
                         // But if it's a primitive/simple type, readValue might handle it if body is "123"
                         log.warn("Failed to convert body to {}: {}", paramType.getSimpleName(), e.getMessage());
                     }
                 }
                 return toolMethod.method.invoke(toolMethod.bean, arg);
             } else {
                 // If method has multiple parameters, try to parse body as JSON Map and use it as params
                 try {
                     Map<String, Object> bodyParams = objectMapper.readValue(body, new TypeReference<Map<String, Object>>(){});
                     // Use bodyParams as the effective params for invocation
                     return invokeMethod(toolMethod.bean, toolMethod.method, bodyParams);
                 } catch (Exception e) {
                     log.warn("Tool {} has bodyTemplate but method has {} parameters, and body could not be parsed as JSON Map. Ignoring body.", toolName, toolMethod.method.getParameterCount());
                 }
             }
        }
        
        return invokeMethod(toolMethod.bean, toolMethod.method, params);
    }

    private Object invokeMethod(Object bean, Method method, Map<String, Object> params) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = parameter.getName(); // Requires -parameters compiler flag or distinct mapping
            // Note: LangChain4j might generate arg0, arg1 if not compiled with parameters.
            // However, the ToolSpecification also uses those names. 
            // So if ToolSpecification says "arg0", the map key will be "arg0".
            // We need to trust that the map keys match parameter names.
            
            Object value = params.get(paramName);
            
            // Handle optional arguments / nulls
            if (value == null) {
                 // Try to look up by @P or other annotations if LangChain4j supports them?
                 // LangChain4j uses parameter names.
            }
            
            // Type conversion
            if (value != null) {
                // 使用 constructType 处理泛型 (例如 List<MyObject>)
                args[i] = objectMapper.convertValue(value, objectMapper.getTypeFactory().constructType(parameter.getParameterizedType()));
            } else {
                args[i] = null; // Or default value?
            }
        }

        return method.invoke(bean, args);
    }

    // --- Schema Conversion Logic ---

    private List<FieldDefinition> convertToFieldDefinitions(JsonObjectSchema jsonObjectSchema, Method method) {
        if (jsonObjectSchema == null || jsonObjectSchema.properties() == null) {
            return Collections.emptyList();
        }

        List<FieldDefinition> fields = new ArrayList<>();
        Map<String, JsonSchemaElement> properties = jsonObjectSchema.properties();
        
        // 原始的 required 列表（LangChain4j 生成的）
        List<String> originalRequired = jsonObjectSchema.required();

        // 遍历参数，重新判断是否必填，并过滤掉没有 @P 注解的参数
        Map<String, Boolean> requiredMap = new HashMap<>();
        Set<String> includedFields = new HashSet<>();
        
        if (method != null) {
            Parameter[] parameters = method.getParameters();
            for (Parameter param : parameters) {
                String paramName = param.getName(); // 需要 -parameters 编译参数
                boolean isRequired = false;
                
                // 1. 检查 @P 注解
                if (param.isAnnotationPresent(P.class)) {
                    P p = param.getAnnotation(P.class);
                    // 只有显式标记 required=true (默认值) 时，才认为是必填
                    isRequired = p.required();
                    includedFields.add(paramName);
                } 
                
                requiredMap.put(paramName, isRequired);
            }
        }

        for (Map.Entry<String, JsonSchemaElement> entry : properties.entrySet()) {
            String name = entry.getKey();
            
            // 过滤：如果方法存在且参数没有 @P 注解，则跳过
            if (method != null && !includedFields.contains(name)) {
                continue;
            }

            JsonSchemaElement element = entry.getValue();
            
            boolean isRequired;
            if (requiredMap.containsKey(name)) {
                // 如果能匹配到方法参数，使用我们的判断逻辑
                isRequired = requiredMap.get(name);
            } else {
                // 匹配不到（比如嵌套属性，或者是 LangChain4j 生成的名字不一致），回退到原始逻辑
                isRequired = originalRequired != null && originalRequired.contains(name);
            }
            
            fields.add(convertElementToField(name, element, isRequired));
        }
        return fields;
    }
    
    // 重载给嵌套对象使用（无 method 上下文）
    private List<FieldDefinition> convertToFieldDefinitions(JsonObjectSchema jsonObjectSchema) {
        return convertToFieldDefinitions(jsonObjectSchema, null);
    }

    private FieldDefinition convertElementToField(String name, JsonSchemaElement element, boolean isRequired) {
        FieldDefinition.FieldDefinitionBuilder builder = FieldDefinition.builder()
                .name(name)
                .displayName(name) // Use name as display name by default
                .description(element.description())
                .required(isRequired);

        if (element instanceof JsonStringSchema) {
            builder.type(FieldDefinition.FieldType.STRING);
        } else if (element instanceof JsonIntegerSchema) {
            builder.type(FieldDefinition.FieldType.INTEGER);
        } else if (element instanceof JsonNumberSchema) {
            builder.type(FieldDefinition.FieldType.NUMBER);
        } else if (element instanceof JsonBooleanSchema) {
            builder.type(FieldDefinition.FieldType.BOOLEAN);
        } else if (element instanceof JsonEnumSchema enumSchema) {
            builder.type(FieldDefinition.FieldType.ENUM);
            // Check if enumValues is List<String>
            List<String> stringValues = new ArrayList<>();
            for (Object val : enumSchema.enumValues()) {
                stringValues.add(val.toString());
            }
            builder.enumValues(stringValues);
        } else if (element instanceof JsonArraySchema arraySchema) {
            builder.type(FieldDefinition.FieldType.ARRAY);
            // Recursively convert items
            JsonSchemaElement items = arraySchema.items();
            if (items != null) {
                // items name is not really important here, use "item"
                builder.items(convertElementToField("item", items, false));
            }
        } else if (element instanceof JsonObjectSchema objectSchema) {
            builder.type(FieldDefinition.FieldType.OBJECT);
            // Recursively convert properties
            builder.properties(convertToFieldDefinitions(objectSchema));
        } else {
            // Fallback
            builder.type(FieldDefinition.FieldType.STRING);
        }

        return builder.build();
    }
}
