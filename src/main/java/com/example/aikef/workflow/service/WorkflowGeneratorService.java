package com.example.aikef.workflow.service;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.LlmModel;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流生成服务
 * 根据用户提示使用AI生成工作流
 */
@Service
public class WorkflowGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowGeneratorService.class);

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private LlmModelService llmModelService;

    @Resource
    private AiToolRepository toolRepository;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 根据用户提示生成或修改工作流
     * 
     * @param userPrompt 用户提示
     * @param modelId 模型ID（可选）
     * @param existingNodesJson 现有节点JSON（可选，用于修改现有工作流）
     * @param existingEdgesJson 现有边JSON（可选，用于修改现有工作流）
     * @return 生成的工作流
     */
    public GeneratedWorkflow generateWorkflow(String userPrompt, UUID modelId, 
                                               String existingNodesJson, String existingEdgesJson) {
        try {
            log.info("开始生成工作流: userPrompt={}, modelId={}", userPrompt, modelId);

            // 1. 获取所有可用工具
            List<AiTool> availableTools = toolRepository.findByEnabledTrueOrderBySortOrderAsc();
            String toolsInfo = buildToolsInfo(availableTools);

            // 2. 获取所有可用模型
            List<LlmModel> availableModels = llmModelService.getEnabledModels();
            String modelsInfo = buildModelsInfo(availableModels);

            // 3. 解析现有工作流（如果提供）
            List<JsonNode> existingNodes = new ArrayList<>();
            List<JsonNode> existingEdges = new ArrayList<>();
            boolean hasExistingWorkflow = false;

            if (existingNodesJson != null && !existingNodesJson.trim().isEmpty() && 
                !existingNodesJson.trim().equals("[]")) {
                try {
                    JsonNode existingNodesArray = objectMapper.readTree(existingNodesJson);
                    if (existingNodesArray.isArray()) {
                        for (JsonNode node : existingNodesArray) {
                            existingNodes.add(node);
                        }
                        hasExistingWorkflow = true;
                    }
                } catch (Exception e) {
                    log.warn("解析现有节点失败，将创建新工作流: {}", e.getMessage());
                }
            }

            if (existingEdgesJson != null && !existingEdgesJson.trim().isEmpty() && 
                !existingEdgesJson.trim().equals("[]")) {
                try {
                    JsonNode existingEdgesArray = objectMapper.readTree(existingEdgesJson);
                    if (existingEdgesArray.isArray()) {
                        for (JsonNode edge : existingEdgesArray) {
                            existingEdges.add(edge);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析现有边失败: {}", e.getMessage());
                }
            }

            // 4. 构建系统提示词（包含现有工作流信息）
            String systemPrompt = buildSystemPrompt(toolsInfo, modelsInfo, existingNodes, existingEdges, hasExistingWorkflow);

            // 5. 使用LLM生成工作流
            LlmModel model = modelId != null ? 
                    llmModelService.getModel(modelId) : 
                    llmModelService.getDefaultModel().orElseThrow(() -> 
                            new IllegalStateException("未配置默认模型"));

            String workflowJson = generateWorkflowWithLLM(systemPrompt, userPrompt, model);

            // 6. 解析并验证生成的工作流
            JsonNode workflowNode = objectMapper.readTree(workflowJson);
            JsonNode nodesArray = workflowNode.get("nodes");
            JsonNode edgesArray = workflowNode.get("edges");

            if (nodesArray == null || !nodesArray.isArray()) {
                throw new IllegalArgumentException("生成的工作流缺少 nodes 数组");
            }
            if (edgesArray == null || !edgesArray.isArray()) {
                throw new IllegalArgumentException("生成的工作流缺少 edges 数组");
            }

            // 7. 合并现有节点和新生成的节点
            List<JsonNode> nodes = new ArrayList<>(existingNodes);
            Set<String> existingNodeIds = existingNodes.stream()
                    .map(n -> n.get("id").asText())
                    .collect(Collectors.toSet());

            // 添加新节点（避免ID冲突）
            for (JsonNode node : nodesArray) {
                JsonNode fixedNode = fixNode(node);
                String nodeId = fixedNode.get("id").asText();
                
                // 如果ID已存在，生成新ID
                if (existingNodeIds.contains(nodeId)) {
                    String newId = generateUniqueNodeId(existingNodeIds);
                    ((ObjectNode) fixedNode).put("id", newId);
                    existingNodeIds.add(newId);
                } else {
                    existingNodeIds.add(nodeId);
                }
                
                nodes.add(fixedNode);
            }

            // 8. 合并现有边和新生成的边
            List<JsonNode> edges = new ArrayList<>(existingEdges);
            for (JsonNode edge : edgesArray) {
                JsonNode fixedEdge = fixEdge(edge, nodes);
                if (fixedEdge != null) {
                    edges.add(fixedEdge);
                }
            }

            // 7. 构建返回结果
            ObjectNode result = objectMapper.createObjectNode();
            result.set("nodes", objectMapper.valueToTree(nodes));
            result.set("edges", objectMapper.valueToTree(edges));

            String nodesJson = objectMapper.writeValueAsString(nodes);
            String edgesJson = objectMapper.writeValueAsString(edges);

            log.info("工作流生成成功: nodes={}, edges={}", nodes.size(), edges.size());

            return new GeneratedWorkflow(nodesJson, edgesJson, result.toString());

        } catch (Exception e) {
            log.error("生成工作流失败", e);
            throw new RuntimeException("生成工作流失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建工具信息字符串
     */
    private String buildToolsInfo(List<AiTool> tools) {
        if (tools.isEmpty()) {
            return "当前没有可用的工具。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表（共 ").append(tools.size()).append(" 个）：\n\n");
        
        for (AiTool tool : tools) {
            sb.append("- 工具名称: ").append(tool.getName()).append("\n");
            if (tool.getDisplayName() != null) {
                sb.append("  显示名称: ").append(tool.getDisplayName()).append("\n");
            }
            if (tool.getDescription() != null) {
                sb.append("  描述: ").append(tool.getDescription()).append("\n");
            }
            sb.append("  类型: ").append(tool.getToolType()).append("\n");
            sb.append("  工具ID: ").append(tool.getId()).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建模型信息字符串
     */
    private String buildModelsInfo(List<LlmModel> models) {
        if (models.isEmpty()) {
            return "当前没有可用的模型。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("可用模型列表（共 ").append(models.size()).append(" 个）：\n\n");
        
        for (LlmModel model : models) {
            sb.append("- 模型名称: ").append(model.getName()).append("\n");
            sb.append("  模型ID: ").append(model.getId()).append("\n");
            sb.append("  提供商: ").append(model.getProvider()).append("\n");
            sb.append("  模型代码: ").append(model.getCode()).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 从文档中加载节点类型说明
     */
    private String loadNodeTypesDocumentation() {
        try {
            ClassPathResource resource = new ClassPathResource("docs/workflow_node.md");
            if (!resource.exists()) {
                log.warn("工作流节点文档不存在: docs/workflow_node.md，使用默认说明");
                return getDefaultNodeTypesDoc();
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            // 提取 "## Node Types" 到 "---" 之间的内容
            int nodeTypesStart = content.indexOf("## Node Types");
            int nodeTypesEnd = content.indexOf("---", nodeTypesStart);
            
            if (nodeTypesStart < 0) {
                log.warn("文档中未找到 '## Node Types' 部分，使用默认说明");
                return getDefaultNodeTypesDoc();
            }
            
            String nodeTypesSection = nodeTypesEnd > nodeTypesStart 
                ? content.substring(nodeTypesStart, nodeTypesEnd)
                : content.substring(nodeTypesStart);
            
            // 将 Markdown 转换为适合系统提示词的格式
            return convertMarkdownToPromptFormat(nodeTypesSection);
            
        } catch (IOException e) {
            log.error("读取工作流节点文档失败，使用默认说明", e);
            return getDefaultNodeTypesDoc();
        }
    }
    
    /**
     * 将 Markdown 格式转换为系统提示词格式
     */
    private String convertMarkdownToPromptFormat(String markdown) {
        StringBuilder sb = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inNodeSection = false;
        int nodeNumber = 0;
        StringBuilder currentConfig = new StringBuilder();
        boolean inConfigSection = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 跳过标题行
            if (trimmed.startsWith("## Node Types")) {
                continue;
            }
            
            // 检测节点标题（### 数字. 节点名称 (`type`)）
            if (trimmed.startsWith("### ")) {
                // 如果之前有节点，先输出配置
                if (inNodeSection && currentConfig.length() > 0) {
                    sb.append(currentConfig);
                    currentConfig.setLength(0);
                }
                
                if (inNodeSection) {
                    sb.append("\n");
                }
                inNodeSection = true;
                inConfigSection = false;
                nodeNumber++;
                
                // 提取节点类型和描述
                String nodeInfo = line.replace("### ", "").trim();
                // 格式: "数字. 节点名称 (`type`)"
                // 提取节点类型（在反引号中）
                int backtickStart = nodeInfo.indexOf('`');
                int backtickEnd = nodeInfo.indexOf('`', backtickStart + 1);
                
                if (backtickStart >= 0 && backtickEnd > backtickStart) {
                    String nodeType = nodeInfo.substring(backtickStart + 1, backtickEnd);
                    String nodeName = nodeInfo.substring(0, backtickStart).trim();
                    // 移除开头的数字和点
                    if (nodeName.matches("^\\d+\\.\\s*.*")) {
                        nodeName = nodeName.replaceFirst("^\\d+\\.\\s*", "");
                    }
                    
                    sb.append(nodeNumber).append(". **").append(nodeType).append("** - ").append(nodeName).append("\n");
                } else {
                    sb.append(nodeNumber).append(". ").append(nodeInfo).append("\n");
                }
                continue;
            }
            
            // 处理配置信息
            if (inNodeSection) {
                if (trimmed.startsWith("- **Description**:")) {
                    String desc = line.replace("- **Description**:", "").trim();
                    sb.append("   - 描述: ").append(desc).append("\n");
                } else if (trimmed.startsWith("- **Configuration") || trimmed.startsWith("- **Parameters")) {
                    // 配置或参数部分
                    inConfigSection = true;
                    if (trimmed.contains("None") || trimmed.contains("无需")) {
                        sb.append("   - 配置: 无需配置\n");
                        inConfigSection = false;
                    } else {
                        currentConfig.append("   - 配置项:\n");
                    }
                } else if (inConfigSection && (trimmed.startsWith("- `") || trimmed.startsWith("    - `") || trimmed.startsWith("        - `"))) {
                    // 配置项（有缩进或无缩进，包括嵌套配置）
                    String configItem = trimmed;
                    // 移除 Markdown 格式
                    configItem = configItem.replace("`", "");
                    configItem = configItem.replace("**", "");
                    // 提取字段名和说明
                    int colonIndex = configItem.indexOf(':');
                    if (colonIndex > 0) {
                        String fieldName = configItem.substring(0, colonIndex).trim();
                        String fieldDesc = configItem.substring(colonIndex + 1).trim();
                        // 移除开头的 "- "
                        if (fieldName.startsWith("- ")) {
                            fieldName = fieldName.substring(2);
                        }
                        // 根据缩进级别决定输出格式
                        int indentLevel = line.length() - line.trim().length();
                        if (indentLevel >= 8) {
                            // 嵌套配置项（8个空格或更多）
                            currentConfig.append("       - ").append(fieldName).append(": ").append(fieldDesc).append("\n");
                        } else {
                            // 顶级配置项（4个空格）
                            currentConfig.append("     - ").append(fieldName).append(": ").append(fieldDesc).append("\n");
                        }
                    } else {
                        // 移除开头的 "- "
                        if (configItem.startsWith("- ")) {
                            configItem = configItem.substring(2);
                        }
                        int indentLevel = line.length() - line.trim().length();
                        if (indentLevel >= 8) {
                            currentConfig.append("       - ").append(configItem).append("\n");
                        } else {
                            currentConfig.append("     - ").append(configItem).append("\n");
                        }
                    }
                } else if (trimmed.isEmpty() && inConfigSection) {
                    // 空行，可能是配置项之间的分隔
                    // 不做处理，继续
                } else if (!trimmed.isEmpty() && inConfigSection && !trimmed.startsWith("-") && !trimmed.startsWith("`")) {
                    // 非配置项行，结束配置部分
                    inConfigSection = false;
                }
            }
        }
        
        // 输出最后一个节点的配置
        if (currentConfig.length() > 0) {
            sb.append(currentConfig);
        }
        
        // 添加 Switch 节点的特殊说明
        sb.append("\n**Switch 类型节点说明**：\n");
        sb.append("- **tool** 节点有两个固定输出: \"executed\"（执行成功）和 \"not_executed\"（未执行）\n");
        sb.append("- **intent** 节点输出: 使用意图配置中的 id\n");
        
        return sb.toString();
    }
    
    /**
     * 默认节点类型说明（当文档读取失败时使用）
     */
    private String getDefaultNodeTypesDoc() {
        return """
                1. **start** - 开始节点（必须且只能有一个）
                   - 配置: 无需配置
                
                2. **end** - 结束节点（必须且至少有一个）
                   - 配置: 无需配置
                
                3. **intent** - 意图识别节点（Switch类型）
                   - 配置: {
                       "modelId": "模型UUID",
                       "customPrompt": "自定义提示词",
                       "historyCount": 0,
                       "intents": [{"id": "intent_id", "label": "意图描述"}]
                     }
                   - 输出: 使用意图配置中的 id
                
                4. **llm** - LLM调用节点
                   - 配置: {
                       "modelId": "模型UUID",
                       "systemPrompt": "系统提示词",
                       "temperature": 0.7,
                       "useHistory": true,
                       "readCount": 5,
                       "tools": ["工具UUID"]
                     }
                
                5. **knowledge** - 知识库查询节点
                   - 配置: {
                       "knowledgeBaseIds": ["知识库UUID"],
                       "topK": 3,
                       "scoreThreshold": 0.5
                     }
                
                6. **reply** - 回复节点
                   - 配置: {
                       "text": "回复内容",
                       "source": "来源说明"
                     }
                
                7. **human_transfer** - 转人工节点
                   - 配置: 无需配置
                
                8. **flow** - 子工作流节点
                   - 配置: {
                       "workflowId": "工作流UUID"
                     }
                
                9. **flow_end** - 子工作流结束节点
                   - 配置: 无需配置
                
                10. **flow_update** - 子工作流更新节点
                    - 配置: {
                        "updateMode": "replace"
                      }
                
                11. **agent** - 自主Agent节点
                    - 配置: {
                        "goal": "目标指令",
                        "modelId": "模型UUID",
                        "tools": ["工具UUID"],
                        "maxIterations": 10,
                        "useHistory": true
                      }
                
                12. **tool** - 工具执行节点（Switch类型）
                    - 配置: {
                        "toolId": "工具UUID",
                        "toolName": "工具名称"
                      }
                    - 输出: "executed" 或 "not_executed"
                
                13. **imageTextSplit** - 图文分割节点
                    - 配置: {
                        "modelId": "模型UUID",
                        "systemPrompt": "提示词"
                      }
                
                14. **setSessionMetadata** - 会话元数据设置节点
                    - 配置: {
                        "modelId": "模型UUID",
                        "systemPrompt": "提示词",
                        "mappings": {"sourceField": "targetField"}
                      }
                
                15. **condition** - 条件判断节点（Switch类型）
                    - 配置: {
                        "conditions": [
                          {
                            "id": "条件ID",
                            "sourceValue": "变量或模板",
                            "conditionType": "contains/equals/...",
                            "inputValue": "比较值"
                          }
                        ]
                      }
                    - 输出: 匹配的条件ID 或 "else"
                
                16. **parameter_extraction** - 参数提取节点（Switch类型）
                    - 配置: {
                        "toolId": "工具UUID",
                        "toolName": "工具名称",
                        "modelId": "模型UUID",
                        "historyCount": 0,
                        "extractParams": ["参数名"]
                      }
                    - 输出: "success" 或 "incomplete"
                
                17. **variable** - 变量操作节点
                    - 配置: {
                        "variableName": "变量名",
                        "sourceField": "字段路径",
                        "sourceNodeId": "节点ID"
                      }
                    - 或: {"operation": "set", "variables": {"key": "value"}}
                
                18. **translation** - 翻译节点
                    - 配置: {
                        "modelId": "模型UUID",
                        "targetText": "目标文本",
                        "systemPrompt": "风格提示",
                        "outputVar": "变量名"
                      }
                
                19. **api** - API调用节点
                    - 配置: {
                        "url": "URL地址",
                        "method": "GET/POST",
                        "headers": {},
                        "body": {},
                        "responseMapping": "JSONPath",
                        "saveToVariable": "变量名"
                      }
                
                **Switch 类型节点说明**：
                - **tool** 节点有两个固定输出: "executed"（执行成功）和 "not_executed"（未执行）
                - **intent** 节点输出: 使用意图配置中的 id
                - **parameter_extraction** 节点有两个固定输出: "success"（参数完整）和 "incomplete"（参数不完整）
                - **condition** 节点输出: 匹配的条件 id 或 "else"
                """;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String toolsInfo, String modelsInfo, 
                                     List<JsonNode> existingNodes, List<JsonNode> existingEdges,
                                     boolean hasExistingWorkflow) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个专业的工作流设计专家，擅长根据用户需求设计AI工作流。\n\n");
        
        // 如果有现有工作流，添加说明
        if (hasExistingWorkflow) {
            promptBuilder.append("## 现有工作流信息\n\n");
            promptBuilder.append("用户提供了一个现有的工作流，你需要基于此工作流进行修改或扩展。\n\n");
            
            if (!existingNodes.isEmpty()) {
                promptBuilder.append("现有节点（共 ").append(existingNodes.size()).append(" 个）：\n");
                try {
                    String existingNodesStr = objectMapper.writeValueAsString(existingNodes);
                    promptBuilder.append(existingNodesStr).append("\n\n");
                } catch (Exception e) {
                    log.warn("序列化现有节点失败", e);
                }
            }
            
            if (!existingEdges.isEmpty()) {
                promptBuilder.append("现有边（共 ").append(existingEdges.size()).append(" 个）：\n");
                try {
                    String existingEdgesStr = objectMapper.writeValueAsString(existingEdges);
                    promptBuilder.append(existingEdgesStr).append("\n\n");
                } catch (Exception e) {
                    log.warn("序列化现有边失败", e);
                }
            }
            
            promptBuilder.append("**重要提示**：\n");
            promptBuilder.append("- 如果用户要求修改现有节点，请保留该节点但更新其配置\n");
            promptBuilder.append("- 如果用户要求添加新功能，请在现有工作流基础上添加新节点\n");
            promptBuilder.append("- 如果用户要求删除节点，请从输出中移除该节点\n");
            promptBuilder.append("- 请保持现有节点的ID不变（除非用户明确要求修改）\n");
            promptBuilder.append("- 新添加的节点ID不能与现有节点ID冲突\n");
            promptBuilder.append("- 请确保所有边的source和target都指向有效的节点ID\n\n");
        } else {
            promptBuilder.append("## 创建新工作流\n\n");
            promptBuilder.append("用户要求创建一个全新的工作流。请根据用户需求设计完整的工作流。\n\n");
        }
        
        promptBuilder.append("## 可用资源\n\n");
        promptBuilder.append(toolsInfo).append("\n");
        promptBuilder.append(modelsInfo).append("\n\n");
        
        promptBuilder.append("## 节点类型说明\n\n");
        
        // 从文档中读取节点说明
        String nodeTypesDoc = loadNodeTypesDocumentation();
        promptBuilder.append(nodeTypesDoc).append("\n\n");
        
        return String.format(promptBuilder.toString() + """
                ## 工作流设计规则
                
                1. **必须包含的节点**:
                   - 至少一个 start 节点（通常放在最左侧，x=0）
                   - 至少一个 end 节点（通常放在最右侧）
                
                2. **节点位置布局**:
                   - 从左到右排列，每个节点之间间隔约 300-400 像素
                   - 同一行的节点 y 坐标相同
                   - 不同分支的节点 y 坐标不同，垂直间隔约 200 像素
                   - start 节点: x=0, y=300
                   - end 节点: x=最大x值, y=300
                
                3. **节点ID生成规则**:
                   - 使用简短且唯一的ID，如: "start1", "llm1", "tool1", "end1"
                   - 避免使用特殊字符，只使用字母、数字和下划线
                   %s
                
                 4. **边的连接规则**:
                    - 每个边必须有 id、source（源节点ID）和 target（目标节点ID）
                    - 边的 id 格式: "xy-edge__" + source + sourceHandle + "-" + target（如果有 sourceHandle）
                      - 例如: "xy-edge__hx4wljexecuted-jspgq" 或 "xy-edge__vdx2vn-2y1jis"
                    - Switch类型节点（tool, intent, parameter_extraction, condition）必须提供 sourceHandle:
                      - **tool 节点**: sourceHandle 必须是 "executed" 或 "not_executed"（固定值）
                      - **intent 节点**: sourceHandle 必须是该 intent 节点配置中 intents 数组里某个 intent 的 id（动态值）
                        - 例如：如果 intent 节点配置了 {"id": "i1764404624506", "label": "咨询部分退款"}，则 sourceHandle 应该是 "i1764404624506"
                      - **parameter_extraction 节点**: sourceHandle 必须是 "success" 或 "incomplete"（固定值）
                      - **condition 节点**: sourceHandle 必须是该 condition 节点配置中 conditions 数组里某个 condition 的 id（动态值），或者 "else"（默认分支）
                    - 普通节点（非 Switch）的边不需要 sourceHandle，设置为 null
                    - 边的完整格式必须包含以下字段:
                      {
                        "id": "边ID（必需）",
                        "type": "custom",
                        "animated": true,
                        "style": {"stroke": "#94a3b8"},
                        "deletable": true,
                        "source": "源节点ID（必需）",
                        "target": "目标节点ID（必需）",
                        "sourceHandle": "源句柄（Switch节点必需，普通节点为null）",
                        "targetHandle": null,
                        "label": null
                      }
                
                5. **工作流设计建议**:
                   - 根据用户需求选择合适的节点类型
                   - 合理使用 Switch 节点进行分支处理
                   - 使用 tool 节点调用可用工具
                   - 使用 llm 节点进行智能对话
                   - 使用 intent 节点进行意图识别和路由
                   - 使用 condition 节点进行条件判断（如：根据变量值判断走哪个分支）
                   - 使用 parameter_extraction 节点提取用户关键信息
                   - 使用 variable 节点进行变量的读取和设置
                   - 使用 translation 节点处理多语言场景
                   - 使用 api 节点调用外部接口（替代简单的 LLM+Tool 组合）
                   - 使用 setSessionMetadata 节点保存会话数据
                   - 使用 knowledge 节点查询知识库
                
                6. **LLM 节点与工具调用**:
                   - LLM 节点如果配置了工具（tools 数组），可以调用工具
                   - **重要**：LLM 节点调用工具后，工具返回的数据当前 LLM 节点不会自动处理
                   - **重要**：带参数的工具需要放到LLM节点中这个节点会判断是否需要调用工具或者询问用户缺少的参数，单纯使用tool节点只适合无参工具
                   - **重要**：如果上一个节点是有配置工具调用节点，一般来说需要在它下一个节点添加一个LLM节点来处理工具返回的数据而不是直接连到END节点
                   - 如果需要处理工具返回的数据，应该：
                     1. 接受工具调用的节点输入的数据需要判断是不是返回数据还是询问用户缺少的参数,或者普通输出
                     2. 在新 LLM 节点的 systemPrompt 中使用占位符 {{sys.lastoutput}} 来获取工具返回的数据
                     3. 例如：systemPrompt = "请根据以下工具返回的数据回答问题：\n{{sys.lastoutput}}"
                   - 这样可以实现：工具调用 -> 处理工具返回 -> 生成最终回复的流程
                
                7. **环境变量占位符**:
                   - 系统提供了两个环境变量占位符，可以在 System Prompt 和 Conversation History 中使用：
                     - **{{sys.lastoutput}}**: 获取上一个节点的输出内容
                       - 用于获取工具节点、LLM 节点等前一个节点的输出
                       - 常用于处理工具返回数据、链式调用等场景
                     - **{{sys.query}}**: 获取当前用户输入的消息
                       - 用于在 System Prompt 中引用用户的问题或输入
                       - 例如：systemPrompt = "用户的问题是：{{sys.query}}，请根据上下文回答"
                   - 使用示例：
                     - LLM 节点的 systemPrompt: "请根据工具返回的数据 {{sys.lastoutput}} 和用户问题 {{sys.query}} 生成回复"
                     - Conversation History 中也可以使用这些占位符
                
                8. **简化回复流程**:
                   - 如果只需要直接回复用户，可以直接将节点连接到 END 节点
                   - END 节点会自动获取上一个节点的输出并发送给用户
                   - 这样比使用 reply 节点更简洁，无需额外配置
                   - 例如：LLM 节点 -> END 节点，或 tool 节点 -> LLM 节点 -> END 节点
                
                ## 输出格式要求
                
                请严格按照以下JSON格式输出工作流：
                
                {
                  "nodes": [
                    {
                      "id": "节点ID",
                      "type": "节点类型",
                      "position": {"x": 数字, "y": 数字},
                      "data": {
                        "label": "节点显示名称",
                        "config": {
                          // 节点配置对象
                        }
                      }
                    }
                  ],
                  "edges": [
                    {
                      "id": "边ID（格式：xy-edge__source+sourceHandle-target）",
                      "type": "custom",
                      "animated": true,
                      "style": {"stroke": "#94a3b8"},
                      "deletable": true,
                      "source": "源节点ID",
                      "target": "目标节点ID",
                      "sourceHandle": "源句柄（tool节点：executed/not_executed，intent节点：intent的id，普通节点：null）",
                      "targetHandle": null,
                      "label": null
                    }
                  ]
                }
                
                请根据用户需求，设计一个完整、合理的工作流。
                """, hasExistingWorkflow ? 
                "- **修改现有工作流时**：请保留现有节点的ID，只更新配置或添加新节点" : 
                "");
    }

    /**
     * 使用LLM生成工作流
     */
    private String generateWorkflowWithLLM(String systemPrompt, String userPrompt, LlmModel model) {
        try {
            // 构建用户提示，强调输出格式
            String enhancedPrompt = userPrompt + "\n\n请严格按照JSON格式输出，只返回JSON对象，不要包含任何其他文字说明。";

            // 直接调用LLM，要求返回JSON格式
            // 工作流生成可能需要较长时间，设置更长的超时（300秒=5分钟）
            var response = langChainChatService.chat(
                    model.getId(),
                    systemPrompt,
                    enhancedPrompt,
                    Collections.emptyList(), // 无历史记录
                    1.0, // 温度
                    null, // 不限制最大token
                    300 // 超时时间：300秒（5分钟）
            );

            if (response == null || !response.success() || response.reply() == null || response.reply().trim().isEmpty()) {
                throw new RuntimeException("LLM返回为空或失败: " + (response != null ? response.errorMessage() : "null"));
            }

            // 尝试提取JSON（可能包含markdown代码块）
            String jsonStr = response.reply().trim();
            
            // 如果包含markdown代码块，提取JSON部分
            if (jsonStr.startsWith("```json")) {
                int start = jsonStr.indexOf("{");
                int end = jsonStr.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    jsonStr = jsonStr.substring(start, end + 1);
                }
            } else if (jsonStr.startsWith("```")) {
                int start = jsonStr.indexOf("{");
                int end = jsonStr.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    jsonStr = jsonStr.substring(start, end + 1);
                }
            }

            // 验证JSON格式
            try {
                objectMapper.readTree(jsonStr);
            } catch (Exception e) {
                log.error("LLM返回的不是有效JSON: {}", jsonStr);
                throw new RuntimeException("LLM返回的不是有效JSON格式", e);
            }

            return jsonStr;

        } catch (Exception e) {
            log.error("LLM生成工作流异常", e);
            throw new RuntimeException("LLM生成工作流失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修复节点（确保ID唯一，位置合理）
     */
    private JsonNode fixNode(JsonNode node) {
        ObjectNode fixed = objectMapper.createObjectNode();
        
        // 确保ID存在且唯一
        String id = node.has("id") ? node.get("id").asText() : generateNodeId();
        fixed.put("id", id);
        
        // 确保type存在
        String type = node.has("type") ? node.get("type").asText() : "llm";
        fixed.put("type", type);
        
        // 修复位置
        ObjectNode position = objectMapper.createObjectNode();
        if (node.has("position")) {
            JsonNode pos = node.get("position");
            position.put("x", pos.has("x") ? pos.get("x").asDouble() : 0);
            position.put("y", pos.has("y") ? pos.get("y").asDouble() : 300);
        } else {
            position.put("x", 0);
            position.put("y", 300);
        }
        fixed.set("position", position);
        
        // 修复data
        ObjectNode data = objectMapper.createObjectNode();
        if (node.has("data")) {
            JsonNode dataNode = node.get("data");
            data.put("label", dataNode.has("label") ? dataNode.get("label").asText() : type);
            if (dataNode.has("config")) {
                data.set("config", dataNode.get("config"));
            }
        } else {
            data.put("label", type);
        }
        fixed.set("data", data);
        
        return fixed;
    }

    /**
     * 修复边（确保引用有效的节点，格式正确）
     */
    private JsonNode fixEdge(JsonNode edge, List<JsonNode> nodes) {
        if (!edge.has("source") || !edge.has("target")) {
            return null; // 缺少必要字段，跳过
        }

        String source = edge.get("source").asText();
        String target = edge.get("target").asText();

        // 检查源节点和目标节点是否存在
        JsonNode sourceNode = nodes.stream()
                .filter(n -> n.get("id").asText().equals(source))
                .findFirst()
                .orElse(null);
        boolean targetExists = nodes.stream().anyMatch(n -> n.get("id").asText().equals(target));

        if (sourceNode == null || !targetExists) {
            log.warn("边引用了不存在的节点: source={}, target={}", source, target);
            return null; // 引用无效节点，跳过
        }

        // 获取源节点类型
        String sourceType = sourceNode.has("type") ? sourceNode.get("type").asText() : "";
        
        // 确定 sourceHandle
        String sourceHandle = null;
        if (edge.has("sourceHandle") && !edge.get("sourceHandle").isNull()) {
            sourceHandle = edge.get("sourceHandle").asText();
        } else {
            // 如果是 Switch 节点但没有提供 sourceHandle，尝试从节点配置中获取
            if ("tool".equals(sourceType)) {
                // tool 节点默认使用 executed（如果未指定）
                sourceHandle = "executed";
            } else if ("intent".equals(sourceType)) {
                // intent 节点需要从配置中获取第一个 intent 的 id
                sourceHandle = getFirstIntentId(sourceNode);
            } else if ("parameter_extraction".equals(sourceType)) {
                sourceHandle = "success";
            } else if ("condition".equals(sourceType)) {
                sourceHandle = "else";
            }
        }

        // 生成边的 ID
        String edgeId;
        if (edge.has("id") && !edge.get("id").isNull()) {
            edgeId = edge.get("id").asText();
        } else {
            // 生成标准格式的 ID: xy-edge__source+sourceHandle-target
            if (sourceHandle != null && !sourceHandle.isEmpty()) {
                edgeId = "xy-edge__" + source + sourceHandle + "-" + target;
            } else {
                edgeId = "xy-edge__" + source + "-" + target;
            }
        }

        // 构建完整的边对象
        ObjectNode fixed = objectMapper.createObjectNode();
        fixed.put("id", edgeId);
        fixed.put("type", "custom");
        fixed.put("animated", true);
        ObjectNode style = objectMapper.createObjectNode();
        style.put("stroke", "#94a3b8");
        fixed.set("style", style);
        fixed.put("deletable", true);
        fixed.put("source", source);
        fixed.put("target", target);
        
        // sourceHandle: Switch 节点必须有值，普通节点为 null
        if (sourceHandle != null && !sourceHandle.isEmpty()) {
            fixed.put("sourceHandle", sourceHandle);
        } else {
            fixed.putNull("sourceHandle");
        }
        
        fixed.putNull("targetHandle");
        fixed.putNull("label");

        return fixed;
    }
    
    /**
     * 从 intent 节点配置中获取第一个 intent 的 id
     */
    private String getFirstIntentId(JsonNode intentNode) {
        try {
            if (intentNode.has("data") && intentNode.get("data").has("config")) {
                JsonNode config = intentNode.get("data").get("config");
                if (config.has("intents") && config.get("intents").isArray()) {
                    JsonNode intents = config.get("intents");
                    if (intents.size() > 0 && intents.get(0).has("id")) {
                        return intents.get(0).get("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取 intent 节点配置失败", e);
        }
        return null;
    }

    /**
     * 生成节点ID
     */
    private String generateNodeId() {
        return "node_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成边ID
     */
    private String generateEdgeId() {
        return "edge_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成唯一的节点ID（避免与现有ID冲突）
     */
    private String generateUniqueNodeId(Set<String> existingIds) {
        String baseId = "node_" + UUID.randomUUID().toString().substring(0, 8);
        String uniqueId = baseId;
        int counter = 1;
        while (existingIds.contains(uniqueId)) {
            uniqueId = baseId + "_" + counter;
            counter++;
        }
        return uniqueId;
    }

    /**
     * 生成的工作流结果
     */
    public record GeneratedWorkflow(
            String nodesJson,
            String edgesJson,
            String fullJson
    ) {}
}

