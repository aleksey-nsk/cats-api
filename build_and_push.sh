mvn clean package
docker build . -t alexz2/cats-api:1.0.0
docker push alexz2/cats-api:1.0.0
