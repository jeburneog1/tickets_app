# 📜 Scripts Directory

Este directorio contiene scripts de inicialización de infraestructura para el sistema de tickets.

## 📄 Scripts Disponibles

### 🗄️ init-dynamodb.sh

**Propósito:** Inicializar DynamoDB Local con todas las tablas necesarias.

**Tablas creadas:**

1. **events**
   - Partition Key: `eventId` (String)
   - GSI: `DateIndex` (indexado por `date`)
   - Capacidad: 5 RCU / 5 WCU

2. **tickets**
   - Partition Key: `ticketId` (String)
   - GSI: 
     - `EventIdIndex` (indexado por `eventId`)
     - `EventIdStatusIndex` (indexado por `eventId` + `status`)
     - `CustomerIdIndex` (indexado por `customerId`)
   - Capacidad: 10 RCU / 10 WCU

3. **orders**
   - Partition Key: `orderId` (String)
   - GSI:
     - `CustomerIdIndex` (indexado por `customerId`)
     - `EventIdIndex` (indexado por `eventId`)
   - Capacidad: 10 RCU / 10 WCU

**Uso manual:**

```bash
# Asegúrate de que DynamoDB Local esté corriendo
docker-compose up -d dynamodb-local

# Ejecutar el script
chmod +x scripts/init-dynamodb.sh
./scripts/init-dynamodb.sh
```

---

### 📬 init-sqs.sh

**Propósito:** Inicializar LocalStack con las colas SQS necesarias para procesamiento asíncrono de órdenes.

**Colas creadas:**

1. **order-processing-dlq** (Dead Letter Queue)
   - Retención de mensajes: 14 días (1209600 segundos)
   - Propósito: Recibir mensajes que fallaron después de 3 intentos

2. **order-processing-queue** (Cola principal)
   - Retención de mensajes: 4 días (345600 segundos)
   - Visibility Timeout: 5 minutos (300 segundos)
   - Receive Wait Time: 20 segundos (long polling)
   - Max Receive Count: 3 (después va a DLQ)

**Configuración:**
- Redrive Policy: Los mensajes que fallen 3 veces se mueven automáticamente a la DLQ
- Long Polling habilitado (20 segundos) para reducir costos y latencia

**Uso manual:**

```bash
# Asegúrate de que LocalStack esté corriendo
docker-compose up -d localstack

# Ejecutar el script
chmod +x scripts/init-sqs.sh
./scripts/init-sqs.sh
```

---

## 🔄 Ejecución Automática

Estos scripts se ejecutan automáticamente cuando usas `docker-compose up` gracias al servicio `aws-init` definido en `docker-compose.yml`.

El servicio `aws-init`:
1. ✅ Espera a que DynamoDB Local esté healthy
2. ✅ Espera a que LocalStack esté healthy
3. ✅ Ejecuta `init-dynamodb.sh`
4. ✅ Ejecuta `init-sqs.sh`
5. ✅ Termina exitosamente
6. ✅ La aplicación principal arranca solo después de que `aws-init` termine

---

## 🛠️ Troubleshooting

### Error: "Permission denied"

```bash
# Solución: Dar permisos de ejecución
chmod +x scripts/*.sh
```

### Error: "DynamoDB/SQS not ready"

Los scripts esperan automáticamente hasta que los servicios estén disponibles. Si fallan:

```bash
# Verificar que los servicios estén corriendo
docker-compose ps

# Ver logs de los servicios
docker-compose logs dynamodb-local
docker-compose logs localstack
```

### Error: "Table/Queue already exists"

Esto es normal. Los scripts detectan si ya existen y continúan sin error.

---

## 📝 Notas

- **AWS CLI:** Los scripts usan AWS CLI. El contenedor `aws-init` usa la imagen `amazon/aws-cli:latest`
- **Credenciales:** Para desarrollo local, usamos credenciales dummy (`test`/`test`)
- **Endpoints:** Los scripts usan los nombres de servicios de Docker (`dynamodb-local`, `localstack`)
- **Región:** Todos los recursos se crean en `us-east-1`

---

## 🔧 Personalización

Si necesitas agregar más tablas o colas, edita los scripts:

1. Abre `init-dynamodb.sh` o `init-sqs.sh`
2. Copia el bloque de creación existente
3. Modifica los nombres y configuraciones
4. Guarda y ejecuta `docker-compose up --build`

---

## ✅ Verificación

### Verificar tablas de DynamoDB:

```bash
aws dynamodb list-tables \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

### Verificar colas de SQS:

```bash
aws sqs list-queues \
  --endpoint-url http://localhost:4566 \
  --region us-east-1
```

### Describir una tabla:

```bash
aws dynamodb describe-table \
  --table-name events \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

### Ver atributos de una cola:

```bash
# Primero obtén la URL de la cola
QUEUE_URL=$(aws sqs get-queue-url \
  --queue-name order-processing-queue \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output text \
  --query 'QueueUrl')

# Luego consulta sus atributos
aws sqs get-queue-attributes \
  --queue-url $QUEUE_URL \
  --attribute-names All \
  --endpoint-url http://localhost:4566 \
  --region us-east-1
```

---

**¿Preguntas?** Consulta [DOCKER-GUIDE.md](../DOCKER-GUIDE.md) para más información sobre el despliegue completo.
