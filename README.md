# Warehouse Sensor Monitoring System

A reactive, multi-service system for monitoring warehouse environmental conditions via UDP sensors.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────┐
│                    Sensor Simulator                            │
│   (sends UDP datagrams at configured intervals)                │
└──────────────┬────────────────────────┬───────────────────────┘
               │ UDP :3344               │ UDP :3355
               ▼                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Warehouse Service (:8081)                     │
│                                                                  │
│  UdpListenerManager                                              │
│   ├── UdpListener (TEMPERATURE, port 3344)                       │
│   └── UdpListener (HUMIDITY,    port 3355)                       │
│           │                                                      │
│           ▼  parse & enrich                                      │
│  MeasurementForwarder ──── HTTP POST ──────────────────────────┐ │
└────────────────────────────────────────────────────────────────┘ │
                                                                    │
                            ┌───────────────────────────────────────┘
                            ▼  POST /api/measurements
┌──────────────────────────────────────────────────────────────────┐
│                  Central Monitoring Service (:8080)               │
│                                                                   │
│  MeasurementController (REST)                                     │
│           │                                                       │
│           ▼  @Async                                               │
│  AlarmService                                                     │
│   ├── temperature > 35°C → ⚠️ ALARM                              │
│   └── humidity    > 50%  → ⚠️ ALARM                              │
└───────────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
warehouse-monitoring/               ← Maven multi-module root
├── central-service/                ← Central Monitoring Service (port 8080)
│   └── src/
│       ├── main/java/com/monitoring/central/
│       │   ├── CentralMonitoringApplication.java
│       │   ├── alarm/
│       │   │   ├── AlarmEvent.java        (Java 17 record)
│       │   │   └── AlarmService.java      (@Async evaluation)
│       │   ├── config/
│       │   │   ├── AsyncConfig.java       (thread pool)
│       │   │   └── ThresholdProperties.java
│       │   ├── model/
│       │   │   ├── SensorReading.java
│       │   │   └── SensorType.java
│       │   └── service/
│       │       └── MeasurementController.java  (REST API)
│       └── test/ ...
├── warehouse-service/              ← Warehouse Service (port 8081)
│   └── src/
│       ├── main/java/com/monitoring/warehouse/
│       │   ├── WarehouseApplication.java
│       │   ├── config/
│       │   │   ├── RestTemplateConfig.java
│       │   │   └── WarehouseProperties.java
│       │   ├── model/
│       │   │   ├── SensorReading.java
│       │   │   └── SensorType.java
│       │   ├── service/
│       │   │   └── MeasurementForwarder.java  (@Async HTTP POST)
│       │   └── udp/
│       │       ├── UdpListener.java           (Runnable, one per sensor type)
│       │       ├── UdpListenerManager.java    (Spring lifecycle)
│       │       └── UdpMessageParser.java      (wire format parser)
│       └── test/ ...
└── sensor-simulator/               ← Test sensor (replaces netcat)
    └── src/main/java/com/monitoring/simulator/
        ├── SensorSimulatorApplication.java
        ├── SensorSimulator.java      (@Scheduled UDP sender)
        └── SimulatorProperties.java
```

---

## Prerequisites

- Java 17+
- Maven 3.8+

---

## Building

```bash
cd warehouse-monitoring
mvn clean install -DskipTests
```

Or with tests:

```bash
mvn clean verify
```

---

## Running the System

Open **three terminal windows** and run in this order:

### Terminal 1 — Central Monitoring Service
```bash
cd central-service
mvn spring-boot:run
```
Starts on **http://localhost:8080**

### Terminal 2 — Warehouse Service
```bash
cd warehouse-service
mvn spring-boot:run
```
Starts on **http://localhost:8081**, opens UDP sockets on **3344** and **3355**

### Terminal 3 — Sensor Simulator
```bash
cd sensor-simulator
mvn spring-boot:run
```
Sends UDP datagrams every few seconds; every 4th–5th packet is a spike that triggers an alarm.

---

## Sending Manual UDP Packets (netcat)

```bash
# Temperature reading (normal — below 35°C threshold)
echo -n "sensor_id=t1; value=30" | nc -u -w1 localhost 3344

# Temperature alarm (above 35°C threshold)
echo -n "sensor_id=t1; value=40" | nc -u -w1 localhost 3344

# Humidity reading (normal — below 50% threshold)
echo -n "sensor_id=h1; value=40" | nc -u -w1 localhost 3355

# Humidity alarm (above 50% threshold)
echo -n "sensor_id=h1; value=70" | nc -u -w1 localhost 3355
```

---

## REST API

### Central Service endpoints

| Method | Path                          | Description                  |
|--------|-------------------------------|------------------------------|
| POST   | /api/measurements             | Accept single sensor reading |
| POST   | /api/measurements/batch       | Accept batch of readings     |
| GET    | /api/status                   | Health check + alarm count   |

### Example POST (curl)
```bash
curl -s -X POST http://localhost:8080/api/measurements \
  -H "Content-Type: application/json" \
  -d '{"sensorId":"t1","type":"TEMPERATURE","value":40.0,"warehouseId":"WH-01"}'
```

### Status check
```bash
curl -s http://localhost:8080/api/status | python3 -m json.tool
```

---

## Thresholds

| Sensor Type  | Threshold | Configured In                           |
|--------------|-----------|-----------------------------------------|
| Temperature  | 35°C      | central-service/application.yml         |
| Humidity     | 50%       | central-service/application.yml         |

---

## Expected Alarm Output

When a threshold is breached, you'll see this in the **Central Service** console:

```
⚠️  ALARM TRIGGERED ⚠️
  Warehouse  : WH-01
  Sensor     : t1 (TEMPERATURE)
  Measured   : 40.00°C
  Threshold  : 35.00°C
  Exceeded By: 5.00°C
  Triggered  : 2024-05-01T12:34:56.789Z
```

---

## Running Tests

```bash
# All modules
mvn test

# Specific module
cd central-service && mvn test
cd warehouse-service && mvn test
```

---

## Configuration

### Warehouse ID (for multi-warehouse scenarios)
Edit `warehouse-service/src/main/resources/application.yml`:
```yaml
warehouse:
  id: WH-02   # change this per warehouse instance
```

### Run a second warehouse on a different port:
```bash
cd warehouse-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082 --warehouse.id=WH-02"
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| UDP for sensors | Matches spec; sensors are fire-and-forget |
| HTTP for warehouse→central | Reliable, observable, easy to extend with auth/TLS |
| @Async alarm evaluation | HTTP response returns immediately; alarms don't block ingest |
| Java 17 records (AlarmEvent, ParsedMessage) | Immutable value objects with minimal boilerplate |
| Switch expressions for threshold dispatch | Exhaustive at compile time — compiler catches missing sensor types |
| Separate UdpListener (Runnable) + Manager (Spring bean) | Testable without Spring; lifecycle managed by Spring |
| ThresholdProperties (@ConfigurationProperties) | Type-safe, externally configurable, no magic strings |
