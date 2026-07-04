OPENAPI_FILE := tsp-output/schema/openapi.yaml

.DEFAULT_GOAL := help

.PHONY: help api-build openapi api-swagger swagger api-file

help: ## Показать список доступных make-комxанд.
	@awk 'BEGIN {FS = ":.*## "}; /^[a-zA-Z0-9_.-]+:.*## / {printf "  %-14s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

api-build: ## Сгенерировать OpenAPI 3.1 из main.tsp в $(OPENAPI_FILE).
	npm run api:build

openapi: api-build ## Алиас для api-build.

api-swagger: ## Сгенерировать OpenAPI и запустить Swagger UI на http://127.0.0.1:8080.
	npm run api:swagger

swagger: api-swagger ## Алиас для api-swagger.

api-file: ## Показать путь к сгенерированному OpenAPI-файлу.
	@echo "$(OPENAPI_FILE)"
