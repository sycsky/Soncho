# ========================================
# 阶段1: 构建阶段
# ========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# ========================================
# 阶段2: 运行阶段
# ========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装必要工具（可选）
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 创建非 root 用户运行应用
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM 优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

