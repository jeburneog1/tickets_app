# 📚 Documentación Técnica - Sistema de Venta de Entradas

Bienvenido a la documentación técnica completa del sistema de venta de entradas Nequi.

---

## � Recursos Importantes

- **Collection de Postman**: [`Request for tickets app.postman_collection.json`](Request%20for%20tickets%20app.postman_collection.json) - Incluye todos los endpoints del API
- **Diagramas del Sistema**: 
  - [`doc/diagrams.html`](doc/diagrams.html) - Visualización interactiva de diagramas
- **Configuración Docker**: [`docker-compose.yml`](docker-compose.yml) - Para levantar el entorno local

---

## 🚀 Comandos Útiles

### Levantar el Ambiente
```bash
# Iniciar servicios (DynamoDB + SQS + App)
docker compose up -d

# Ver logs en tiempo real
docker compose logs -f tickets-service

# Ver logs de un servicio específico
docker compose logs dynamodb-local
docker compose logs localstack
```

### Build y Tests
```bash
# Compilar el proyecto
./gradlew build

# Ejecutar tests
./gradlew test

# Ejecutar tests con reporte de cobertura
./gradlew clean test jacocoTestReport
```

### Detener el Ambiente
```bash
# Detener todos los servicios
docker compose down

# Detener y eliminar volúmenes (limpieza completa)
docker compose down -v
```

### Acceso a Servicios Locales
- **API**: http://localhost:8080
- **DynamoDB Local**: http://localhost:8000
- **LocalStack (SQS)**: http://localhost:4566

---

## 🔑 Conceptos Clave del Sistema

### Arquitectura Reactiva
El sistema usa **Spring WebFlux** con programación reactiva (Mono/Flux) para manejar alta concurrencia sin bloquear threads.

### Procesamiento Asíncrono
Las órdenes se procesan de forma **asíncrona** usando:
- **Amazon SQS** para colas de mensajes
- **Consumer** que procesa las órdenes en background
- **Retries automáticos** para resiliencia

### Persistencia NoSQL
- **Amazon DynamoDB** para todas las entidades (Event, Order, Ticket)
- **Optimistic Locking** para evitar condiciones de carrera
- **GSI (Global Secondary Indexes)** para búsquedas eficientes

### Estados de Tickets
1. `AVAILABLE` → Disponible para compra
2. `RESERVED` → Reservado temporalmente (10 min)
3. `PENDING_CONFIRMATION` → En proceso de confirmación
4. `SOLD` → Vendido (estado final)
5. `COMPLIMENTARY` → Cortesía (estado final)

---

## 📊 Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Lenguaje** | Java | 25 |
| **Framework** | Spring Boot | 4.0.0 |
| **Programación Reactiva** | Spring WebFlux | 4.0.0 |
| **Base de Datos** | Amazon DynamoDB | - |
| **Mensajería** | Amazon SQS | - |
| **Build Tool** | Gradle | 8.12 |
| **Testing** | JUnit 5 + Reactor Test | - |
| **Containerización** | Docker | - |

---

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/nequi/tickets/
│   │   ├── domain/              # Entidades y lógica de negocio
│   │   │   ├── model/           # Records inmutables (Event, Order, Ticket)
│   │   │   ├── repository/      # Interfaces (Ports)
│   │   │   └── exception/       # Excepciones de dominio
│   │   ├── usecase/             # Casos de uso (lógica de aplicación)
│   │   ├── infrastructure/      # Implementaciones técnicas
│   │   │   ├── controller/      # REST Controllers (WebFlux)
│   │   │   ├── repository/      # DynamoDB Repositories (Adapters)
│   │   │   ├── messaging/       # SQS Producer & Consumer
│   │   │   ├── scheduler/       # Tareas programadas
│   │   │   └── dto/             # DTOs de entrada/salida
│   │   └── config/              # Configuración Spring
│   ├── resources/
│   │   └── application.yml      # Configuración de la app
│   └── doc/                     # 📚 Documentación técnica (AQUÍ ESTÁS)
└── test/
    └── java/com/nequi/tickets/
        ├── unit/                # Tests unitarios
        └── integration/         # Tests de integración
```

**Última actualización:** Febrero 24, 2026
