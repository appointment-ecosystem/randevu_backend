# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Maven Wrapper ve pom.xml kopyalanır
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Bağımlılıkları önceden indirmek için offline modda derleme hazırlığı yapılır (layer caching)
RUN ./mvnw dependency:go-offline -B

# Kaynak kod kopyalanır ve paketlenir (testler geçici olarak atlanır)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Uygulama kullanıcısı oluşturulur (Root olmayan güvenli modda çalıştırmak için)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Paketlenen jar dosyası build aşamasından kopyalanır
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# HTTP portu dışa aktarılır
EXPOSE 8080

# Uygulama dev profilinde başlatılır (istenirse runtime'da SPRING_PROFILES_ACTIVE ile ezilebilir)
ENTRYPOINT ["java", "-jar", "app.jar"]
