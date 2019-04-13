FROM java:8-alpine
ADD target/device-simulator.jar /device-simulator.jar
ADD dist/ssl /ssl
ADD scripts /scripts
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java","-jar","-server","-XX:+UseG1GC","/device-simulator.jar"]