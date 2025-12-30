package com.example.aikef.workflow.node;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.LlmModel;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图文分割节点
 * 通过AI匹配上下文中的数据，如果是图文数据，将文本介绍和文本的配图提取成结构化数据
 * 
 * 输出格式: struct#{"struct":[{img:'',content:''},{img:'',content:''}],"overview":""}
 * - struct: 图文数据数组，每个元素包含img和content
 * - overview: 根据用户输入生成的介绍文字，用于回答问题
 * 如果不是图文数据，则不做任何处理，直接输出原内容
 * 
 * 配置示例:
 * {
 *   "modelId": "uuid",  // 可选，使用默认模型
 *   "systemPrompt": "你是一个图文内容分析助手，负责识别和提取图文数据"  // 可选
 * }
 */
@LiteflowComponent("imageTextSplit")
public class ImageTextSplitNode extends BaseWorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(ImageTextSplitNode.class);

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private LlmModelService llmModelService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            String input = getInput();
            if (input == null || input.trim().isEmpty()) {
                log.warn("图文分割节点输入为空，跳过处理");
                setOutput(input);
                recordExecution(input, input, startTime, true, null);
                return;
            }

            // 获取节点配置
            JsonNode config = getNodeConfig();
            String modelIdStr = getConfigString("modelId", null);
            String systemPrompt = getConfigString("systemPrompt", getDefaultSystemPrompt());

            // 构建提取提示词
            String extractionPrompt = buildExtractionPrompt(input);

            // 调用LLM进行图文识别和提取
            ImageTextResult result = extractImageTextData(input, extractionPrompt, systemPrompt, modelIdStr);

            String output;
            if (result != null && result.items != null && !result.items.isEmpty()) {
                // 有图文数据，格式化为 struct# 开头的结构化数据
                String structData = formatAsStruct(result);
                output = structData;
                log.info("图文分割节点提取到 {} 条图文数据，overview: {}", 
                        result.items.size(), 
                        result.overview != null && result.overview.length() > 50 ? 
                                result.overview.substring(0, 50) + "..." : result.overview);
            } else {
                // 没有图文数据，不做任何处理，直接输出原内容
                output = input;
                log.info("图文分割节点未识别到图文数据，保持原内容");
            }

            setOutput(output);
            recordExecution(input, output, startTime, true, null);

        } catch (Exception e) {
            log.error("图文分割节点执行失败", e);
            String errorOutput = getInput(); // 出错时保持原内容
            setOutput(errorOutput);
            recordExecution(getInput(), errorOutput, startTime, false, e.getMessage());
        }
    }

    /**
     * 提取图文数据
     */
    private ImageTextResult extractImageTextData(String input, String extractionPrompt, 
                                                 String systemPrompt, String modelIdStr) {
        try {
            // 获取模型配置
            LlmModel modelConfig = getModelConfig(modelIdStr);

            // 构建 JSON Schema（对象，包含 struct 数组和 overview 介绍文字）
            Map<String, dev.langchain4j.model.chat.request.json.JsonSchemaElement> itemProperties = new LinkedHashMap<>();
            itemProperties.put("img", JsonStringSchema.builder()
                    .description("图片URL或图片标识")
                    .build());
            itemProperties.put("content", JsonStringSchema.builder()
                    .description("对应的文本内容")
                    .build());

            JsonObjectSchema itemSchema = JsonObjectSchema.builder()
                    .addProperties(itemProperties)
                    .required("img", "content")
                    .build();

            Map<String, dev.langchain4j.model.chat.request.json.JsonSchemaElement> rootProperties = new LinkedHashMap<>();
            rootProperties.put("struct", JsonArraySchema.builder()
                    .items(itemSchema)
                    .description("图文数据列表")
                    .build());
            rootProperties.put("overview", JsonStringSchema.builder()
                    .description("根据用户输入生成的介绍文字，用于回答问题")
                    .build());

            JsonObjectSchema rootSchema = JsonObjectSchema.builder()
                    .addProperties(rootProperties)
                    .required("struct", "overview")
                    .build();
            
            LangChainChatService.StructuredOutputResponse response = langChainChatService.chatWithStructuredOutput(
                    modelConfig.getId(),
                    systemPrompt,
                    extractionPrompt,
                    rootSchema,
                    "image_text_items",
                    1.0
            );

            if (!response.success() || response.jsonResult() == null || response.jsonResult().trim().isEmpty()) {
                log.warn("LLM结构化输出失败: {}", response.errorMessage());
                return null;
            }

            String jsonText = response.jsonResult();
            if (jsonText == null || jsonText.trim().isEmpty()) {
                log.warn("LLM返回为空，无法提取图文数据");
                return null;
            }

            // 解析 JSON 响应
            JsonNode jsonNode = objectMapper.readTree(jsonText);
            JsonNode structNode = jsonNode.get("struct");
            JsonNode overviewNode = jsonNode.get("overview");
            
            if (structNode == null || !structNode.isArray() || structNode.size() == 0) {
                log.info("LLM返回的struct为空或不是数组，未识别到图文数据");
                return null;
            }

            // 转换为 ImageTextItem 列表
            List<ImageTextItem> items = new ArrayList<>();
            for (JsonNode itemNode : structNode) {
                String img = itemNode.has("img") ? itemNode.get("img").asText() : "";
                String content = itemNode.has("content") ? itemNode.get("content").asText() : "";
                
                // 过滤掉空的项
                if ((img != null && !img.trim().isEmpty()) || 
                    (content != null && !content.trim().isEmpty())) {
                    items.add(new ImageTextItem(img != null ? img : "", content != null ? content : ""));
                }
            }

            if (items.isEmpty()) {
                return null;
            }

            // 获取 overview（介绍文字）
            String overview = overviewNode != null && overviewNode.isTextual() ? overviewNode.asText() : "";
            
            // 返回包含 struct 和 overview 的对象
            return new ImageTextResult(items, overview);

        } catch (Exception e) {
            log.error("提取图文数据失败", e);
            return null;
        }
    }

    /**
     * 构建提取提示词
     */
    private String buildExtractionPrompt(String input) {
        WorkflowContext ctx = getWorkflowContext();
        String userQuery = ctx.getQuery(); // 获取用户输入，用于生成overview
        
        return String.format("""
            请分析以下内容，识别其中是否包含图文数据（一条文本对应一张图片）。
            
            如果是图文数据（例如商品列表、产品介绍等），请提取成结构化数据：
            - struct字段：数组，每条图文数据包含：图片URL或标识（img字段）和对应的文本内容（content字段）
            - 如果有多张图片，每条文本只取一张图片（优先取第一张或最相关的）
            - 如果只有文本没有图片，img字段为空字符串
            - 如果只有图片没有文本，content字段为空字符串
            - overview字段：根据用户输入"%s"生成的总体介绍文字，用于回答问题。要求：
              * 应该是概括性的介绍，不要重复struct中每个item的具体content内容
              * 可以从整体角度描述这些图文数据的特点、分类、用途等
              * 要自然流畅，不要太干燥
              * 例如：如果struct是商品列表，overview可以是"为您推荐以下几款热门商品"这样的总体介绍，而不是重复每个商品的具体描述
            
            如果不是图文数据（纯文本、纯图片列表等），请返回空的struct数组和空的overview。
            
            输入内容：
            %s
            
            请严格按照JSON格式返回，包含struct数组和overview字符串。
            """, userQuery != null ? userQuery : "用户查询", input);
    }

    /**
     * 获取默认系统提示词
     */
    private String getDefaultSystemPrompt() {
        return """
            你是一个图文内容分析助手，擅长识别和提取图文混合数据。
            你的任务是：
            1. 判断输入内容是否包含图文数据（文本+图片的组合）
            2. 如果是图文数据，提取每条图文对（一条文本对应一张图片）
            3. 如果有多张图片，每条文本只取一张最相关的图片
            4. 如果不是图文数据，返回空的struct数组和空的overview
            5. overview应该是总体介绍，不要重复struct中每个item的具体content内容
            
            请确保输出的JSON格式正确，struct是数组，overview是字符串。
            """;
    }

    /**
     * 格式化为 struct# 开头的结构化数据
     */
    private String formatAsStruct(ImageTextResult result) {
        try {
            // 转换为 Map 列表
            List<Map<String, String>> itemMaps = new ArrayList<>();
            for (ImageTextItem item : result.items) {
                Map<String, String> itemMap = new HashMap<>();
                itemMap.put("img", item.img != null ? item.img : "");
                itemMap.put("content", item.content != null ? item.content : "");
                itemMaps.add(itemMap);
            }

            // 构建包含 struct 和 overview 的对象
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("struct", itemMaps);
            resultMap.put("overview", result.overview != null ? result.overview : "");

            // 序列化为 JSON
            String json = objectMapper.writeValueAsString(resultMap);
            
            // 添加 struct# 前缀
            return "struct#" + json;
        } catch (Exception e) {
            log.error("格式化结构化数据失败", e);
            return getInput(); // 失败时返回原内容
        }
    }

    /**
     * 获取模型配置
     */
    private LlmModel getModelConfig(String modelIdStr) {
        if (modelIdStr != null && !modelIdStr.isEmpty()) {
            return llmModelService.getModel(java.util.UUID.fromString(modelIdStr));
        } else {
            return llmModelService.getDefaultModel()
                    .orElseThrow(() -> new RuntimeException("未配置默认模型"));
        }
    }

    /**
     * 图文项数据类
     */
    private static class ImageTextItem {
        String img;
        String content;

        ImageTextItem(String img, String content) {
            this.img = img;
            this.content = content;
        }
    }

    /**
     * 图文提取结果（包含struct数组和overview介绍）
     */
    private static class ImageTextResult {
        List<ImageTextItem> items;
        String overview;

        ImageTextResult(List<ImageTextItem> items, String overview) {
            this.items = items;
            this.overview = overview;
        }
    }
}
