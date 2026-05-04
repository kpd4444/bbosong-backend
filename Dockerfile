FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --chown=app:app build/libs/*.jar app.jar

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
