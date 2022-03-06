Пишем Spring Boot микросервис для деплоя в Kubernetes с нуля!

Напишем REST-сервис с двумя методами:
1) сохранить котика
2) показать всех котиков

Развернём Kubernetes через инструмент kind(?), и развернем наш сервис в кубере.

1. Сначала пишу свой кошачий сервис.

2. БД Postgres в контейнере Докер. Настройки контейнера в файле docker-compose.yaml в корне проекта.

3. Приложение:
http://localhost:8081/

4. Документация апи:
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>1.6.6</version>
        </dependency>
        
        Адрес: http://localhost:8081/swagger-ui/index.html

5. Далее @Accessors(chain = true)

## Теперь попробуем приложение обернуть в Докер (время 44:10)

1. Создаём Dockerfile в корне.

FROM alpine:3.13
alpine - это минималистичный линукс, в котором нет ничего лишнего
и он максимально мало весит.

RUN apk add openjdk11
На stackoverflow можно увидеть эту команду (загуглил "alpine install jdk11").

Далее надо положить джарник в докер-имейдж
COPY target/cats-api-0.0.1-SNAPSHOT.jar /app.jar
Но сначала джарник надо создать. Выполняю команду: mvn clean package

Далее команды будем выполнять не в консоли, а через скрипт build_and_push.sh
Это сэкономит время, т.к. не надо будет постоянно вводить последовательность одних
и тех же команд руками.

Продолжаем:
COPY target/cats-api-0.0.1-SNAPSHOT.jar /app.jar
Говорим откуда с нашего и хоста, и куда положить на виртуалку (в корень и назвать app.jar).

Теперь укажем с чего должна запуститься виртуальная машина:
ENTRYPOINT ["java", "-jar", "/app.jar"]

Чтобы это всё сбилдить указываем в файле .sh команду
docker build . -t alexz2/cats-api:1.0.0
. значит что будем искать Докерфайл в текущей директории
Через -t указываем имя и версию.

Еще через консоль залогинился на Докер Хабе:
`docker login`
Username: alexz2
Password: <пароль>

Теперь попробуем всё сбилдить: пишу в консоли:  
`chmod +x build_and_push.sh` это чтобы можно было запускать скрипт через консоль. Т.е. сделали
наш файл экзекютабле.

Теперь запускаем:  `./build_and_push.sh`
- собрался джарник
- собрался докер имейдж

Теперь пробуем запустить наш образ: в консоли пишу:
docker run -it --rm alexz2/cats-api:1.0.0
получаем ошибку `org.postgresql.util.PSQLException: Connection to localhost:15432 refused` т.к. приложение 
пытаемся запустить в контейнере, а там нет БД.

Укажем в application.yaml

                  #        url: jdbc:postgresql://localhost:15432/cats_db
                  url: jdbc:postgresql://${DATASOURCE_HOST:localhost}:15432/cats_db

когда мы работаем с Докером, то мы указывваем конфигурации в большинстве случаев
через переменные окружения. На Спринге это делается просто, мы можем прописать переменную окружения DATASOURCE_HOST
и тогда у нас подтянутся наши настройки. Мы можем извне указать хост для БД. В реальной жизни мы также
будем прокидывать логин/пароль, порт и название БД.

Далее удалим образ `docker image rm alexz2/cats-api:1.0.0 `

И опять запустим скрипт .sh

Теперь надо узнать айпишник нашего хоста из контейнера:
`ifconfig`

      enp4s0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
              inet 192.168.1.35  netmask 255.255.255.0  broadcast 192.168.1.255
              inet6 fe80::aef6:2d6f:e51d:8029  prefixlen 64  scopeid 0x20<link>
              ether b4:2e:99:a1:a5:7f  txqueuelen 1000  (Ethernet)
              RX packets 848031  bytes 1163124216 (1.1 GB)
              RX errors 0  dropped 113  overruns 0  frame 0
              TX packets 369179  bytes 34315008 (34.3 MB)
              TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
              device memory 0xfca00000-fca1ffff  

Набираем команду и чуть модернизируем (-e DATASOURCE_HOST=192.168.1.35):
`docker run -it --rm -e DATASOURCE_HOST=192.168.1.35 alexz2/cats-api:1.0.0`

Теперь видим, что всё запустилось.
Даже больше: мы можем прокинуть порт на хост:
-p 8082:8081
`docker run -it --rm -e DATASOURCE_HOST=192.168.1.35 -p 8082:8081 alexz2/cats-api:1.0.0`

Далее в браузере:
http://localhost:8082/api/v1/cat
и вижу что всё работает.

Теперь запушим. Сделаем докер пуш (пропишем в .sh файл)
docker push alexz2/cats-api:1.0.0

И опять в терминале выполним команду:
./build_and_push.sh

Теперь вижу что у меня на Докер Хаб появился новый образ `alexz2/cats-api`

2. Далее мы запустим наш докер имейдж в kind-е.
Идем по адресу 
https://kind.sigs.k8s.io/

Kind - то эмулятор кубернетеса.
Тут надо посмотреть как его установить:

curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.11.1/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

kind version  => kind v0.11.1 go1.16.4 linux/amd64

3. Сначала нарисуем, что мы собираемся сделать:

Кубернетес кластер - это набор машин, который обычно состоит из 1 или нескольких **мастеров**,
и **воркеры** (их много). **Мастер-машины** определяют на каких воркерах должно работать
наше приложение, а **воркеры** это уже непосредственно те машины, на которых запускается приложение.

Например хотим запустить наше приложение Cats API в 3 экземплярах.
Запущенное в кубернетес приложение называется **pod**.
cat pod 1 - cp1
cat pod 2 - cp2
cat pod 3 - cp3

В итоге у нас 3 воркера. И каждое приложение на отдельном воркере запустится.

Допустим у нас внутри кубернетес кластера есть еще собачью сервисы:
dog pod 1 - dp1

В итоге у нас есть кластер. В нём 2 приложения:
катс в 3 экземплярах, и догс в 1 экземпляре.

И вот дог хочет сделать запрос на кэтса. Но он не хочет думаьб, в скольки экземплярах
запущен наш кошачий сервис, на каких нодах он запущен, как там распределена нагрузка и т.д..

Специально для этого в кубернетес есть слой который называется **сервис**.
Сервис он как раз распределяет нагрузку между нашими подами.

И если какое то приложение внутри нашего кластера захочет обратиться к кэтс, то оно
может пойти на сервис, а сервис уже сам определит куда обращаться.

Помимо этого у собак может быть свой сервис.

А тепепрь что будет, если придет человек и скажет, не могли бы вы предоставить мне
ответ на мой запрос к кэт сервису? Т.е. прилетит некий запрос из вне. Но чтобы обращаться извне
к кэт сервису, то его надо специальным образом настроить. И для того чтобы сделать 
правильное обращение к нашему кэт сервису извне, у кубернетеса есть специальный плагин
который называется ingres plugin
У него есть разные реализации. Мы возьмём реализацию от nginx-а. А вот уже ingres может
перенаправлять запросы на сервисы. Он чем то похож на сервисы, но если сервисы созданы чтобы распределять
нагрузку, и также используются в качестве сервис-дискавери, т.е. они знают о том где были запущены
наши поды; то ингрес он больше нужен для того чтобы разроутить вот эту вот точку входа, когда
кто-то сторонний заходит в наш кластер, и хочет сделать какой-то запрос. И в итоге клиенту нашего приложения
не надо думать о том, какие там есть сервисы, сколько там подов и т.д.. Клиент просто знает что по урлу
/api/v1/cats  он получит одну информацию, а по другому урлу он получит другую информацию. И ему ничего
не надо знать о внутреннем механизме кубера.

Сейчас реализуем часть, связанную с кошачьим сервисом.

Зачем всё это нужно? Почему кубернетес так удобен? Когда мы всё это поднимем, то кубер сам будет
определять на какой машине всё это запустить. Сам организует нам сервис, и т.д.
Если бы не было кубера, то нам бы самим пришлось деплоить каждый раз на какой-то сервис,
думать насчёт сервис-дискавери, ну и т.д.. Например мы работали в 3 инстансах, а потом решили что в 5 будет лучше.
Для кубера это ещё одна простая команда. Без кубера нам пришлось бы долго возиться с этим.

4. Теперь осуществим всё это на практике (01:08:00).

Сначала установим такую утилиту kubectl (это клиент кубернетеса):
curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.23.0/bin/linux/amd64/kubectl
chmod +x ./kubectl

Переместите двоичный файл в директорию из переменной окружения PATH:
sudo mv ./kubectl /usr/local/bin/kubectl

kubectl version --client

Теперь установим ingres plugin
https://kind.sigs.k8s.io/docs/user/ingress/
Как запустить kind чтобы на него можно было потом легко установить ingres plugin
По данному урлу видим пример конфига.
Создаем конфиг kind-config.yaml

Мы видим что в нашем kind-конфиге кластер:
kind: Cluster

версия:
apiVersion: kind.x-k8s.io/v1alpha4

Далее смотрим те ноды на которых будет запущено наше приложение:
nodes:

Нода control-plane это как раз **мастер-нода**.

Далее конфигурация для kind-а:

    kubeadmConfigPatches:
                - |
                    kind: InitConfiguration
                    nodeRegistration:
                      kubeletExtraArgs:
                        node-labels: "ingress-ready=true"
                    
чтобы всё получилось с ingres-ом.

Далее мы на мастер-ноде прокидываем некие порты чтобы как раз была такая
единая точка входа:

     extraPortMappings:
            -   containerPort: 80
                hostPort: 8888
                protocol: TCP
            
т.е. на мастере будет открыт какой-то порт, на который мы
будем делать запросы.

kind delete cluster => Deleting cluster "kind" ...
на всякий случай. Вдруг уже был создан ранее какой-то kind-кластер.

Еще сделаем 
    -   role: worker
    -   role: worker
    -   role: worker
т.е. будет 1 мастер и 3 воркера.

kind create cluster --config kind-config.yaml
В итоге кластер создался (сюда скриншот).

kind готов, теперь хочу Ingress NGINX плагин
https://kind.sigs.k8s.io/docs/user/ingress/
команда которая ставит этот плагин:
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

Далее команда
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
убедимся что всё ок => pod/ingress-nginx-controller-59cbb6ccb6-7xkgr condition met (ingress поставился)

Далее начнём с нижнего уровня, который связан с подами. Посмотрим как сделать конфиг, чтобы кубернетес-кластер нас
понял и запустил наше приложение в 3 экземплярах.

Гуглим "kubernetes deployment example"
Идём сюда: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/ и тут копируем пример конфига.
Deployment - это как раз та сущность кубернетеса, которая отвечает за поды. И описав деплоймент мы можем
описать какие поды в скольки экземплярах нам нужны.

Создадим папку k8s и в неё будем складывать все конфиги связанные 
с кубернетесом. Сперва создадим deployment.yaml и вставляем сюда пример конфига.
Видим что эта сущность Деплоймент:
kind: Deployment

Далее
kubectl apply -f k8s/deployment.yaml
=> deployment.apps/cats-api-deployment created

kubectl get pods
посмотреть запущенные поды

Чтобы мониторить (отслеживать в реальном времени) нужен флаг --watch
kubectl get pods --watch

это был самый нижний уровень (поды).

Далее перейдём к сервису.
гуглим "kubernetes service example"
https://kubernetes.io/docs/concepts/services-networking/service/

скопипастим пример и поменяем чуть чуть. Создаём в k8s service.yaml и вставляем туда и поменяем теперь.

Видим что контейнер запустился и упал с ошибкой.
Посмотрим ошибку:
kubectl logs cats-api-deployment-58b69bb468-k88sq

Вижу ошибку "Caused by: org.postgresql.util.PSQLException: Connection to localhost:15432 refused. Check that
the hostname and port are correct and that the postmaster is accepting TCP/IP connections."


надо добавить environment переменную
опять иду в deployment.yaml
добавляем блок env
                    env:
                        -   name: DATASOURCE_HOST
                            value: 192.168.1.35
                            
Далее опять
kubectl apply -f k8s/deployment.yaml
kubectl get pods --watch
они пытаются рестартануться

Остановим их:
kubectl delete pods --all
=> все поды удалены

Не неверно. Надо было удалить не поды а Деплоймент. Потому что деплоймент следит
за тем, чтобы было 3 пода. И если это нарушается то он пытается восстановить это.

Пишу
kubectl delete deployments --all
=> deployment.apps "cats-api-deployment" deleted

Далее
kubectl apply -f k8s/deployment.yaml
=> deployment.apps/cats-api-deployment created

Потом
kubectl get pods --watch

Теперь смотрю логи:
kubectl logs cats-api-deployment-869476485d-6ngzx
=> видно что всё запустилось и всё ок.

Мы можем больше: мы можем пробросить порт
kubectl port-forward cats-api-deployment-869476485d-6ngzx 8899:8081  (порт на машине и порт внутри контейнера)

теперь в браузере открываю http://localhost:8899/api/v1/cat
и вижу в браузере наших котов.

Т.е. контейнер запустился , всё отлично.

Теперь ещё убедимся что их 3 контейнера:
kubectl get pods

Т.е. все наши 3 пода работают.

### Пойдём дальше и применим наш сервис, который уже написали

kubectl apply -f k8s/service.yaml
=> service/cats-api-service created

kubectl get service
вижу свой сервис cats-api-service 

           NAME               TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)   AGE
           cats-api-service   ClusterIP   10.96.112.31   <none>        80/TCP    94s
           kubernetes         ClusterIP   10.96.0.1      <none>        443/TCP   95m


Тепреь сделаем конфиг с ингрессом:
посмотрим тут https://kind.sigs.k8s.io/docs/user/ingress/
скопипастим и создадим файл ingress.yaml

Это будет единая точка входа в наш кластер. И потенциально это могут быть не только коты. Назову
name: my-ingress

Теперь применим этот конфиг:
kubectl apply -f k8s/ingress.yaml
=> ingress.networking.k8s.io/my-ingress created

теперь посмотрим в файл kind-config.yaml
hostPort: 8888
теперь пойдем на 8888 и посмотрим кто нас ждёт на cats-api
Идём http://localhost:8888/cats-api/api/v1/cat
Видим

           Whitelabel Error Page
           This application has no explicit mapping for /error, so you are seeing this as a fallback.
           
           Sun Mar 06 10:53:20 GMT 2022
           There was an unexpected error (type=Not Found, status=404).

404 потому что к путю теперь добавлен cats-api
Как это быстро пофиксить? Идем в deployment.yaml и скажем нашему приложению чтобы оно слушало нас
начиная с cats-api. Для этого добавим ещё одну переменную окружения, которая
называется spring.mvc.servlet.path
т.е с этого пути должно стартовать наше приложение.

                        -   name: spring.mvc.servlet.path
                            value: /cats-api

Обновим  наши поды 
kubectl apply -f k8s/deployment.yaml
=> deployment.apps/cats-api-deployment configured

Проверим что с подами всё хорошо:
kubectl get pods
видим что старые поды удалены, новые поднялись (имена другие)

Тепепь опять открываем
http://localhost:8888/cats-api/api/v1/cat

и видим список котов
[{"id":"bfe41dd1-38ba-4d29-9549-c199cb5b9300","name":"Murzik fon Cat","birthDate":"2020-04-15","createdAt":"2022-03-06T11:41:53.678806"},{"id":"465019f6-bdde-4936-a052-23b5bcfe5821","name":"Barsik van der Dog","birthDate":"2020-05-21","createdAt":"2022-03-06T11:42:39.356039"}]

Т.е мы в итоге проходим полный путь:
- человечек пришёл и сделал такой запрос /cats-api/api/v1/cat
- и у него нагрузка балансируется, и всё работает
- я как клиент даже не знаю, в какой из 3 подов отправился наш запрос.
- очень удобно так организовывать масштабирование.


#### Попробуем с этим немножко поиграться.

Например в deployment.yaml поменяем 
replicas: 10
т.е. 10 инстансов.

Если бы мы это делали без кубернетеса то это было бы очень тяжело.

Говорим применить
kubectl apply -f k8s/deployment.yaml
=> deployment.apps/cats-api-deployment configured

Теперь посмотрим
kubectl get pods
вижу все 10 штук

далее иду
http://localhost:8888/cats-api/api/v1/cat

и вижу что всё работает.

### Что я использовал:
- [Пишем Spring Boot микросервис для деплоя в Kubernetes](https://www.youtube.com/watch?v=KPLJ0i5Ocws)
