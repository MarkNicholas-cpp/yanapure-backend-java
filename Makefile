.PHONY: build run test up down fmt

build:
	./mvnw -q -DskipTests package

test:
	./mvnw -q -DskipTests=false verify

fmt:
	./mvnw -q spotless:apply

run:
	./mvnw spring-boot:run

up:
	docker compose up --build -d

down:
	docker compose down -v
