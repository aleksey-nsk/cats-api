Пишем Spring Boot микросервис для деплоя в Kubernetes с нуля!

Напишем REST-сервис с двумя методами:
1) сохранить котика
2) показать всех котиков

Развернём Kubernetes через инструмент kind(?), и развернем наш сервис в кубере.

1. Сначала пишу свой кошачий сервис.

2. Приложение:
http://localhost:8081/

3. Документация апи:
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>1.6.6</version>
        </dependency>
        
        Адрес: http://localhost:8081/swagger-ui/index.html

4. Далее @Accessors(chain = true)

5. Попробуем приложение обернуть в Докер (44:10)



