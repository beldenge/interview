FROM amazoncorretto:25 AS build

WORKDIR /workspace

RUN dnf install -y maven \
    && dnf clean all

COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests package

FROM amazoncorretto:25

WORKDIR /app

COPY --from=build /workspace/target/interview-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
