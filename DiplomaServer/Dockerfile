FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Копируем файлы сборки
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Делаем скрипт сборки исполняемым и собираем приложение
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный jar из этапа сборки
COPY --from=build /app/target/*.jar app.jar

# Определяем переменную для профиля Spring
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]