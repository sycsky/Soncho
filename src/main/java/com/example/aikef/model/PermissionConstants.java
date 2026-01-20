package com.example.aikef.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限常量定义
 */
public class PermissionConstants {
    
    // 所有权限键
    public static final String SET_READ_INFO = "setReadInfo";
    public static final String SET_CANCELLATION_POLICY = "setCancellationPolicy";
    public static final String MANAGE_KNOWLEDGE_BASE_SETTING = "manageKnowledgeBaseSetting";
    public static final String ACCESS_CUSTOMER_MANAGEMENT = "accessCustomerManagement";
    public static final String MANAGE_TEAM = "manageTeam";
    public static final String ACCESS_SYSTEM_STATISTICS = "accessSystemStatistics";
    public static final String DESIGN_WORKFLOW = "designWorkflow";
    public static final String ACCESS_ROLE_CONFIG = "accessRoleConfig";
    public static final String ACCESS_AI_TOOLS = "accessAiTools";
    public static final String ACCESS_BILLING = "accessBilling";
    
    /**
     * 获取所有权限键
     */
    public static String[] getAllPermissions() {
        return new String[]{
            SET_READ_INFO,
            SET_CANCELLATION_POLICY,
            MANAGE_KNOWLEDGE_BASE_SETTING,
            ACCESS_CUSTOMER_MANAGEMENT,
            MANAGE_TEAM,
            ACCESS_SYSTEM_STATISTICS,
            DESIGN_WORKFLOW,
            ACCESS_ROLE_CONFIG,
            ACCESS_AI_TOOLS,
            ACCESS_BILLING
        };
    }
    
    /**
     * 创建包含所有权限的权限Map（所有权限都设置为true）
     */
    public static Map<String, Object> createAllPermissionsMap() {
        Map<String, Object> permissions = new HashMap<>();
        for (String permission : getAllPermissions()) {
            permissions.put(permission, true);
        }
        return permissions;
    }
}


