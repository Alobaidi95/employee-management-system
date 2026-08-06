 # ---- Stage 1: Build ----
 # Full JDK + Maven, used only to compile the app. Thrown away after this stage.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

 # Copy just the pom.xml first (not the source code yet) and download
 # dependencies. Docker caches each instruction as a "layer" - as long as
 # pom.xml doesn't change, this layer is reused on future builds instead of
 # re-downloading every dependency every time you change a .java file.
COPY pom.xml .
RUN mvn dependency:go-offline -B

 # Now copy the actual source and build the jar.
 # -DskipTests: this Dockerfile builds the app, it doesn't run the test
 # suite - that's CI's job (your GitHub Actions workflow already does that
 # on every push). Running tests again here just slows down every build.
COPY src ./src
RUN mvn clean package -DskipTests -B

 # ---- Stage 2: Runtime ----
 # Minimal JRE only - no Maven, no JDK, no source code, none of Stage 1
 # ends up here except the one file we explicitly copy below.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

 # Copy ONLY the built jar from the build stage (referenced by name "build")
COPY --from=build /app/target/*.jar app.jar

 # Documents that the container listens on 8080 - doesn't actually publish
 # the port by itself (that happens with `docker run -p`), it's metadata.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]