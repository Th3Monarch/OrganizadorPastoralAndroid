# Plan de Implementación: Inteligencia Artificial Gemini Real y Rediseño Móvil

## Resumen Ejecutivo
Transformar el Asistente Pastoral en una **Inteligencia Artificial generativa real** impulsada por el modelo **`gemini-3.5-flash`** (con comprensión de todo el contexto pastoral de la institución y respaldo offline), y **rediseñar la interfaz móvil** del panel de Inicio organizando las acciones rápidas y estadísticas en una cuadrícula ergonómica de 2 columnas para eliminar cortes de texto como «Asistent e» y «Pendien...».

---

## Decisiones Críticas y Preferencias del Usuario

> [!IMPORTANT]
> Confirmadas por el usuario en la fase de clarificación:
> 1. **Modo de IA**: Conexión a Gemini en la nube (`gemini-3.5-flash`) enviando el contexto pastoral en vivo (institución, responsable, agenda, tareas, notas y comunidad) para respuestas teológicas, oraciones, cronogramas y consultas pastorales reales, con respaldo local instantáneo ante fallos de conexión.
> 2. **Diseño en Pantallas Móviles**: Cuadrícula de 2 columnas con tarjetas completas y legibles, asegurando que ninguna etiqueta se trunque ni se divida en saltos de línea forzados.

---

## 1. Experiencia de Usuario y Diseño Visual

### Cuadrícula de Acciones Rápidas (2 Columnas)
En lugar de forzar 4 botones diminutos en una sola fila horizontal (lo que provocaba la rotura de «Asistente»), se implementará una cuadrícula balanceada de 2 columnas x 2 filas con tarjetas táctiles cómodas:
- **Fila 1**: `[💡 + Nueva Idea]` y `[📅 + Nuevo Evento]`
- **Fila 2**: `[✅ + Nueva Tarea]` y `[✨ Asistente IA]`
Cada tarjeta tendrá altura mínima de 48dp, icono representativo y texto completo en tipografía clara sin saltos de línea irregulares.

### Panel de Estadísticas y Recordatorios Sin Cortes
- Las estadísticas del dashboard se presentarán en tarjetas de 2 columnas con etiquetas completas:
  - **Notas e Ideas**
  - **Próximos Eventos**
  - **Tareas Pendientes** (mostrando la palabra completa sin truncar)
  - **Personas en Comunidad**
- Las tarjetas de recordatorios (*Vencidos*, *Hoy*, *Mañana*) contarán con ajuste de texto automático (`autoSize` / `fontSize` dinámico) para garantizar que en cualquier resolución se lean fluidamente.

### Experiencia del Asistente Gemini Real
- **Modo en Línea**: Cuando haya conexión, el asistente consulta directamente a la API de Gemini (`gemini-3.5-flash`) a través del cliente REST con OkHttp y Retrofit.
- **Contexto Pastoral en Vivo**: El prompt del sistema integra en tiempo real el nombre del responsable, colegio, eventos agendados, tareas pendientes y personas de la comunidad.
- **Capacidades Reales**:
  - Redacción de reflexiones para retiros y misas.
  - Generación de oraciones y dinámicas para grupos juveniles.
  - Planificación de reuniones y cronogramas pastorales.
  - Asistencia analítica y priorización sobre los datos cargados.
- **Modo Offline**: Si no hay conexión o no se detecta red, se activa el motor pastoral interno para procesar comandos nativos (`/hoy`, `/semana`, `/lista`, `/buscar`, `/plantillas`, `/exportar`) sin interrupciones.

---

## 2. Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                 MainPastoralApp (Jetpack Compose)           │
├──────────────────────────────┬──────────────────────────────┤
│       DashboardScreen        │       AssistantScreen        │
│  - Cuadrícula 2 col Acciones │  - Chat con streaming/espera │
│  - Cuadrícula 2 col Stats    │  - Chips rápidos /ayuda,/hoy │
│  - Sin cortes de texto       │  - Indicador de estado IA    │
└──────────────┬───────────────┴──────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐┌─────────────────────────────┐
│      PastoralRepository      ││   PastoralAssistantEngine   │
│  - pastoral-data.json local  ││  - Gemini REST Client       │
│  - 100% export/import        ││    (gemini-3.5-flash)       │
│  - StateFlow reactivo        ││  - Fallback Pastoral Local  │
└──────────────────────────────┘└─────────────────────────────┘
```

---

## 3. Plan de Modificaciones Técnicas

### Paso 1: Configuración de Dependencias y Permisos
- Declarar `<uses-permission android:name="android.permission.INTERNET" />` en `AndroidManifest.xml`.
- Descomentar la variable `GEMINI_API_KEY` en `.env.example` y configurar la lectura desde `BuildConfig.GEMINI_API_KEY`.

### Paso 2: Cliente de Inteligencia Artificial Gemini
- Implementar `GeminiApiClient` utilizando Retrofit, OkHttp (con timeouts de 60 segundos) y Moshi/Kotlinx Serialization.
- Utilizar el modelo oficial `gemini-3.5-flash` con instrucciones de sistema basadas en el manual pastoral del colegio.
- Inyectar el resumen de la base de datos pastoral (eventos, tareas, notas, comunidad) en las consultas para que la IA responda con conocimiento preciso de la labor del usuario.

### Paso 3: Rediseño de DashboardScreen
- Reemplazar la fila de acciones rápidas por filas de 2 columnas (`Row` con `weight(1f)`) con tarjetas elevadas.
- Reemplazar las insignias de estadísticas por tarjetas dobles legibles donde la palabra «Pendientes» se muestre en su totalidad.
- Optimizar la escala tipográfica en `ReminderCard` para pantallas de diferentes anchos.

### Paso 4: Verificación y Compilación
- Compilar la aplicación con `compile_applet`.
- Ejecutar el conjunto de pruebas unitarias y de Robolectric con `gradle :app:testDebugUnitTest`.
