# Создать jar-файл в папке target
echo ""
echo "***** CREATE JAR *****"
mvn clean package

# Сбилдить образ при помощи Dockerfile
echo ""
echo "***** BUILD IMAGE *****"
docker build -t alexz2/cats-api:1.0.0 . -f ./Dockerfile

# Запушить образ на Docker Hub
echo ""
echo "***** PUSH TO DOCKER HUB *****"
docker push alexz2/cats-api:1.0.0
