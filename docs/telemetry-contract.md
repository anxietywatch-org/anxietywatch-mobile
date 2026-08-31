# AnxietyWatch — Contrato de Telemetría (fuente única)

> Estado: **spec de referencia**. Define UN solo formato de telemetría para todas las
> capas: reloj → teléfono → API → BD → web. Si algo no coincide con este documento,
> el bug está en esa capa, no en el documento.
>
> Última revisión: 2026-08-28 · Base: rama `wip/mobile-integration-2026-08-28`
> · Hardware de validación: Galaxy Watch 6

---

## 0. Alcance

Esto cubre **telemetría continua** (ritmo cardíaco, IBI, acelerómetro, temperatura de
piel). **No** cubre:

- **Eventos sospechosos** (`SuspectedEventRequest`): es una *detección*, no telemetría.
  Esquema propio (`features` + `baseline`). Lo decide el reloj. Documento aparte.
- **SOS / cancelación / decisión de evento**: esquemas propios.

La **detección de anomalías es responsabilidad exclusiva del reloj**. El teléfono NO
corre ningún pipeline de features/scoring; solo mapea, persiste y reenvía telemetría.

---

## 1. Formato canónico (API + BD + web)

Fuente de verdad: `CreateTelemetryBatchRequest` + `TelemetrySampleDto` en
[`ApiDtos.kt`](../app/src/main/java/com/anxietywatch/mobile/data/remote/ApiDtos.kt).
Contrato confirmado con backend (`api.mangoon.xyz`) el 11/ago/2026.

**Endpoint:** `POST api/v1/telemetry/batch`
**Respuesta:** `TelemetryBatchAckResponse { batchId, accepted, duplicate }` — HTTP `202` lote
nuevo, HTTP `200` duplicado idempotente. Ambos = éxito.

```json
{
  "batchId": "b0e1c2d3-4f56-7890-abcd-ef1234567890",
  "deviceId": "9a8b7c6d-5e4f-3210-9876-543210fedcba",
  "userId": null,
  "sessionId": "11112222-3333-4444-5555-666677778888",
  "startedAt": "2026-08-28T10:00:00Z",
  "endedAt": "2026-08-28T10:00:04Z",
  "sequence": 12,
  "samples": [
    {
      "timestamp": "2026-08-28T10:00:00Z",
      "heartRateBpm": 78.0,
      "ibiMs": [812.0, 799.0],
      "accelerometer": { "x": 0.02, "y": -0.98, "z": 0.15 },
      "skinTemperatureCelsius": 33.4,
      "ambientTemperatureCelsius": null,
      "quality": { "heartRate": "good", "ibi": "good", "wearingState": "onBody" }
    }
  ]
}
```

### 1.1 Campos del lote

| Campo | Tipo | Req | Notas |
|---|---|---|---|
| `batchId` | GUID (string) | sí | Idempotencia. **Lo genera el reloj** y se conserva sin cambios en todo el trayecto. |
| `deviceId` | GUID (string) | sí | Reloj vinculado. En el móvil = `MonitoringSessionContext.pairedDeviceId()`. Nunca el UUID cero. |
| `userId` | `null` | sí | Siempre `null` desde el móvil. El backend lo infiere del JWT. |
| `sessionId` | GUID (string) | sí | Sesión de monitoreo del **móvil** (rota cada 24 h). No es la sesión del reloj. |
| `startedAt` | ISO-8601 UTC `Z` | sí | `timestamp` del primer sample del lote. |
| `endedAt` | ISO-8601 UTC `Z` | sí | `timestamp` del último sample del lote. |
| `sequence` | int | sí | Orden del lote dentro de la `sessionId`. Monótono creciente. Lo asigna el móvil. |
| `samples` | array | sí | ≥ 1. Un lote vacío se descarta y **no se confirma** (sin ACK al reloj). |

### 1.2 Campos del sample

| Campo | Tipo | Req | Notas |
|---|---|---|---|
| `timestamp` | ISO-8601 UTC `Z` | sí | Momento de captura. Clave de deduplicación dentro del lote: lecturas de distintos sensores con el mismo `timestamp` se fusionan en un sample. |
| `heartRateBpm` | double \| null | no | `null` si en ese `timestamp` no hubo lectura de HR. |
| `ibiMs` | double[] | **sí** | Intervalos latido-a-latido en ms. **Nunca `null`** — usar `[]` si no hay. |
| `accelerometer` | `{x,y,z}` \| null | no | Ejes en **g** (1 g ≈ 9,81 m/s²). `null` si no hubo lectura. Ver §3. |
| `skinTemperatureCelsius` | double \| null | no | °C. |
| `ambientTemperatureCelsius` | double \| null | no | °C. Hoy nadie lo llena. Ver §3. |
| `quality` | `SampleQualityDto` | sí | Default `{ "unknown", "unknown", "unknown" }`. |

### 1.3 `quality`

| Campo | Valores | Notas |
|---|---|---|
| `heartRate` | `good` \| `fair` \| `poor` \| `unknown` | Calidad de la lectura de HR. |
| `ibi` | `good` \| `fair` \| `poor` \| `unknown` | Calidad de los IBI. |
| `wearingState` | `onBody` \| `offBody` \| `unknown` | ¿El reloj está puesto? Ver §3. |

`accelerometer` es `{ "x": double, "y": double, "z": double }` — los tres obligatorios si
el objeto está presente.

---

## 2. Envelope Wear → Teléfono (`wear-telemetry-records-v2`)

El reloj **no** manda el formato canónico. Manda lecturas crudas por sensor y el teléfono
las ensambla. Este envelope es interno del transporte Data Layer; nunca llega a la API.

**Transporte:** `DataItem` en la ruta `/fog/v1/telemetry/{batchId}`.
**Payload:** bytes UTF-8 bajo la clave `"payload"` del `DataMap`.

```json
{
  "schemaVersion": "wear-telemetry-records-v2",
  "batchId": "b0e1c2d3-4f56-7890-abcd-ef1234567890",
  "records": [
    { "capturedAt": "2026-08-28T10:00:00Z", "type": "HEART_RATE",
      "payload": { "bpm": 78.0, "ibiMillis": [812.0, 799.0], "signalQuality": 0.95 } },
    { "capturedAt": "2026-08-28T10:00:00Z", "type": "ACCELEROMETER",
      "payload": { "x": 0.02, "y": -0.98, "z": 0.15 } },
    { "capturedAt": "2026-08-28T10:00:00Z", "type": "SKIN_TEMPERATURE",
      "payload": { "celsius": 33.4 } }
  ]
}
```

### 2.1 Envelope

| Campo | Tipo | Notas |
|---|---|---|
| `schemaVersion` | string | Debe ser exactamente `"wear-telemetry-records-v2"`. Si no, el teléfono descarta el lote. |
| `batchId` | GUID | Debe coincidir con el `{batchId}` de la ruta (`responseIdMatches`). Si no coincide, descarte. |
| `records` | array | Puede ir vacío → el lote resultante se descarta sin ACK. |

### 2.2 Record

| Campo | Tipo | Notas |
|---|---|---|
| `capturedAt` | ISO-8601 UTC `Z` | Se parsea con `Instant.parse()` (exige sufijo `Z` **estricto**, no `+00:00`). Timestamp inválido → se descarta ese record. |
| `type` | string | Case-insensitive. Valores: `HEART_RATE`, `ACCELEROMETER`, `SKIN_TEMPERATURE`. Otro valor → se ignora ese record. |
| `payload` | object | Forma según `type` (ver abajo). |

### 2.3 Payload por tipo

**`HEART_RATE`**

| Campo | Tipo | → canónico |
|---|---|---|
| `bpm` | double | `sample.heartRateBpm` |
| `ibiMillis` | (double\|string)[] | `sample.ibiMs` (se parsea a double; entradas no numéricas se descartan) |
| `signalQuality` | double 0..1 | `sample.quality.heartRate` **y** `sample.quality.ibi` (misma cadena, ver §2.4) |
| `wearingState` | string: `onBody`\|`offBody`\|`unknown` | `sample.quality.wearingState` (pasa tal cual; valor desconocido → `unknown`) |

**`ACCELEROMETER`**

| Campo | Tipo | → canónico |
|---|---|---|
| `x`, `y`, `z` | double (g) | `sample.accelerometer = { x, y, z }` |

**`SKIN_TEMPERATURE`**

| Campo | Tipo | → canónico |
|---|---|---|
| `celsius` | double | `sample.skinTemperatureCelsius` |

### 2.4 Mapeo `signalQuality` (double) → cadena de calidad

| `signalQuality` | cadena |
|---|---|
| `≥ 0.8` | `good` |
| `0.5 … 0.7999…` | `fair` |
| `> 0 … 0.4999…` | `poor` |
| `null`, `≤ 0` | `unknown` |

### 2.5 Reglas de ensamblado en el teléfono

1. Los `records` se agrupan por `capturedAt` (epoch ms) → un `TelemetrySampleDto` por
   timestamp.
2. `startedAt` / `endedAt` del lote = primer / último timestamp (ordenados).
3. `sequence` = `MonitoringSessionContext.nextSequence()`.
4. `sessionId` = sesión de monitoreo del móvil. `deviceId` = `pairedDeviceId()`. `userId` = `null`.
5. `ibiMs` ausente → `[]` (`httpIbiMs()`).
6. `quality.wearingState` = valor del campo `wearingState` del record `HEART_RATE`
   (`onBody`/`offBody`, case-insensitive). Cualquier otro valor o ausente → `"unknown"`.
7. `ambientTemperatureCelsius` = siempre `null` — ningún wearable actual lo aporta (§3.3).
8. Lote sin samples válidos → descarte, **sin ACK** al reloj.
9. Al confirmar entrega (`DELIVERED`) el teléfono borra el `DataItem` de Wear.

---

## 3. Puntos abiertos / brechas conocidas

| # | Tema | Estado / decisión |
|---|---|---|
| 1 | **Acelerómetro `x/y/z` vs `magnitudeG`** | **Cerrado (2026-08-28).** Wear: `AndroidMotionSensorProvider` promedia x/y/z por ventana de 1 s; el record `ACCELEROMETER` emite solo `{x,y,z}` en g; `magnitudeG` queda derivado, solo para detección interna. Mobile: `applyTelemetryReading` lee `x/y/z` directo (eje faltante → sin acelerómetro). **Requiere despliegue coordinado** — el móvil nuevo ya no interpreta `magnitudeG`. |
| 2 | **`quality.wearingState`** | **Cerrado (2026-08-28).** Contrato: campo `wearingState` (`onBody`\|`offBody`\|`unknown`) en el payload del record `HEART_RATE`. **Mobile:** ya lee el campo (`applyTelemetryReading`), default `"unknown"`. **Wear:** emite `wearingState`; hoy `"unknown"` porque ningún provider expone on-body state fiable — queda pendiente cablearlo a un sensor real si se consigue. |
| 3 | **`ambientTemperatureCelsius`** | **Decidido (2026-08-28):** el hardware actual (Galaxy Watch 6) no expone sensor de temperatura ambiente. Se queda como **siempre `null`**. El campo permanece en el canónico por si un wearable futuro lo aporta; ningún emisor lo llena hoy. |
| 4 | **`capturedAt` con `+00:00`** | El parser del teléfono usa `Instant.parse()` (solo acepta `Z`). El reloj debe serializar timestamps con sufijo `Z`, nunca `+00:00`. **Pendiente wear.** |
| 5 | **`signalQuality` numérico se pierde** | El valor 0..1 del reloj se colapsa a 4 cubos de cadena. Si el backend/web algún día quiere el número, habría que añadirlo al canónico. Hoy no se necesita. |

---

## 4. Cambios que dependen de este documento

### 4.1 Estado — Wear (`Prueba1/apps/wear`, vía OpenCode 2026-08-28)

Hecho (tests JVM + `assembleDebug` verdes, **sin prueba física**): envelope
`wear-telemetry-records-v2`, `capturedAt` con `Z`, `ACCELEROMETER` = `{x,y,z}`,
`HEART_RATE` con `bpm`/`ibiMillis`/`signalQuality`/`wearingState`, pairing por nonce
con `wearableDeviceId` persistente, ACK con payload vacío, NACK terminal, outbox con
4 estados durables, esquemas de SOS/cancel/suspected/decision, `fogProtocol` en
capabilities. Pendiente: validación física en Galaxy Watch, y cablear `wearingState`
a un sensor real (hoy emite `"unknown"`).

### 4.2 Estado — Mobile (rama `wip/mobile-integration-2026-08-28`, sin commitear)

Hecho: `applyTelemetryReading` lee `x/y/z`; `wearingState` se lee del record
`HEART_RATE`; `PhoneTelemetryMappingTest` actualizado; suite unitaria verde.

### 4.3 Formato canónico (§1)

Cualquier cambio requiere aprobación de backend + ajuste en web + migración de BD.
No es un cambio local.

---

## 5. Prerrequisitos del transporte Wear Data Layer

Lecciones de dos equipos que sí lograron entrega física Wear→móvil (Pixel Watch y
Galaxy Watch 6). El transporte **no entrega nada** si esto no se cumple:

| Requisito | Estado AnxietyWatch |
|---|---|
| **Mismo `applicationId` en reloj y teléfono** (+ firma compatible). Wear Data Layer solo entrega entre apps "correspondientes". El `namespace` puede diferir. | Teléfono: `com.anxietywatch.mobile`. Reloj: **hay que cambiarlo a `com.anxietywatch.mobile`** (era `com.anxietywatch.wear` → causa del `Failed to deliver message to AppKey`). |
| Firma debug: ambos APK compilados desde la misma máquina/usuario comparten `~/.android/debug.keystore` → coincide sin config extra. Release: misma clave explícita. | Debug OK si se instala desde la misma máquina. Release: pendiente. |
| Manifest listener con `<data>` que tenga **`scheme` + `host` + `pathPrefix`** (los 3, o el filtro nunca matchea). `BIND_LISTENER` está deprecado y rompe el lint. | Móvil: `PhoneDataLayerListenerService` con `DATA_CHANGED`+`MESSAGE_RECEIVED`, `scheme="wear"` `host="*"` `pathPrefix="/fog/v1"`. Reloj: idem `/fog/v1`. |
| Cada lado publica una capability en `res/values/wear.xml` (`android_wear_capabilities`). | Móvil: `fog_phone_v1`. Reloj: `fog_watch_v1`. |
| El `nodeId` de Wear es **transitorio** — nunca cachearlo; descubrir nodos al enviar (`NodeClient.connectedNodes`, preferir `isNearby`). | — |
| Descubrir la contraparte con `CapabilityClient.getCapability(cap, FILTER_REACHABLE)`. | Móvil descubre por `connectedNodes`; capability recomendada como refuerzo. |

### 5.1 Runbook de prueba física ("app cerrada")

1. Emparejar reloj↔teléfono por el proceso normal de Wear OS (nunca pairing BT propio).
2. Instalar ambos APK debug desde la misma máquina (firma debug coincide).
3. `adb -s <phone> logcat -s PhoneDataLayerBridge`.
4. Pairing por nonce desde el teléfono → ver respuesta en `/fog/v1/pairing/identity`.
5. Enviar telemetría → ver `onDataChanged item path=/fog/v1/telemetry/...` en el móvil.
6. "App cerrada": `HOME` (no abrir la app) → esperar ~40 s → `adb shell am kill <pkg>`.
   **Nunca `am force-stop`** (deja el paquete en estado `stopped` y GMS no lo arranca).
   La entrega de GMS puede tardar ~1 min; el servicio debe despertar solo.
