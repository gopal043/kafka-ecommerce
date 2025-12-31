# 📋 Table of Contents

Features

Architecture

Tech Stack

Prerequisites

Quick Start

Detailed Setup

API Documentation

Testing

Monitoring


# 🌟 Features
Core Features

✅ Real-time Order Processing with Kafka event streaming

✅ JWT Authentication with Spring Security

✅ API Gateway with Nginx (Rate limiting, Load balancing)

✅ Circuit Breaker Pattern using Resilience4j

✅ Distributed Tracing with Zipkin

✅ Multi-database Strategy (PostgreSQL + MongoDB)

✅ Containerized with Docker Compose

# Kafka Implementations
Multiple Producer/Consumer patterns

Kafka Streams for real-time analytics

Exactly-once semantics

Consumer groups & rebalancing


# Microservices Patterns
Event-Driven Architecture

SAGA pattern for distributed transactions

CQRS pattern implementation

Service discovery & configuration

Health checks & metrics

🏗️ Architecture

    ┌─────────────────┐    ┌─────────────────────────────────────┐    ┌─────────────────┐
    │   Client        │───▶│         Nginx API Gateway          │───▶│   Analytics     │
    │   (Postman/     │    │   (Rate Limiting, JWT Validation)  │    │   Dashboard     │
    │    Browser)     │    └─────────────────────────────────────┘    └─────────────────┘
    └─────────────────┘               │           │           │
                                      ▼           ▼           ▼
                          ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
                          │  Auth        │ │  Order       │ │  Inventory   │
                          │  Service     │ │  Service     │ │  Service     │
                          │  (8084)      │ │  (8081)      │ │  (8082)      │
                          └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
                                 │                 │                │
                          ┌──────┴─────────────────┴────────────────┴──────┐
                          │             Apache Kafka Cluster                │
                          │            (Event Streaming Platform)           │
                          │         Topics: orders, user-orders,            │
                          │               inventory-events                  │
                          └───────────────────────┬─────────────────────────┘
                                                  │
                                      ┌───────────┴───────────┐
                                      ▼                       ▼
                            ┌──────────────────┐   ┌──────────────────┐
                            │ Notification     │   │ Kafka Streams    │
                            │ Service          │   │ (Real-time       │
                            │ (8083)           │   │ Analytics)       │
                            └──────────────────┘   └──────────────────┘
                      
                      

# 🛠️ Tech Stack
Component	Technology

Backend	Java 17, Spring Boot 3.1, Spring Security, Spring Kafka

Event Streaming	Apache Kafka, Kafka Streams, Kafka UI

Databases	PostgreSQL, MongoDB, Redis

API Gateway	Nginx (Rate limiting, Load balancing)

Security	JWT, Spring Security, BCrypt

Resilience	Resilience4j (Circuit Breaker, Retry)

Monitoring	Zipkin (Distributed Tracing), Micrometer

Containerization	Docker, Docker Compose

Build Tool	Maven

# 📦 Prerequisites

Java 17+ (Check: java -version)

Maven 3.8+ (Check: mvn -version)

Docker & Docker Compose (Check: docker --version)

Git (Check: git --version)

cURL or Postman for API testing

8GB+ RAM recommended for running all services



# 1. Clone and Setup the repository
git clone <your-repository-url>
cd kafka-ecommerce

# Start all containers (this may take 5-10 minutes first time)
docker-compose up -d --build

# Check running services
docker ps
(should show 9 containers running)

# 3. Create Kafka topics
docker exec kafka kafka-topics --create --topic orders --bootstrap-server localhost:9092 --partitions 3

docker exec kafka kafka-topics --create --topic inventory-events --bootstrap-server localhost:9092 --partitions 3

docker exec kafka kafka-topics --create --topic user-orders --bootstrap-server localhost:9092 --partitions 3

# 4. build individually
cd ../order-service && mvn clean package -DskipTests

cd ../inventory-service && mvn clean package -DskipTests

cd ../notification-service && mvn clean package -DskipTests

cd ../auth-service && mvn clean package -DskipTests

# 5. run services individually
cd ../order-service && java -jar order-service/target/*.jar

cd ../inventory-service && java -jar inventory-service/target/*.jar

cd ../notification-service && java -jar notification-service/target/*.jar

cd ../auth-service && java -jar auth-service/target/*.jar

# 6. Run Nginx container
cd /ngnix
docker build -t ecommerce-nginx:latest .
docker run -d --name nginx-gateway -p 80:80 --network kafka-network ecommerce-nginx:latest

# 📚 API Documentation
Base URLs

API Gateway: http://localhost:80

Order Service: http://localhost:8081

Inventory Service: http://localhost:8082

Notification Service: http://localhost:8083

Auth Service: http://localhost:8084

# Authentication Flow
1. Register User
bash
curl -X POST http://localhost:80/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
2. Login (Get JWT Token)
bash
curl -X POST http://localhost:80/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
Response:

json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "john_doe"
}
Order Management
Create Order
bash
curl -X POST http://localhost:80/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": "john_doe",
    "shippingAddress": "123 Main St, New York, NY",
    "items": [
      {
        "productId": "prod001",
        "productName": "MacBook Pro 16\"",
        "quantity": 1,
        "price": 2499.99
      }
    ]
  }'
  
Get Order Details
bash
curl http://localhost:80/api/orders/{orderId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
  
Cancel Order
bash
curl -X POST http://localhost:80/api/orders/{orderId}/cancel \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
  
Inventory Management
bash
# Check inventory
curl http://localhost:80/api/inventory/prod001 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Restock product
curl -X POST http://localhost:80/api/inventory/prod001/restock/100 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"


  # 📊 Monitoring
1. Kafka UI
URL: http://localhost:8080

Features:

View topics and partitions

Monitor consumer groups

Browse messages

Check topic configurations

2. Zipkin Distributed Tracing
URL: http://localhost:9411

Features:

End-to-end request tracing

Latency analysis

Dependency mapping

3. Service Health Checks
bash
# Individual service health
curl http://localhost:8081/actuator/health  # Order Service

curl http://localhost:8082/actuator/health  # Inventory Service

curl http://localhost:8083/actuator/health  # Notification Service

curl http://localhost:8084/actuator/health  # Auth Service

# Circuit breaker status
curl http://localhost:8081/actuator/circuitbreakers
