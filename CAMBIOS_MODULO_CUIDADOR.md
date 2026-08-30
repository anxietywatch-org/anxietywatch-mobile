# Cambios del módulo de cuidador

## Objetivo

Preparar el módulo de cuidador para el Escenario B, en el que el backend todavía no ofrece los endpoints de lectura necesarios. La interfaz no muestra pacientes, eventos, métricas, ubicaciones ni alertas inventadas y queda estructurada para conectar datos reales en el Escenario A.

El backend confirmó posteriormente los contratos y el módulo pasó al Escenario A. Los estados vacíos descritos en este documento se conservan para respuestas sin datos, pero las fuentes principales ahora son HTTP y FCM reales.

## Auditoría realizada

Se revisaron las pantallas y rutas relacionadas con pacientes vinculados:

- Dashboard y lista de pacientes del cuidador.
- Detalle de paciente.
- Detalle de evento.
- Alerta crítica.
- Guía de apoyo.
- Navegación entre estas pantallas.
- Flujo inverso relacionado con “Mi cuidador” y “Compartir con cuidador”.
- Fuentes HTTP, DTO, ViewModels y posibles accesos inseguros a listas.

Además de los datos de ejemplo conocidos, se encontraron estos problemas:

- El dashboard enviaba el nombre del paciente al navegar en lugar de su ID.
- Cualquier ID desconocido o vacío se interpretaba como Alex.
- La alerta crítica inventaba identidad, género, crisis, ubicación y teléfono de emergencia.
- “Compartir con cuidador” y “Ver ubicación” eran acciones visibles sin implementación.
- La guía mostraba un ID de evento que no utilizaba.
- Los IDs se insertaban en rutas sin codificación URI.
- La gráfica de frecuencia cardíaca dependía de dos listas paralelas y podía acceder a una etiqueta inexistente.
- La ruta de alerta crítica no tiene actualmente una entrada real desde navegación, deep link o notificaciones.

## Datos de ejemplo eliminados

Se eliminaron del runtime del cuidador:

- María como nombre fijo del cuidador.
- Alex y Sofía como pacientes simulados.
- Estados de calma o actividad elevada.
- BPM y tiempos de sincronización hardcodeados.
- “Intervenciones hoy: 2”.
- Historial ficticio de intervenciones.
- Eventos de crisis, respiración y ritmo elevado de ejemplo.
- Gráficas y horarios de BPM simulados.
- Estado fijo “Estable”.
- Duración, intensidad, BPM pico y recuperación inventados.
- Notas del sistema y etiquetas analíticas simuladas.
- “Parque Central” y la tarjeta de ubicación placeholder.
- IDs técnicos visibles en las pantallas.
- El botón “Compartir con cuidador” sin implementación.
- El teléfono fijo `112` dentro de la alerta del cuidador.

El `tel:112` que todavía existe en el proyecto pertenece al flujo de crisis del propio paciente y no al módulo de cuidador.

## Estados asíncronos comunes

Se creó `app/src/main/java/com/anxietywatch/mobile/ui/common/AsyncStateContent.kt`.

Incluye:

- `AsyncUiState.Loading`.
- `AsyncUiState.Empty`.
- `AsyncUiState.Success<T>`.
- `AsyncUiState.Error`.
- `LoadingState`, con indicador de progreso y mensaje.
- `EmptyState`, con tarjeta, icono, título y explicación.
- `ErrorState`, con icono de error y botón “Reintentar”.

Estos componentes siguen los colores, tarjetas y tipografía Material 3 ya utilizados en la aplicación.

## Dashboard del cuidador

Archivos:

- `app/src/main/java/com/anxietywatch/mobile/ui/dashboard/DashboardCaregiverViewModel.kt`
- `app/src/main/java/com/anxietywatch/mobile/ui/dashboard/DashboardCaregiverScreen.kt`

Cambios:

- El ViewModel ya no construye pacientes ni telemetría localmente.
- En Escenario B, `loadDashboard()` termina en `AsyncUiState.Empty`.
- El estado vacío muestra “No hay pacientes vinculados todavía”.
- El estado de carga muestra un indicador de progreso.
- El estado de error permite reintentar.
- Una respuesta futura `Success` con una lista vacía también muestra el estado vacío.
- Los campos de estado, BPM y última sincronización son opcionales y solo aparecen cuando existen.
- La navegación utiliza `patient.id`, no `patient.name`.
- Se eliminaron los resúmenes e intervenciones ficticias.

## Vinculación provisional de pacientes

Se agregó una sección “Vincular nuevo paciente” al dashboard. Es visible debajo del contenido tanto cuando no hay pacientes como cuando existe una lista real.

La sección incluye:

- `OutlinedTextField` con label “Código”.
- Placeholder `ANX-XXXXXX`.
- Botón “Vincular”.
- Saneo en vivo a mayúsculas.
- Solo permite letras, números y guion.
- Límite visual de 20 caracteres.
- Validación local de longitud entre 4 y 20 caracteres.
- Mensaje de error debajo del campo.
- Desactivación del campo y botón durante `Loading`.
- Descarte del error cuando el usuario vuelve a editar, conservando el código introducido.

Se creó `LinkPatientUiState` con:

- `Idle`.
- `Loading`.
- `Error(message)`.
- `Success`.

`linkPatient(code)` no realiza ninguna llamada HTTP. Si el código es válido, muestra:

> Esta función todavía no está disponible. Vuelve a intentar más tarde.

El código contiene el TODO acordado:

```kotlin
// TODO: reemplazar por POST /api/caregiver/patients/link cuando backend lo confirme, ver conversación con Persona 1
```

No se reutilizó `AnxietyWatchApi.acceptByCode()`, porque ese endpoint reemplaza la sesión activa y está destinado al ingreso inicial.

## Detalle de paciente

Archivos:

- `app/src/main/java/com/anxietywatch/mobile/ui/wellness/PatientDetailViewModel.kt`
- `app/src/main/java/com/anxietywatch/mobile/ui/wellness/PatientDetailScreen.kt`

Cambios:

- Se creó un ViewModel independiente con `StateFlow` público.
- En Escenario B, `loadPatient(patientId)` termina en estado vacío.
- No se deduce el nombre u otro dato a partir del ID.
- Se agregaron estados de carga, vacío y error con reintento.
- Se crearon modelos para paciente, muestras cardíacas y eventos reales futuros.
- Los campos opcionales solo se renderizan si están presentes.
- Una lista de eventos vacía muestra “No hay eventos registrados”.
- La gráfica usa objetos que contienen valor y etiqueta, evitando índices entre listas paralelas.
- La selección de barras se mantiene acotada y segura.

## Detalle de evento

Archivos:

- `app/src/main/java/com/anxietywatch/mobile/ui/events/EventDetailViewModel.kt`
- `app/src/main/java/com/anxietywatch/mobile/ui/events/EventDetailScreen.kt`

Cambios:

- Se creó un ViewModel independiente con `StateFlow` público.
- En Escenario B, `loadEvent(eventId)` termina en estado vacío.
- Se agregaron estados de carga, vacío y error con reintento.
- Se crearon modelos para título, categoría, fecha, resumen, métricas, ubicación, notas y etiquetas.
- Cada sección solo se muestra cuando el dato real existe.
- Se eliminaron la ubicación placeholder, métricas, notas y etiquetas simuladas.
- Se eliminó el botón de compartir sin implementación.
- El ID del evento dejó de mostrarse en la interfaz.

## Alerta crítica

Archivos:

- `app/src/main/java/com/anxietywatch/mobile/ui/alerts/CriticalAlertViewModel.kt`
- `app/src/main/java/com/anxietywatch/mobile/ui/alerts/CriticalAlertScreen.kt`

Cambios:

- Se creó un ViewModel independiente con `StateFlow` público.
- En Escenario B, `loadAlert(eventId)` termina en estado vacío.
- Se agregaron estados de carga, vacío y error con reintento.
- El estado vacío permite volver a la pantalla anterior.
- La identidad, el mensaje, la ubicación y el teléfono proceden ahora de un modelo de datos.
- La ubicación y el botón de llamada solo aparecen si llegan valores reales.
- El teléfono se codifica antes de construir la URI del marcador.
- Se eliminó la acción de ubicación sin implementación.
- Se eliminó el ID visible.

## Guía de apoyo

Archivos:

- `app/src/main/java/com/anxietywatch/mobile/ui/support/SupportGuideScreen.kt`
- `app/src/main/java/com/anxietywatch/mobile/navigation/Routes.kt`
- `app/src/main/java/com/anxietywatch/mobile/navigation/AnxietyWatchNavHost.kt`

Cambios:

- La guía dejó de recibir y mostrar un `eventId`.
- Su ruta ahora es `support_guide`, sin argumento.
- Se considera contenido editorial genérico y no una pantalla de datos asíncronos.
- La alerta navega directamente a la ruta editorial.

## Navegación segura

Cambios en `Routes.kt` y `AnxietyWatchNavHost.kt`:

- `PatientDetail.build(patientId)` usa `Uri.encode(patientId)`.
- `EventDetail.build(eventId)` usa `Uri.encode(eventId)`.
- El dashboard envía el ID real del paciente.
- IDs vacíos o desconocidos ya no generan datos falsos; las pantallas terminan en estado vacío.

## Pruebas unitarias

Se crearon estos archivos JUnit4:

- `app/src/test/java/com/anxietywatch/mobile/ui/dashboard/DashboardCaregiverViewModelTest.kt`
- `app/src/test/java/com/anxietywatch/mobile/ui/wellness/PatientDetailViewModelTest.kt`
- `app/src/test/java/com/anxietywatch/mobile/ui/events/EventDetailViewModelTest.kt`
- `app/src/test/java/com/anxietywatch/mobile/ui/alerts/CriticalAlertViewModelTest.kt`

Las pruebas verifican:

- Estado inicial `Loading` en paciente, evento y alerta.
- Estado inicial observable `Empty` en dashboard, porque ejecuta la carga síncrona desde `init`.
- Estado final `Empty` después de cada método de carga.
- Validación de un código de vinculación inválido.
- Saneo de un código válido y respuesta de función todavía no disponible.
- Retorno de `Error` a `Idle` al descartar el error de vinculación.

No se agregaron:

- Pruebas de Compose UI.
- Reflection.
- Turbine.
- `kotlinx-coroutines-test`.
- Dependencias nuevas.

## Verificaciones ejecutadas

```text
cmd.exe /c gradlew.bat :app:compileDebugKotlin
BUILD SUCCESSFUL
```

```text
cmd.exe /c gradlew.bat :app:testDebugUnitTest
BUILD SUCCESSFUL
```

También se buscaron los nombres, IDs, métricas, ubicaciones y textos de ejemplo eliminados para comprobar que no permanecieran en el flujo de cuidador.

## Firebase Cloud Messaging

Se agregó la configuración de Firebase necesaria para recibir alertas del cuidador:

- Plugin Google Services `4.4.4` en el catálogo y los Gradle de raíz y `app`.
- Firebase BoM `34.5.0`.
- Dependencia `firebase-messaging` administrada por el BOM.
- `app/google-services.json` excluido mediante `.gitignore`.

Se usa `firebase-messaging` en lugar del módulo retirado `firebase-messaging-ktx`, ya que Firebase BoM 34 dejó de administrar los artefactos KTX separados.

### Servicio de push

Se creó `app/src/main/java/com/anxietywatch/mobile/push/CaregiverPushService.kt` y se registró en `AndroidManifest.xml` para `com.google.firebase.MESSAGING_EVENT`.

El servicio:

- Extiende `FirebaseMessagingService`.
- Recibe renovaciones mediante `onNewToken`.
- Registra el token mediante `POST api/devices/register` usando el JWT activo.
- Ejecuta el registro en un `CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
- Ya no registra el token FCM en Logcat.
- Procesa mensajes mediante `onMessageReceived`.
- Crea el canal `critical_alerts` con importancia alta y vibración.
- Usa el título y cuerpo recibidos o mensajes genéricos que no afirman datos clínicos.
- Comprueba `POST_NOTIFICATIONS` en Android 13 o superior.
- Abre `MainActivity` con `eventId`, `patientName`, `alertMessage`, ubicación y teléfono opcionales.

### Navegación desde notificaciones

- `MainActivity` procesa el intent inicial y los intents recibidos mediante `onNewIntent` por su modo `singleTop`.
- `AnxietyWatchNavHost` consume el evento tanto con la app cerrada como abierta.
- `Routes.CriticalAlert` dispone de un builder que codifica el ID con `Uri.encode`.
- La alerta solo se abre si hay una sesión válida con rol `family_member`.
- Si falta `eventId`, la notificación abre la aplicación sin fabricar un identificador.
- Si la sesión no es válida o pertenece a otro rol, se mantiene el flujo normal de autenticación o paciente.
- Los datos personales no forman parte de la ruta de Navigation; se mantienen como estado interno.
- `CriticalAlertViewModel` muestra el payload real como `AsyncUiState.Success`.

## Integración del Escenario A

### Dashboard

- `GET api/caregiver/patients` carga pacientes vinculados reales.
- Una lista vacía produce `AsyncUiState.Empty`.
- Cada respuesta se mapea de `patientId/fullName` a `id/name`.
- Estado, BPM y sincronización permanecen opcionales; no se inventan si la lista no viene enriquecida.

### Vinculación

- `POST api/caregiver/patients/link` usa el JWT activo y no reemplaza la sesión.
- Se mantienen el saneo y la validación local del código.
- Se mapean `404`, `409` y `429` a mensajes específicos.
- En éxito se muestra confirmación, se limpia el campo y se recarga el dashboard.

### Detalle de paciente

- Se consultan detalle, episodios, telemetría latest y eventos por `patientId`.
- La última telemetría se mapea a la gráfica sin simular muestras adicionales.
- Los eventos y episodios se presentan sin duplicar IDs.
- `403/404` producen estado vacío; otros fallos producen un error reintentable.

### Detalle de evento

- La navegación conserva `patientId` y `eventId`.
- Se consulta `GET api/caregiver/patients/{patientId}/events` y se selecciona el evento por ID.
- No se inventó un endpoint individual inexistente.
- Las secciones opcionales solo aparecen cuando la respuesta contiene datos.

### Alertas críticas

- FCM entrega el evento y los datos necesarios para `CriticalAlertUiModel`.
- La navegación crítica requiere una sesión válida de `family_member`.
- Ubicación y llamada solo aparecen cuando el payload las incluye.

## Pendientes restantes

- Enriquecer las tarjetas del dashboard con `telemetry/latest` puede hacerse en una segunda pasada.
- No existe un logout visible del cuidador; por eso `POST api/devices/unregister` está declarado pero todavía no tiene un punto seguro de invocación.
- La guía no requiere endpoint mientras permanezca como contenido editorial genérico.
- Añadir pruebas de ViewModels asíncronos cuando el proyecto incorpore soporte de dispatcher de pruebas.

## Restricciones respetadas

- No se agregaron llamadas de red simuladas.
- No se reutilizó el endpoint de ingreso por código.
- No se agregaron datos clínicos o personales de ejemplo.
- No se añadieron acciones que no tengan implementación real.
