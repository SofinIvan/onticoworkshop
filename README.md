# Ontico Workshop — Calendar Booking

Сервис бронирования встреч. Организатор создаёт встречи и настраивает доступность; гость выбирает свободный слот и записывается.

## Технологии

| Слой | Технология |
|---|---|
| Фронтенд | Vite 7 + React 19 + TypeScript 5.9 |
| UI-библиотека | Mantine v8 |
| API-спецификация | TypeSpec → OpenAPI 3.1 |
| Бэкенд | Spring Boot 3.5.14 + Java 17 |
| База данных | SQLite (автосоздание `onticoworkshop.db`) |
| ORM | Hibernate 6 + Spring Data JPA |
| Сборка бэкенда | Maven |

## Переменные окружения

Создайте `.env` в корне проекта:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Если переменная не задана — фронтенд работает с демо-данными (без бэкенда).

## Быстрый старт

```bash
# Установка зависимостей
npm install

# Сборка всего проекта
make build

# Запуск (бэкенд + фронтенд)
make dev

# Или раздельно:
make backend-run    # Бэкенд на http://localhost:8080
make frontend-dev   # Фронтенд на http://localhost:5173
```

## Docker (локальная разработка)

```bash
make docker-build   # Сборка образов (JAR бэкенда собирается локально)
make docker-up      # Запуск: фронтенд на :80, бэкенд на :8080
make docker-down    # Остановить и удалить контейнеры
```

`docker-compose.yml` использует раздельные контейнеры: бэкенд (`backend/Dockerfile`) + фронтенд (`Dockerfile.frontend-only`).

## Деплой

Приложение задеплоено на Render.com:
- **URL**: https://onticoworkshop.onrender.com
- Автодеплой из ветки `develop` при push
- Бесплатный тариф: сервис засыпает через 15 мин бездействия, холодный старт ~100 сек
- SQLite база эфемерная (сбрасывается при редеплое), seed-данные автосоздаются

Production Dockerfile (`Dockerfile`) — multi-stage: собирает Java-бэкенд и React-фронтенд, запускает оба в одном контейнере (nginx + Java).

## Структура проекта

```
├── src/                    # Фронтенд (Vite + React + TypeScript)
│   ├── App.tsx             # Основной компонент, всё состояние
│   ├── api.ts              # API-клиент и типы
│   └── main.tsx            # Точка входа
├── backend/                # Бэкенд (Spring Boot + Java 17)
│   └── src/main/java/com/onticoworkshop/
│       ├── model/          # JPA-сущности
│       ├── repository/     # Spring Data репозитории
│       ├── service/        # Бизнес-логика
│       ├── controller/     # REST-контроллеры
│       ├── dto/            # Request/response DTO
│       ├── config/         # CORS, seed data
│       └── exception/      # Обработка ошибок
├── main.tsp                # TypeSpec API-спецификация
├── CONTEXT.md              # Глоссарий предметной области
├── AGENTS.md               # Инструкции для AI-агентов
├── docs/adr/               # Architecture Decision Records
└── Makefile                # Команды разработки
```

## Основные возможности

- **Workspace** — создание и настройка встреч (Meeting): название, длительность, шаг слотов, буферы, ограничения по дням и часам (MeetingTimeRule)
- **Availability** — настройка еженедельной доступности и исключений на даты
- **Booking** — выбор встречи, даты, слота и запись гостя
- **Публичная ссылка** — каждый Meeting имеет UUID, ссылка `/?meeting={uuid}` открывает страницу бронирования для гостей
- **Защита от дублей** — один гость не может записаться на одну встречу дважды
- **Автосоздание гостей** — при первом бронировании гость сохраняется как User
- **Таблица бронирований** — фильтрация по встрече и гостю, сортировка по столбцам
- **Каскадное удаление** — при удалении встречи удаляются все её бронирования
- **Модальные подтверждения** — все операции удаления требуют подтверждения

## API

Спецификация API описана в `main.tsp`. Для генерации OpenAPI:

```bash
make api-build     # tsp-output/schema/openapi.yaml
make api-swagger   # Swagger UI на http://127.0.0.1:8080
```

## Документация

- `CONTEXT.md` — глоссарий предметной области
- `AGENTS.md` — инструкции для разработки с AI-агентом
- `docs/adr/` — архитектурные решения
- `docs/research/` — исследования (cal.com booking flow)

## Проверка

```bash
make check    # Сборка фронтенда + бэкенда
npm run build # Только фронтенд (tsc -b && vite build)
```
