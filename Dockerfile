FROM maven:3.10.0-eclipse-temurin-21
WORKDIR /work

# 1. dependencies first: this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2. then the code, which changes often
COPY src ./src
COPY testng-*.xml ./
RUN mvn -B -q test-compile

ENV REPORTS_DIR=/work/reports
CMD ["mvn", "-B", "test"]