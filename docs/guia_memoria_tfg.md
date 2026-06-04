# Guía de redacción de la memoria del TFG

Esta guía está adaptada a la estructura que te están pidiendo para la memoria. La idea es que tú redactes el texto final, pero que tengas claro qué debe ir en cada apartado, qué partes de la aplicación conviene mencionar y qué elementos técnicos pueden reforzar la explicación.

El proyecto desarrollado es una aplicación Android para analizar ruido ambiental en tiempo real, guardar mediciones geolocalizadas, visualizarlas en un mapa de calor, consultar un histórico y generar explicaciones mediante IA.

---

# 1. Introducción

## Qué debe cumplir este apartado

La introducción debe exponer el trabajo de forma global y sencilla. Una persona que no conozca el proyecto debería entender:

- cuál es el tema;
- qué problema general aborda;
- qué tipo de solución se ha desarrollado;
- qué funcionalidades principales tiene;
- cómo se organiza la memoria.

No es el sitio para explicar código ni arquitectura en detalle.

## Enfoque recomendado

Presenta el trabajo como una aplicación Android de análisis acústico ambiental:

- captura audio del micrófono;
- calcula niveles sonoros;
- muestra espectro de frecuencias;
- permite guardar muestras geolocalizadas;
- representa los datos en un mapa de calor;
- conserva un histórico;
- incorpora IA local para clasificación de sonidos;
- usa IA generativa para explicar mediciones.

## Posible estructura

### 1.1. Contexto general

Habla del ruido ambiental como un fenómeno presente en entornos urbanos, laborales y domésticos.

Ideas:

- afecta al bienestar, descanso y concentración;
- no siempre es fácil medirlo o interpretarlo;
- los datos acústicos aislados pueden ser difíciles de entender sin contexto.

### 1.2. Presentación del trabajo

Explica que el TFG consiste en desarrollar una app móvil que permite analizar y registrar ruido ambiental.

### 1.3. Resumen de funcionalidades

Menciona brevemente:

- analizador en tiempo real;
- mapa de calor;
- histórico;
- ajustes;
- YAMNet;
- Gemini.

### 1.4. Organización de la memoria

Incluye un párrafo al final indicando qué se explica en los siguientes apartados.

## Frase útil

> Este Trabajo de Fin de Grado consiste en el desarrollo de una aplicación Android orientada al análisis de ruido ambiental. La aplicación permite capturar audio en tiempo real, calcular niveles sonoros, visualizar el espectro de frecuencias, guardar mediciones geolocalizadas y representar los datos sobre un mapa de calor. Además, incorpora técnicas de inteligencia artificial para clasificar sonidos ambientales y generar explicaciones comprensibles de las mediciones almacenadas.

## Qué evitar

No digas todavía:

- cómo funcionan los DAOs;
- qué clases hay;
- cómo está implementado Room;
- detalles internos de Gemini;
- código fuente.

Tampoco digas que sustituye a un sonómetro profesional. Mejor:

> La aplicación se plantea como una herramienta de análisis y apoyo, no como un instrumento certificado de medición acústica.

---

# 2. Motivación

## Qué debe cumplir este apartado

La motivación debe dejar claro por qué el tema es importante y por qué tiene sentido hacer este TFG. Debe servir como base para los objetivos.

Puedes plantearla desde tres perspectivas:

1. Motivación social o práctica.
2. Motivación técnica.
3. Motivación personal/académica.

## 2.1. Motivación social o práctica

Ideas que puedes usar:

- el ruido ambiental es un problema cotidiano;
- muchas personas no tienen herramientas para registrar o comparar niveles de ruido;
- visualizar mediciones sobre un mapa aporta contexto;
- explicar una medición en lenguaje natural ayuda a usuarios no expertos.

Relación con tu app:

| Motivación | Parte de la app |
|---|---|
| Conocer el entorno acústico | Analizador en tiempo real |
| Contextualizar mediciones | Geolocalización + mapa |
| Revisar mediciones pasadas | Historial |
| Entender datos técnicos | Explicación IA |
| Detectar tipo de ruido | Clasificación YAMNet |

## 2.2. Motivación técnica

Tu proyecto es interesante técnicamente porque combina:

- desarrollo Android moderno;
- procesado digital de señal;
- bases de datos locales;
- mapas;
- modelos de IA locales;
- IA generativa;
- arquitectura por capas.

Frase útil:

> Desde el punto de vista técnico, el proyecto permite integrar distintas áreas de la informática, como el desarrollo móvil, el procesamiento de señal, la persistencia local, la geolocalización y la inteligencia artificial aplicada.

## 2.3. Motivación personal o académica

Puedes hablar de:

- interés por Android;
- interés por construir una app completa;
- interés por IA aplicada;
- interés por proyectos con utilidad real;
- oportunidad de aplicar conocimientos del grado.

## Cierre del apartado

El final debe conducir a los objetivos:

> Estas motivaciones conducen al planteamiento de una solución móvil capaz de medir, registrar, visualizar e interpretar ruido ambiental de forma accesible para el usuario.

---

# 3. Objetivos

## Qué debe cumplir este apartado

Los objetivos deben ser claros, verificables y relacionados con el resultado final conseguido.

Te recomiendo plantear:

- un objetivo general;
- varios objetivos específicos.

## Objetivo general recomendado

> Diseñar e implementar una aplicación Android para la medición, análisis, almacenamiento geolocalizado, visualización e interpretación de ruido ambiental mediante técnicas de procesado digital de señal e inteligencia artificial.

## Objetivos específicos recomendados

### OE1. Captura y análisis de audio

Implementar un sistema de captura de audio desde el micrófono del dispositivo que permita analizar la señal en tiempo real.

Partes de la app:

- `data/audio/AudioCaptureManager.kt`
- `ui/analyzer/AnalyzerViewModel.kt`

### OE2. Cálculo de niveles sonoros y ponderaciones

Calcular niveles sonoros y permitir el uso de ponderaciones A, C y Z.

Partes:

- `dsp/FFTCalculator.kt`
- `domain/model/WeightingType.kt`
- selector de ponderación en el analizador.

### OE3. Visualización del espectro

Mostrar el espectro de frecuencias de la señal capturada de forma visual.

Partes:

- `ui/analyzer/LogarithmicSpectrumAnalyzer.kt`
- `dsp/SpectrumWeighting.kt`

### OE4. Registro de muestras geolocalizadas

Guardar muestras acústicas junto con información temporal, geográfica, espectral y de clasificación.

Partes:

- `domain/repository/AudioRepository.kt`
- `data/repository/RoomAudioRepository.kt`
- Room.

### OE5. Representación geográfica

Visualizar las mediciones almacenadas sobre un mapa de calor.

Partes:

- `ui/map/MapScreen.kt`
- `ui/map/MapViewModel.kt`
- MapLibre.

### OE6. Consulta histórica

Permitir consultar, desplegar, plegar y borrar muestras guardadas.

Partes:

- `ui/history/HistoryScreen.kt`
- `ui/history/HistoryViewModel.kt`
- `domain/repository/HistoryRepository.kt`

### OE7. Inteligencia artificial

Integrar IA local para clasificación de sonidos e IA generativa para explicación de mediciones.

Partes:

- `data/ml/SoundClassifierManager.kt`
- `data/ai/NoiseExplanationService.kt`
- `assets/yamnet.tflite`

### OE8. Configuración de análisis

Permitir modificar parámetros de análisis, como tamaño de buffer y offset de calibración.

Partes:

- `ui/settings/SettingsScreen.kt`
- `data/settings/AppSettings.kt`

### OE9. Arquitectura mantenible

Organizar el proyecto con MVVM y separación por capas inspirada en Clean Architecture.

Partes:

- `ui`
- `domain`
- `data`
- `dsp`

## Tabla objetivo-resultado

Incluye una tabla parecida:

| Objetivo | Resultado final |
|---|---|
| Captura de audio | Analizador funcional en tiempo real |
| Ponderaciones | Selector A/C/Z y explicación contextual |
| Espectro | Gráfica logarítmica con peak hold |
| Muestras geolocalizadas | Guardado en Room con ubicación |
| Mapa | Heatmap con filtros |
| Histórico | Lista de muestras desplegables |
| IA | YAMNet y Gemini integrados |
| Ajustes | Buffer y offset configurables |
| Arquitectura | Separación `ui`, `domain`, `data`, `dsp` |

Esta tabla será muy útil para después redactar conclusiones.

---

# 4. Estado del arte / Contexto tecnológico / Estudio estratégico

## Qué debe cumplir este apartado

Aquí debes documentar aplicaciones o soluciones existentes que hagan cosas parecidas y justificar por qué tu camino elegido tiene sentido.

No se trata solo de listar apps. Debes comparar, sacar conclusiones y explicar qué aporta tu propuesta.

## 4.1. Tipos de soluciones existentes

Puedes organizar el estudio en categorías.

### A. Apps de sonómetro básico

Aplicaciones orientadas a mostrar un nivel en dB en tiempo real.

Ejemplos a investigar:

- Sound Meter;
- Decibel X;
- Sound Meter Pro;
- apps genéricas de medidor de decibelios.

Qué suelen ofrecer:

- nivel actual;
- máximo/mínimo;
- gráficas simples;
- calibración básica;
- interfaz directa.

Limitaciones típicas:

- poca contextualización geográfica;
- histórico limitado;
- sin explicación de resultados;
- precisión dependiente del micrófono;
- pocas opciones de análisis avanzado.

### B. Apps de medición más técnica o profesional

Ejemplo:

- NIOSH Sound Level Meter.

Qué aporta:

- enfoque más riguroso;
- parámetros acústicos más profesionales;
- posible orientación a salud laboral.

Limitaciones para tu enfoque:

- puede ser menos visual/geográfica;
- no necesariamente orientada a mapas de ruido personalizados;
- no integra IA generativa.

### C. Mapas colaborativos de ruido

Ejemplos a investigar:

- NoiseCapture;
- proyectos de mapas de ruido ciudadanos;
- mapas municipales de ruido.

Qué ofrecen:

- geolocalización;
- mapas;
- participación ciudadana;
- análisis espacial.

Limitaciones:

- pueden requerir backend o infraestructura colaborativa;
- no siempre ofrecen análisis local detallado;
- no siempre incluyen explicación individual de cada muestra.

### D. Herramientas de análisis acústico

Herramientas más técnicas:

- analizadores FFT;
- software de audio;
- herramientas de laboratorio.

Qué ofrecen:

- análisis espectral;
- visualización avanzada;
- mayor precisión si se usan equipos adecuados.

Limitaciones:

- menos accesibles para usuarios no expertos;
- no integradas en una app móvil simple;
- no centradas en geolocalización ni explicación automática.

## 4.2. Tabla comparativa recomendada

Puedes crear una tabla así:

| Solución | dB en tiempo real | Espectro | Mapa | Histórico | Clasificación IA | Explicación IA | Ajustes |
|---|---:|---:|---:|---:|---:|---:|---:|
| Sound Meter | Sí | Limitado | No | Limitado | No | No | Básicos |
| Decibel X | Sí | Parcial | No/limitado | Sí | No | No | Sí |
| NIOSH SLM | Sí | Técnico | No | Sí | No | No | Sí |
| NoiseCapture | Sí | No/limitado | Sí | Sí | No | No | Parcial |
| App desarrollada | Sí | Sí | Sí | Sí | Sí | Sí | Sí |

Importante: verifica las funcionalidades exactas de cada app antes de entregar.

## 4.3. Alternativas tecnológicas consideradas

Aquí puedes justificar elecciones.

### Android nativo frente a web

Opción elegida: Android nativo.

Justificación:

- acceso directo al micrófono;
- mejor integración con ubicación;
- ejecución local de TensorFlow Lite;
- persistencia local;
- mejor experiencia móvil.

### Room frente a almacenamiento simple

Opción elegida: Room.

Justificación:

- estructura relacional;
- consultas;
- histórico;
- relación entre muestras, bandas y clasificaciones;
- migraciones.

### MapLibre frente a otras opciones

Opción elegida: MapLibre.

Justificación:

- mapas vectoriales;
- capas heatmap;
- personalización;
- alternativa abierta.

### TensorFlow Lite frente a clasificación remota

Opción elegida: TensorFlow Lite/YAMNet local.

Justificación:

- no enviar audio a servidores;
- menor dependencia de conexión;
- clasificación en dispositivo;
- modelo preentrenado adecuado para audio ambiental.

### Gemini frente a explicación manual

Opción elegida: Gemini bajo demanda.

Justificación:

- transforma datos técnicos en texto comprensible;
- mejora accesibilidad;
- no afecta al análisis en tiempo real porque se usa desde el histórico.

## 4.4. Conclusión del estado del arte

Debes terminar explicando qué hueco intenta cubrir tu app:

> Tras analizar soluciones existentes, se observa que muchas aplicaciones permiten medir niveles sonoros en tiempo real, y otras se centran en mapas o en análisis técnico. Sin embargo, la solución desarrollada combina en una misma aplicación análisis en tiempo real, espectro, geolocalización, histórico, clasificación local de sonidos y explicación generativa de las mediciones.

---

# 5. Análisis del problema

## Qué debe cumplir este apartado

Aquí tienes que analizar el problema de forma sistemática. No basta con decir "quiero hacer una app de ruido".

Debes identificar:

- necesidades;
- usuarios;
- limitaciones;
- oportunidades;
- riesgos;
- requisitos derivados.

## 5.1. Problema principal

Puedes formularlo así:

> El usuario puede percibir que un entorno es ruidoso, pero normalmente no dispone de una herramienta sencilla que le permita medir, registrar, contextualizar e interpretar ese ruido de forma integrada.

## 5.2. Necesidades detectadas

Lista recomendada:

1. Medir ruido de forma inmediata.
2. Conocer cómo cambia el ruido en el tiempo.
3. Relacionar mediciones con ubicación.
4. Consultar mediciones anteriores.
5. Entender qué tipo de sonido puede estar presente.
6. Interpretar datos técnicos sin conocimientos acústicos.
7. Ajustar la medición al dispositivo.

## 5.3. Usuarios potenciales

Puedes definir perfiles:

### Usuario general

Quiere saber si un entorno es ruidoso.

### Estudiante o investigador

Quiere registrar muestras para análisis.

### Persona interesada en su entorno urbano

Quiere observar zonas más ruidosas.

### Usuario no experto

Necesita explicaciones simples.

## 5.4. Limitaciones del problema

Muy importante:

- los micrófonos de móviles no son iguales;
- no hay calibración profesional automática;
- el ruido ambiental es variable;
- la ubicación puede tener error;
- los modelos de IA pueden equivocarse;
- Gemini requiere conexión.

## 5.5. Análisis DAFO opcional

Puede quedar muy bien.

| Fortalezas | Debilidades |
|---|---|
| App integrada y accesible | Precisión dependiente del móvil |
| Análisis en tiempo real | No es sonómetro certificado |
| Mapa e histórico | Requiere permisos |
| IA local y generativa | Gemini necesita conexión |

| Oportunidades | Amenazas |
|---|---|
| Interés en ruido urbano | Comparación con apps ya existentes |
| Uso educativo o ciudadano | Restricciones de permisos Android |
| Ampliación con exportación de datos | Variabilidad de hardware |

## 5.6. Resultado del análisis

El análisis debe justificar la solución:

> A partir de estas necesidades se plantea una aplicación móvil que no solo mida ruido, sino que lo almacene, lo geolocalice, lo visualice y ayude a interpretarlo.

---

# 6. Solución propuesta

## Qué debe cumplir este apartado

Debes presentar la solución elegida:

- en qué consiste;
- qué fases tendrá;
- cómo se implantará;
- cómo se validará.

## 6.1. Descripción de la solución

La solución es una aplicación Android con cuatro módulos funcionales:

1. Analizador acústico.
2. Mapa de calor.
3. Historial de mediciones.
4. Ajustes y calibración.

Además, integra:

- clasificación local YAMNet;
- explicación con Gemini;
- persistencia Room.

## 6.2. Fases del desarrollo

Puedes presentarlas así:

| Fase | Descripción |
|---|---|
| F1 | Investigación de ruido, FFT, ponderaciones y tecnologías Android |
| F2 | Captura de audio y cálculo de niveles |
| F3 | Visualización del espectro |
| F4 | Persistencia local con Room |
| F5 | Geolocalización y mapa de calor |
| F6 | Clasificación YAMNet |
| F7 | Histórico y explicación IA |
| F8 | Ajustes y calibración |
| F9 | Refactorización arquitectónica |
| F10 | Pruebas y documentación |

## 6.3. Implantación prevista

Como es una app Android:

- se instala en un dispositivo o emulador;
- solicita permisos;
- usa el micrófono y ubicación;
- guarda datos localmente;
- usa conexión para mapa y Gemini.

No hay backend propio.

## 6.4. Validación prevista

Pruebas:

- compilación;
- pruebas de permisos;
- captura de audio;
- guardado de muestras;
- visualización de historial;
- mapa;
- explicación IA;
- ajustes.

---

# 7. Diseño de la solución

Este apartado debe dividirse en arquitectura y diseño detallado.

---

## 7.1. Arquitectura

### Qué debes explicar

Grandes bloques del sistema:

```text
UI
Domain
Data
DSP
```

### Diagrama recomendado

```text
Pantallas Compose
      |
ViewModels
      |
Repositorios de dominio
      |
Implementaciones data
      |
Room / AudioRecord / Location / TFLite / Gemini
```

### Relación con MVVM

Explica:

```text
Screen -> ViewModel -> Repository/Service
```

Ejemplos:

```text
HistoryScreen -> HistoryViewModel -> HistoryRepository
MapScreen -> MapViewModel -> AudioRepository
AnalyzerScreen -> AnalyzerViewModel
```

### Relación con Clean Architecture

Frase recomendada:

> La aplicación utiliza MVVM como patrón de presentación y una organización por capas inspirada en Clean Architecture. La separación no es completamente estricta en todos los módulos, pero permite aislar gran parte de la lógica de datos e infraestructura.

## 7.2. Diseño detallado de subsistemas

### Subsistema de audio

Componentes:

- `AudioCaptureManager`;
- `FFTCalculator`;
- `SpectrumWeighting`;
- `ThirdOctaveCalculator`;
- `AnalyzerViewModel`.

Responsabilidad:

- capturar audio;
- calcular dB;
- calcular espectro;
- preparar datos para UI y guardado.

### Subsistema de persistencia

Componentes:

- `AppDatabase`;
- `AudioSampleDao`;
- `FrequencyBinDao`;
- `SoundClassificationDao`;
- `GeoTileDao`;
- `RoomAudioRepository`;
- `RoomHistoryRepository`.

Responsabilidad:

- guardar muestras;
- consultar histórico;
- obtener datos para mapa.

### Subsistema de mapa

Componentes:

- `MapScreen`;
- `MapViewModel`;
- MapLibre.

Responsabilidad:

- visualizar mediciones;
- aplicar filtros;
- mostrar heatmap.

### Subsistema de IA

Componentes:

- `SoundClassifierManager`;
- `NoiseExplanationService`.

Responsabilidad:

- clasificación local;
- explicación generativa.

### Subsistema de configuración

Componentes:

- `SettingsScreen`;
- `AppSettings`.

Responsabilidad:

- buffer;
- offset.

## 7.3. Modelo de datos

Incluye una descripción de tablas:

```text
AudioSample
FrequencyBin
SoundClassification
GeoTile
```

Relaciones:

```text
AudioSample 1---N FrequencyBin
AudioSample 1---N SoundClassification
```

## Código útil para diseño

Interfaz:

```kotlin
interface AudioRepository {
    suspend fun saveCompleteAudioSample(...)
    suspend fun getHeatmapData(...): List<HeatmapTile>
}
```

Explica que permite separar la UI de Room.

---

# 8. Desarrollo de la solución

## Qué debe cumplir este apartado

Aquí debes explicar cómo se pasó de la propuesta inicial a la solución final, problemas encontrados, decisiones tomadas y particularidades.

No pongas mucho código. Muestra solo partes relevantes.

## 8.1. Desarrollo del analizador

Cuenta:

- primero se implementó captura de audio;
- luego FFT;
- luego ponderaciones;
- luego espectro;
- luego captura de muestras;
- luego clasificación.

Problemas/dificultades que puedes mencionar:

- gestión de permisos;
- buffer de audio;
- actualización en tiempo real;
- suavizado visual;
- peak hold;
- calibración mediante offset.

Fragmento relevante:

```kotlin
val results = fftCalculator.calculateWeightings(
    audioBuffer,
    SAMPLE_RATE,
    _uiState.value.offset
)
```

## 8.2. Desarrollo del guardado de muestras

Cuenta:

- se creó Room;
- se guardó muestra principal;
- se añadieron bandas;
- se añadieron clasificaciones;
- se añadió explicación IA.

Fragmento:

```kotlin
repository.saveCompleteAudioSample(
    avgDb = avgDb.toFloat(),
    peakDb = captureMaxDb.toFloat(),
    latitude = location?.latitude,
    longitude = location?.longitude,
    spectralEnergy = thirdOctaveBands,
    labels = labels,
    dominantFreq = dominantFreq,
    weighting = state.selectedWeighting.name
)
```

## 8.3. Desarrollo del mapa

Cuenta:

- integración MapLibre;
- creación de fuente GeoJSON;
- capa heatmap;
- filtros;
- permisos de ubicación.

Problema interesante:

- el mapa inicialmente puede depender de datos agregados;
- se decidió usar muestras reales para histórico/mapa global;
- se añadió selector temporal.

## 8.4. Desarrollo del histórico

Cuenta:

- listado de muestras;
- detalle desplegable;
- borrado;
- plegado al pulsar de nuevo;
- explicación IA.

Fragmento:

```kotlin
if (_selectedSampleDetails.value?.sample?.id == sample.id) {
    _selectedSampleDetails.value = null
    return
}
```

## 8.5. Desarrollo de IA

Cuenta:

- YAMNet se usa localmente;
- Gemini se usa bajo demanda;
- se diseñó un prompt con datos técnicos.

Fragmento:

```kotlin
val strongestBands = bins
    .sortedByDescending { it.energy }
    .take(5)
```

## 8.6. Refactorización arquitectónica

Este apartado puede quedar muy bien.

Cuenta que durante el desarrollo se reorganizó el proyecto:

- `AudioRepository` pasó a ser interfaz de dominio;
- Room quedó en `data/repository`;
- histórico dejó de usar Room directamente;
- Gemini se movió a `data/ai`;
- YAMNet a `data/ml`;
- audio a `data/audio`;
- ubicación a `data/location`;
- ajustes a `data/settings`;
- herramientas DSP fuera del ViewModel.

Esto demuestra evolución y criterio técnico.

---

# 9. Implantación

## Qué debe cumplir este apartado

Se explica cómo se lleva la solución a un entorno donde pueda probarse.

En tu caso, la implantación es la instalación/ejecución de la app Android en dispositivo o emulador.

## Qué contar

### 9.1. Entorno de desarrollo

Incluye:

- Android Studio;
- Kotlin;
- Gradle;
- SDK Android;
- dispositivo/emulador.

### 9.2. Configuración necesaria

Menciona:

- `google-services.json` para Firebase AI;
- modelo `yamnet.tflite` en assets;
- permisos Android;
- conexión para mapa y Gemini.

### 9.3. Construcción

Comando:

```powershell
.\gradlew.bat assembleDebug
```

### 9.4. Puesta en marcha

Pasos:

1. Instalar APK.
2. Abrir app.
3. Aceptar permisos.
4. Iniciar análisis.
5. Capturar muestra.
6. Consultar mapa/histórico.

### 9.5. Datos locales

Explica que los datos se guardan en Room y permanecen en el dispositivo.

---

# 10. Pruebas

## Qué debe cumplir este apartado

Debes presentar pruebas para verificar que funciona correctamente y, si procede, validación de usuario.

## 10.1. Pruebas de compilación

Comando:

```powershell
.\gradlew.bat assembleDebug
```

Resultado esperado:

```text
BUILD SUCCESSFUL
```

## 10.2. Pruebas funcionales

Tabla recomendada:

| Prueba | Procedimiento | Resultado esperado |
|---|---|---|
| Permisos | Abrir app por primera vez | Solicita audio y ubicación |
| Analizador | Conceder permisos | Muestra dB y espectro |
| Ponderación | Cambiar A/C/Z | Cambia el nivel mostrado |
| Ayuda | Pulsar `?` | Muestra explicación de ponderaciones |
| Captura | Pulsar botón de captura | Guarda muestra |
| Historial | Abrir historial | Aparecen muestras |
| Plegado | Pulsar muestra seleccionada | Se pliega |
| IA | Pulsar explicar | Genera texto |
| Mapa | Abrir mapa | Muestra heatmap |
| Ajustes | Cambiar buffer | Se actualiza análisis |

## 10.3. Pruebas de permisos

Menciona el problema solucionado:

- antes podía ser necesario reiniciar tras aceptar permisos;
- se corrigió mostrando el `NavHost` solo cuando `permissionsGranted` es true.

Fragmento:

```kotlin
if (permissionsGranted) {
    NavHost(...)
} else {
    PermissionWaitingScreen(...)
}
```

## 10.4. Pruebas de persistencia

Qué comprobar:

- muestra se guarda;
- bandas se guardan;
- clasificaciones se guardan;
- explicación IA se actualiza;
- borrado elimina la muestra.

## 10.5. Pruebas de validación

Si no has hecho validación formal con usuarios, puedes decir:

> Se realizaron pruebas manuales de uso sobre los flujos principales de la aplicación para comprobar que el comportamiento coincidía con los requisitos funcionales definidos.

No inventes usuarios si no los has tenido.

## 10.6. Pruebas de rendimiento

Puedes mencionar de forma cualitativa:

- análisis en tiempo real;
- selector de buffer;
- buffer mayor mejora resolución;
- buffer menor mejora respuesta.

No hace falta inventar métricas si no las tienes.

---

# 11. Conclusiones

## Qué debe cumplir este apartado

Todo lo que aparezca aquí debe estar relacionado con objetivos iniciales.

No metas conclusiones nuevas que no tengan relación con objetivos.

## Estructura recomendada

### 11.1. Revisión de objetivos

Usa una tabla:

| Objetivo | ¿Cumplido? | Evidencia |
|---|---:|---|
| Captura de audio | Sí | Analizador en tiempo real |
| Ponderaciones | Sí | Selector A/C/Z |
| Espectro | Sí | Gráfica logarítmica |
| Guardado | Sí | Room + histórico |
| Mapa | Sí | Heatmap |
| IA | Sí | YAMNet + Gemini |
| Ajustes | Sí | Buffer + offset |
| Arquitectura | Sí | MVVM + capas |

### 11.2. Valoración del resultado

Explica qué has conseguido:

- app funcional;
- integración de varias tecnologías;
- análisis acústico;
- geolocalización;
- IA;
- arquitectura mantenible.

### 11.3. Limitaciones

Incluye:

- no es sonómetro profesional;
- depende del micrófono;
- calibración aproximada;
- Gemini requiere conexión;
- YAMNet puede fallar;
- pruebas con usuario limitadas.

### 11.4. Trabajo futuro

Ideas:

- calibración con sonómetro real;
- exportación CSV/GeoJSON;
- estadísticas por zona;
- alertas;
- backend colaborativo;
- Hilt;
- más tests;
- comparación con equipos profesionales.

## Frase útil

> En conjunto, los objetivos planteados se han alcanzado de forma satisfactoria, obteniéndose una aplicación funcional que integra medición acústica, visualización geográfica, persistencia local e inteligencia artificial. No obstante, existen líneas de mejora relacionadas con la calibración, la validación experimental y la evolución arquitectónica.

---

# 12. Bibliografía

Incluye fuentes sobre:

- ruido ambiental;
- ponderaciones acústicas;
- FFT;
- Android Developers;
- Jetpack Compose;
- Room;
- MapLibre;
- TensorFlow Lite;
- YAMNet;
- Firebase AI/Gemini.

---

# 13. Anexos recomendados

## Anexo A. Manual de usuario

Explica:

1. conceder permisos;
2. usar analizador;
3. cambiar ponderación;
4. capturar muestra;
5. consultar mapa;
6. consultar histórico;
7. generar explicación;
8. cambiar ajustes.

## Anexo B. Estructura del proyecto

Incluye árbol de paquetes.

## Anexo C. Fragmentos de código relevantes

Incluye solo fragmentos esenciales:

- repositorio;
- captura de audio;
- guardado;
- Gemini;
- permisos;
- Room.

---

# 14. Orden recomendado para redactar

No escribas en orden de la memoria. Te recomiendo:

1. Objetivos.
2. Estado del arte.
3. Análisis del problema.
4. Solución propuesta.
5. Diseño.
6. Desarrollo.
7. Pruebas.
8. Conclusiones.
9. Motivación.
10. Introducción.

La introducción queda mejor al final.

---

# 15. Checklist final

## Introducción

- [ ] Se entiende el proyecto globalmente.
- [ ] No hay exceso de detalle técnico.
- [ ] Se presenta la solución.

## Motivación

- [ ] Explica por qué el tema importa.
- [ ] Conecta con objetivos.

## Objetivos

- [ ] Hay objetivo general.
- [ ] Hay objetivos específicos verificables.
- [ ] Se podrán recuperar en conclusiones.

## Estado del arte

- [ ] Compara apps existentes.
- [ ] Justifica el camino elegido.

## Análisis del problema

- [ ] Identifica necesidades.
- [ ] Menciona limitaciones.
- [ ] Usa algún método sistemático.

## Solución propuesta

- [ ] Explica en qué consiste.
- [ ] Describe fases.
- [ ] Indica implantación y validación.

## Diseño

- [ ] Explica arquitectura.
- [ ] Explica subsistemas.
- [ ] Incluye modelo de datos.

## Desarrollo

- [ ] Explica problemas y decisiones.
- [ ] No incluye demasiado código.
- [ ] Muestra partes relevantes.

## Implantación y pruebas

- [ ] Explica instalación/ejecución.
- [ ] Incluye pruebas funcionales.
- [ ] Incluye resultados.

## Conclusiones

- [ ] Responde a objetivos.
- [ ] Reconoce limitaciones.
- [ ] Propone trabajo futuro.

