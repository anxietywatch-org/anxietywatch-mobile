# AnxietyWatch Mobile — Integration Checkpoint

## Estado validado

- Patient frontend implementado.
- Caregiver frontend V2 implementado.
- Caregiver conectado parcialmente a backend real.
- Pairing Galaxy Watch 7 validado físicamente.
- Patient session preservation validada.
- Data Layer Wear → Mobile validado físicamente.
- Contrato telemetry `ibiMs` corregido.
- Mapping `heart_rate` / `skin_temperature` corregido.
- Permanent telemetry NACK implementado.
- Historical `TERMINAL_FAILED` redelivery resuelto en tests.
- State-preserving Android test workflow disponible.

## Estado de entrega Wear/Fog

Baseline físico actual de QA:

Mobile:
- `PENDING_HTTP`: 0
- `ACK_PENDING`: 0
- `DELIVERED`: 11
- `TERMINAL_FAILED`: 9

Wear:
- batches total: 40
- `CONFIRMED`: 11
- `SENT`: 29
- readings: 462
- suspected `QUEUED`: 1

Estos son datos de QA y no datos de producción que deban reproducirse.

## Pendiente

- Prueba física terminal NACK.
- Recuperación controlada de históricos.
- Happy path nuevo HTTP 202 → ACK → DELIVERED.
- Prueba de pérdida/recuperación ACK.
- Suspected event físico.
- SOS/cancel físico.
- Caregiver Patient Detail HTTP 500.
- Caregiver Alerts backend no disponible.
- QA accessibility/light mode/font scaling pendiente.
- Gates finales release/signing.

## Advertencias

- No usar `connectedDebugAndroidTest` durante QA con sesión real.
- No limpiar Room/Wear outbox sin plan explícito.
- No abrir Wear durante recuperación histórica sin preparar el flujo.
- El suspected event histórico permanece en cuarentena.
- Este branch es WIP y no representa una release final.
