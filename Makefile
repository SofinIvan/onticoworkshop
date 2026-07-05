OPENAPI_FILE := tsp-output/schema/openapi.yaml

.DEFAULT_GOAL := help

.PHONY: help dev build check backend-build backend-run frontend-dev api-build openapi api-swagger swagger api-file

help: ## Показать список доступных make-команд.
	@awk 'BEGIN {FS = ":.*## "}; /^[a-zA-Z0-9_.-]+:.*## / {printf "  %-14s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

dev: ## Запустить бэкенд (фон) + фронтенд. Ctrl+C остановит фронтенд, бэкенд — lsof -ti:8080 | xargs kill.
	cd backend && mvn spring-boot:run -q &
	sleep 2
	npm run dev

build: ## Собрать фронтенд + бэкенд.
	$(MAKE) backend-build
	npm run build

check: build ## Собрать и проверить (алиас для build).

backend-build: ## Собрать JAR бэкенда.
	cd backend && mvn clean package -q -DskipTests

backend-run: ## Запустить бэкенд на http://localhost:8080.
	cd backend && mvn spring-boot:run -q

frontend-dev: ## Запустить Vite dev server на http://localhost:5173.
	npm run dev

api-build: ## Сгенерировать OpenAPI 3.1 из main.tsp в $(OPENAPI_FILE).
	npm run api:build

openapi: api-build ## Алиас для api-build.

api-swagger: ## Сгенерировать OpenAPI и запустить Swagger UI на http://127.0.0.1:8080.
	npm run api:swagger

swagger: api-swagger ## Алиас для api-swagger.

api-file: ## Показать путь к сгенерированному OpenAPI-файлу.
	@echo "$(OPENAPI_FILE)"
