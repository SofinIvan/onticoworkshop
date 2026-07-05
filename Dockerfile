FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B -q
COPY backend/src ./src
RUN mvn package -DskipTests -B -q

FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY tsconfig.json tsconfig.app.json tsconfig.node.json vite.config.ts index.html ./
COPY src/ ./src/
ENV VITE_API_BASE_URL=/api
RUN npm run build

FROM nginx:alpine
RUN apk add --no-cache openjdk17-jre

COPY --from=backend-build /app/target/onticoworkshop-backend-*.jar /app/app.jar
COPY --from=frontend-build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN sed -i 's|http://backend:8080/|http://127.0.0.1:8080/|g' /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD sh -c "java -jar /app/app.jar & exec nginx -g 'daemon off;'"
