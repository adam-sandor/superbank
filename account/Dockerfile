FROM maven:3-adoptopenjdk-16
WORKDIR /build
COPY . /build
RUN mvn package

FROM adoptopenjdk:16
WORKDIR /app
COPY --from=0 /build/target/account.jar /app
CMD ["java", "-jar", "account.jar"]