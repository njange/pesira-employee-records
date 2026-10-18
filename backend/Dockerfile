FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY --chmod=755 mvnw ./
COPY pom.xml ./
# Dependencies get their own layer, so changing only the code skips the download.
RUN ./mvnw -q dependency:go-offline
COPY src src
# The tests start PostgreSQL with Testcontainers, which needs Docker; run them outside the image build.
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:25-jre
RUN useradd --system app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
