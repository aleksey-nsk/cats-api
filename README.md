# Задача

- Написать простой микросервис на Spring Boot.
- **Задеплоить микросервис в Kubernetes**.

# Микросервис

- Простой микросервис на Spring Boot. Есть 2 метода: сохранить кота и показать список котов. Также добавил метод,
  возвращающий текущий IP адресс.

- Использована БД **Postgres** в контейнере **Docker**. Настройки контейнера указываем  
  в файле docker/**docker-compose.yaml**. Одну и ту же БД будем использовать при запуске приложения
  локально/в контейнере/в кластере.

- Настройки приложения (порт, логирование, подключение к БД) прописываем  
  в файле src/main/resources/**application.yaml**.

- Для миграций используется **Liquibase**.

- Для документирования REST API используем **Springdoc OpenAPI UI**. Для этого нужно добавить зависимость:  
  ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/03_springdoc.png)

Открываем приложение (список всех котиков) по адресу: http://localhost:8081/api/v1/cat  

Текущий IP адресс: http://localhost:8081/api/v1/ip => **127.0.1.1**  

Документация к API доступна по адресу: http://localhost:8081/swagger-ui/index.html. Тут же можно тестировать конечные
точки (кнопка "**Try it out**"):  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/04_open_doc.png)

# Как поместить микросервис в Docker-контейнер

1. Сначала поместим наш кошачий сервис в Docker-контейнер. Для этого создаём в корне проекта **Dockerfile** с
   содержимым:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/05_dockerfile_3.png)

- `alpine` - a minimal Docker image based on **Alpine Linux** with a complete package index and only 5 MB in size!
  **Alpine Linux** - дистрибутив Linux, ориентированный на безопасность, легковесность и нетребовательность к ресурсам.
  В основном используется во встраиваемых системах, также хорошо подходит для создания Docker-контейнеров.

- `apk add openjdk11` - install jdk11 in Alpine Linux docker image.

- `COPY target/cats-api-0.0.1-SNAPSHOT.jar /app.jar` - скопировать jar-файл в Docker image (предварительно надо
  выполнить в консоли команду **mvn clean package** чтобы создать этот jar-ник в папке **target**). Чтобы скопировать
  файлы надо указать 2 пути:  
  `target/cats-api-0.0.1-SNAPSHOT.jar` - путь до файлов на хосте относительно файла Dockerfile;  
  `/app.jar` - и путь куда положить эти файлы внутри образа (в данном случае в корень,  
  и назвать **app.jar**).

- `ENTRYPOINT ["java", "-jar", "/app.jar"]` - ENTRYPOINT позволяет задавать дефолтные команды и аргументы во время
  запуска контейнера. Она похожа на CMD, но параметры ENTRYPOINT не переопределяются, если контейнер запущен с
  параметрами командной строки. Вместо этого аргументы командной строки, передаваемые `docker run my_image`, добавляются
  к аргументам инструкции ENTRYPOINT. Например, `docker run my_image bash` добавляет аргумент bash в конец, ко всем
  другим аргументам ENTRYPOINT. Docker-файл обязательно должен содержать либо  
  CMD-инструкцию, либо ENTRYPOINT-инструкцию.

2. Чтобы сэкономить время и не билдить каждый раз в консоли руками, создаём в корне скрипт **build_and_push.sh** с
   содержимым:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/06_build_script_2.png)

- `docker build -t alexz2/cats-api:1.0.0 . -f ./Dockerfile` - сбилдить образ
- `docker build` - команда для создания образа
- `-t alexz2/cats-api:1.0.0` - с помощью параметра -t задаём имя образу и версию
- `.` - указываем где искать файлы для команды COPY (в данном случае - в текущей директории)
- `-f ./Dockerfile` - путь до Dockerfile

3. Также надо через консоль залогиниться на **Docker Hub**:

- `docker login`
- Username: `alexz2`
- Password: `<мой_пароль>`

4. Далее сделаем наш скрипт _исполняемым_ (_executable_). Для этого пишем в консоли:  
   `chmod +x build_and_push.sh`

5. И теперь пробуем всё сбилдить и запушить. Запускаем файл командой:  
   `./build_and_push.sh`  
   Что при этом происходит:

- создаётся джарник;
- билдится образ;
- полученный образ пушится на Docker Hub.

В итоге видим свой образ:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/07_1_see_image.png)

Если запустим ещё раз команду `./build_and_push.sh`, то сбилдится новый образ  
и запушится на Docker Hub:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/07_2_build_again.png)

Также видим, что образ загружен на Docker Hub:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/08_docker_hub.png)

6. Теперь попробуем запустить из нашего образа контейнер с приложением. Пишем команду в консоли:  
   `docker run -it --rm alexz2/cats-api:1.0.0`

- `docker run` - запустить контейнер из образа;
- `--rm` - если запустить контейнер с таким ключом, то он будет автоматически удалён сразу же  
  после остановки;
- `alexz2/cats-api:1.0.0` - образ.

Получаем ошибку:  
_"org.postgresql.util.PSQLException: Connection to localhost:15431 refused. Check that the hostname and port are correct
and that the postmaster is accepting TCP/IP connections"_  
т.к. приложение пытается запуститься в контейнере, а там нет БД.

В файле src/main/resources/**application.yaml** укажем:

    # БЫЛО:
    # url: jdbc:postgresql://localhost:15431/cats
     
    # СТАЛО:
    url: jdbc:postgresql://${DATASOURCE_HOST:localhost}:15431/cats

Когда мы работаем с Docker, то в большинстве случаев указываем конфигурации через  
**переменные окружения**. На Spring-е можно прописать **переменную окружения DATASOURCE_HOST**  
и тогда у нас подтянутся наши настройки. В итоге мы можем извне указать хост для БД. В реальной жизни мы также можем
прокидывать логин/пароль, порт и название БД.

Далее удаляем образ командой  
`docker image rm alexz2/cats-api:1.0.0`  
и опять запускаем скрипт **build_and_push.sh**

Теперь надо узнать айпишник нашего хоста (чтобы указать его для контейнера). Пишем в консоли команду `ifconfig` и
получаем:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/09_ip_host.png)

Далее опять пробуем запустить из нашего образа контейнер с приложением. Немного модернизируем команду (добавляем **-e
DATASOURCE_HOST=192.168.1.69**):  
`docker run -it --rm -e DATASOURCE_HOST=192.168.1.69 alexz2/cats-api:1.0.0`  
и видим что теперь всё запустилось без ошибок.

Далее мы можем **прокинуть порт** на хост (**-p 8082:8081**):  
`docker run -it --rm -e DATASOURCE_HOST=192.168.1.69 -p 8082:8081 alexz2/cats-api:1.0.0`  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/10_app_start_in_container.png)

Теперь приложение доступно в браузере по адресу: http://localhost:8082/api/v1/cat  
Текущий IP адресс: http://localhost:8082/api/v1/ip => **172.17.0.3**  
Документация доступна по адресу: http://localhost:8082/swagger-ui/index.html

# Как задеплоить микросервис в Kubernetes (используя kind)

1. Что собираемся делать, схема:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/11_schema.png)

**Кубернетес кластер** - это набор машин, состоящий обычно из 1 **мастера (Master Node)**, и большого количества
**воркеров (Worker Nodes)**. Мастер-машина определяет, на каких воркерах должно работать наше приложение, а воркеры -
это уже непосредственно те машины, на которых запускается приложение.

Например хотим запустить наше приложение _Cats API_ в 3 экземплярах. Значит у нас будет 3 **пода**:

- cats pod 1
- cats pod 2
- cats pod 3

Допустим у нас внутри Кубернетес кластера есть ещё _Собачий API_. И он состоит из 1 пода:

- dogs pod 1

В итоге в нашем Кубернетес кластере находится 2 приложения:

- _Cats API_ в 3 экземплярах
- _Dogs API_ в 1 экземпляре

Допустим dogs хочет сделать запрос на cats. И он при этом не хочет думать о том, в скольких экземплярах запущен наш
кошачий api, на каких **нодах** он запущен, как там распределена нагрузка и т.д.. Специально для этого в Кубернетес есть
слой, который называется **сервис**. Сервис как раз и распределяет нагрузку между нашими подами. И если какое-то
приложение внутри нашего кластера захочет обратиться к cats, то оно может пойти на сервис, а сервис уже сам определит
куда обращаться. Помимо этого у собак может быть свой сервис (**dogs service**).

А теперь что будет, если придёт человек и скажет, не могли бы вы предоставить мне ответ на мой запрос к Cats API, т.е.
прилетит некий запрос извне. Чтобы обращаться извне к cats сервису, его надо специальным образом настроить. И для этого
у Кубернетеса есть **Ingress Controller**. У Ingress-контроллера есть разные реализации: мы возьмём реализацию
от NGINX-а (**NGINX Ingress Controller**). Ingress может перенаправлять запросы на сервисы (**cats service** и **dogs
service**). Он чем то похож на сервисы, но если сервисы созданы чтобы распределять нагрузку, а также используются в
качестве "сервис дискавери" (т.е. они знают о том где запущены наши поды), то Ingress больше нужен для того чтобы
**разроутить** точку входа, когда кто-то сторонний заходит в наш кластер и хочет сделать какой-то запрос. И в итоге
клиенту нашего приложения не надо думать о том, какие там есть сервисы, сколько там подов и т.д.. Клиент просто знает
что по одному урлу он получит одну информацию, а по другому урлу он получит другую информацию. И ему ничего не надо
знать о внутреннем механизме Кубернетеса.

Схема 1 Кубернетес кластера с Ingress Controller-ом:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_8_pic1.png)  

Схема 2:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_8_pic2.png)  

Схема 3:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_8_pic3.png)  

Схема 4:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_8_pic4.png)  

Зачем всё это нужно? Почему Кубернетес так удобен? Когда мы всё это поднимем, то Кубер сам будет определять, на какой
машине всё это запускать. Если бы не было Кубера, то нам бы самим пришлось деплоить
каждый раз, думать насчёт сервис дискавери, и т.д.. Например мы работали в 3 **инстансах**, а потом
решили что в 10 будет лучше: для Кубера это ещё одна простая команда. Без кубера нам пришлось бы долго возиться с этим.

2. Теперь реализуем часть, связанную с Cats API. Сначала установим **kind**.

**Kind** — это инструмент для запуска локальных кластеров Kubernetes с помощью "узлов"  
контейнера Docker. **Kind** - это эмулятор Кубернетеса.

**Kind** is a tool for running local Kubernetes clusters using Docker container “nodes”. Kind was primarily designed for
testing Kubernetes itself, but may be used for local development or CI.

Для установки открываем документацию [Kind](https://kind.sigs.k8s.io/). Инструкция по установке находится в
разделе [Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start/). Установка на Linux:

    curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.11.1/kind-linux-amd64    
    chmod +x ./kind   
    sudo mv ./kind /usr/local/bin/kind

В итоге видим:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/12_kind_2.png)

Проверяем версию в консоли:  
`kind version` => _kind **v0.11.1** go1.16.4 linux/amd64_

3. Далее установим утилиту **kubectl** - это клиент Kubernetes-а.

Инструмент командной строки **kubectl** позволяет запускать команды для **кластеров Kubernetes**. Вы можете использовать
kubectl для развертывания приложений, проверки и управления ресурсов кластера, а также для просмотра логов.

Установим [kubectl](https://kubernetes.io/ru/docs/tasks/tools/install-kubectl/):

- Загрузить версию v1.23.0 для Linux:  
  `curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.23.0/bin/linux/amd64/kubectl`

- Сделать двоичный файл kubectl исполняемым:  
  `chmod +x ./kubectl`

- Переместить двоичный файл в директорию из переменной окружения PATH:  
  `sudo mv ./kubectl /usr/local/bin/kubectl`  
  В итоге видим:  
  ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/13_kubectl_2.png)

- Проверить установленную версию:  
  `kubectl version --client` => _Client Version: version.Info{Major:"1", Minor:"23", GitVersion:"v1.23.0", GitCommit:"
  ab69524f795c42094a6630298ff53f3c3ebab7f4", GitTreeState:"clean", BuildDate:"2021-12-07T18:16:20Z", GoVersion:"
  go1.17.3", Compiler:"gc", Platform:"linux/amd64"}_

4. Запустим кластер **kind** так, чтобы на него можно было потом легко установить Ingress: пример конфига
   возьмём [отсюда](https://kind.sigs.k8s.io/docs/user/ingress/).

Создадим конфиг k8s/**kind-config.yaml** с таким содержимым:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/14_kind_config_2.png)

- `kind: Cluster` - видим что в нашем конфиге описан кластер.
- `apiVersion: kind.x-k8s.io/v1alpha4` - версия.
- `nodes` - в этом разделе описаны ноды.
- Нода `control-plane` это как раз **мастер-нода**.
- `kubeadmConfigPatches` - конфигурация для **kind**-а, чтобы всё потом заработало с **Ingress**-ом.
- `extraPortMappings` - на мастер-ноде прокидываем порты, чтобы как раз была единая точка входа, т.е. на мастере будет
  открыт конкретный порт, на который мы будем слать запросы.
- `role: worker` - также сделаем 2 **воркера**. В итоге будет 1 мастер и 2 воркера.

Сначала убедимся, что кластер отсутствует:  
`kind get clusters` => вывод в терминал такой: _No kind clusters found._  

Посмотрим конфигурацию kubectl:  
`kubectl config view` => вывод такой:  

    apiVersion: v1
    clusters: null
    contexts: null
    current-context: ""
    kind: Config
    preferences: {}
    users: null
   
Теперь создаём кластер командой:  
`kind create cluster --name my-cluster --config k8s/kind-config.yaml`    
и видим что кластер создался:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_1_cluster_2.png)  

Также посмотрим кластер командой:  
`kind get clusters` => _my-cluster_

Снова посмотрим конфигурацию kubectl:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_2_config_2.png)  

Посмотрим ноды кластера:    
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_3_get_nodes.png)  

Также проверим следующие команды:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_4_docker_commands_1.png)    
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_5_docker_commands_2.png)  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_6_docker_commands_3.png)  

5. Теперь установим [Ingress NGINX](https://kind.sigs.k8s.io/docs/user/ingress#ingress-nginx). Команда в консоли
   для установки:  
`kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml`  
   
результат выполнения команды:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/15_7_ingress_setup.png)  

Далее команда:

    kubectl wait --namespace ingress-nginx \
      --for=condition=ready pod \
      --selector=app.kubernetes.io/component=controller \
      --timeout=90s

=> _pod/ingress-nginx-controller-6bccc5966-sxdfc condition met_ (всё ок, Ingress поставился).

6. Начнём с самого нижнего уровня, который связан с **подами**. Посмотрим как сделать конфиг, чтобы Кубернетес-кластер
   нас понял и запустил наше приложение в 3 экземплярах.

Гуглим "_kubernetes deployment example_" и
открываем [инструкцию по Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/). Тут
копируем пример конфига.

**Deployment** - это как раз та сущность Кубернетеса, которая отвечает за поды. И описав Деплоймент, мы можем описать,
какие поды и в скольки экземплярах нам нужны.

В папку **k8s** будем складывать все конфиги, связанные с Кубернетесом. В самом конце тут будут такие конфиги:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/16_k8s_directory.png)

Создаём **deployment.yaml**, вставляем сюда пример конфига и немного редактируем. В результате получается так:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/17_deployment_3.png)  

Далее команда в терминале:    
`kubectl apply -f k8s/deployment.yaml` => _deployment.apps/cats-api-deployment created_

Чтобы посмотреть поды нужна команда:  
`kubectl get pods`

Чтобы мониторить поды в реальном времени нужно добавить флаг **--watch**, т.е. такая команда:  
`kubectl get pods --watch`

В итоге смотрим поды:    
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/18_1_get_pods.png)

Видим, что поды запускаются и крашатся. Посмотрим ошибку командой:  
`kubectl logs cats-api-deployment-58b69bb468-b5xpv`  
Ошибка такая:  
_org.postgresql.util.PSQLException: Connection to localhost:15431 refused. Check that the hostname and port are
correct and that the postmaster is accepting TCP/IP connections_  
Нужно добавить **environment-переменную**: опять идём в k8s/**deployment.yaml** и добавляем блок env:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/18_2_env.png)  

Далее удалим поды:  
`kubectl delete pods --all` => все поды удалены.  
Неверно! Надо было удалить не поды а Деплоймент! Потому что Деплоймент следит за тем, чтобы было 3 пода. И если это
нарушается, то он пытается восстановить это. Поэтому пишу:  
`kubectl delete deploy --all` => _deployment.apps "cats-api-deployment" deleted_  
И вот теперь:  
`kubectl get pods` => _No resources found in default namespace._  
`kubectl get deploy` => _No resources found in default namespace._  

Далее:    
`kubectl apply -f k8s/deployment.yaml` => _deployment.apps/cats-api-deployment created_  
и опять `kubectl get pods` => теперь видим что все 3 пода запущены:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/19_pods_running.png)
Опять смотрим логи: `kubectl logs cats-api-deployment-869476485d-k2txk`  
и видим что теперь всё запустилось и всё ок:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/20_pod_ok.png)

Мы можем больше: мы можем **пробросить порт (сделать порт-форвард)**:    
`kubectl port-forward cats-api-deployment-869476485d-k2txk 8899:8081`  
где **8899** - порт на хост-машине, **8081** - порт внутри контейнера.  
Теперь приложение в браузере доступно по адресу: http://localhost:8899/api/v1/cat  
Текущий IP адрес: http://localhost:8899/api/v1/ip => **10.244.2.7**  
Документация доступна по адресу: http://localhost:8899/swagger-ui/index.html

7. Далее перейдём к уровню с **сервисами**. Гуглим "_kubernetes service example_" и открываем  
   [инструкцию по Service](https://kubernetes.io/docs/concepts/services-networking/service/). Тут скопипастим
   пример сервиса.

Создаём файл k8s/**service.yaml**, вставляем пример сервиса, редактируем и получаем:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/21_service.png)

Применим свой сервис:  
`kubectl apply -f k8s/service.yaml` => _service/cats-api-service created_

Команда в терминале:    
`kubectl get svc` => увидим наш сервис **cats-api-service**:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/22_get_service.png)

8. Теперь сделаем конфиг с Ингрессом: скопипастим образец
   [тут](https://kind.sigs.k8s.io/docs/user/ingress#using-ingress), создадим файл k8s/**ingress.yaml**,
   подредактируем и получим:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/23_ingress.png)  

- это и будет единая точка входа в наш кластер;
- описали в этом файле **рулы (правила)**.  

Применим этот конфиг (создадим **Ingress Rules**):  
`kubectl apply -f k8s/ingress.yaml` => _ingress.networking.k8s.io/my-ingress created_

9. Посмотрим в файл k8s/**kind-config.yaml**. Тут видим порт (**hostPort: 8888**).  
   Пойдём в браузере на **порт 8888** и посмотрим, что нас ждёт на **cats-api**:  
   http://localhost:8888/cats-api/api/v1/cat  
   Видим ошибку 404:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/24_error_page.png)
   Ошибка 404 потому что к путю теперь добавлен **cats-api**. Чтобы это пофиксить надо открыть  
   k8s/**deployment.yaml** и сказать нашему приложению, чтобы оно слушало нас начиная с **cats-api**. Для этого
   добавим ещё одну **переменную окружения**, которая называется **spring.mvc.servlet.path**, т.е с этого пути
   должно стартовать наше приложение:  
   ![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/25_deployment_final.png)

Обновим наши поды:     
`kubectl apply -f k8s/deployment.yaml` => _deployment.apps/cats-api-deployment configured_

Проверим что с подами всё хорошо:  
`kubectl get pods` => видим что старые поды удалены, новые поднялись (имена другие):  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/26_new_pods.png)

Опять пробуем открыть: http://localhost:8888/cats-api/api/v1/cat и теперь видим список всех котиков.  
Документация теперь доступна по адресу: http://localhost:8888/cats-api/swagger-ui/index.html  
Текущий IP адрес: http://localhost:8888/cats-api/api/v1/ip => 

Т.е мы в итоге проходим полный путь:

- человек пришёл и сделал такой запрос: `/cats-api/api/v1/cat`;
- нагрузка балансируется, и всё работает;
- клиент даже не знает, в какой из 3 подов отправился запрос;
- очень удобно так организовывать масштабирование.

10. Попробуем что-нибудь немножко изменить. Например в **deployment.yaml** поменяем:

        spec:
            # replicas: 3 # запустить в таком количестве экземпляров те приложения, которые имеют указанную метку
            replicas: 10

т.е. теперь сделаем **10 инстансов**. Если бы мы это делали без Кубернетеса, то это было бы очень тяжело.

Говорим применить:   
`kubectl apply -f k8s/deployment.yaml` => _deployment.apps/cats-api-deployment configured_

Теперь смотрим поды: `kubectl get pods` => и видим все 10 штук:  
![](https://github.com/aleksey-nsk/cats-api/blob/master/screenshots/27_now_10_pods.png)

Далее опять открываем:  
http://localhost:8888/cats-api/api/v1/cat  
и видим, что всё работает.

11. В конце всё удалим:   
`kubectl delete svc --all`  
`kubectl delete deploy --all`  
`kind delete cluster --name my-cluster`  

Всё удалено, проверяем:  
- `kind get clusters` => _No kind clusters found._  

  
- `kubectl config view`  => вывод в терминал:  

         apiVersion: v1
         clusters: null
         contexts: null
         current-context: ""
         kind: Config
         preferences: {}
         users: null

# Использованные источники

- [Пишем Spring Boot микросервис для деплоя в Kubernetes](https://www.youtube.com/watch?v=KPLJ0i5Ocws)
- [Документация Kind](https://kind.sigs.k8s.io/)
- [Установка и настройка kubectl](https://kubernetes.io/ru/docs/tasks/tools/install-kubectl/)
- [Ingress](https://kind.sigs.k8s.io/docs/user/ingress/)
- [Инструкция по Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [Инструкция по Service](https://kubernetes.io/docs/concepts/services-networking/service/)
