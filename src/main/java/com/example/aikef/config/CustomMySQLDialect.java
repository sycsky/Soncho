package com.example.aikef.config;

import org.hibernate.dialect.MySQLDialect;

public class CustomMySQLDialect extends MySQLDialect {
    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci";
    }
}
