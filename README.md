# sanctions-java-poc (Java-only POC)
Build:
    mvn clean package
Run (example):
    java -Xms1G -Xmx2G -cp target/sanctions-java-poc-0.1.0.jar com.example.sanctions.server.LookupServer ./data/chronicle.dat src/main/resources/sample_data.csv
Test:
    curl "http://localhost:8080/lookup?name=MOHD%20KHALIQUE"