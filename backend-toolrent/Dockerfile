# ============================================
# DOCKERFILE PARA BACKEND TOOLRENT
# ============================================
# Multi-stage build para optimizar tamaño de imagen

# ====== STAGE 1: BUILD ======
FROM eclipse-temurin:17-jdk-alpine AS build

LABEL maintainer="ToolRent Team"
LABEL description="Backend ToolRent - Build Stage"

WORKDIR /app

# Copiar archivos de Maven wrapper
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .

# Dar permisos de ejecución al wrapper
RUN chmod +x mvnw

# Descargar dependencias (esta capa se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src ./src

# Compilar y empaquetar la aplicación (sin ejecutar tests)
RUN ./mvnw clean package -DskipTests

# ====== STAGE 2: RUNTIME ======
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="ToolRent Team"
LABEL description="Backend ToolRent - Runtime"

WORKDIR /app

# Copiar el JAR compilado desde la etapa de build
COPY --from=build /app/target/toolrent-backend.jar app.jar

# Exponer puerto 8090
EXPOSE 8090

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_HOST=mysql
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
