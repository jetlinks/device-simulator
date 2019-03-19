FROM java:8-alpine
ADD target/device-simulator.jar /device-simulator.jar
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java","-jar","-server","-XX:+UseG1GC","/device-simulator.jar"]