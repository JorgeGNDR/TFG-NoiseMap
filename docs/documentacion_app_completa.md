# Documentación completa de la aplicación

## 1. Descripción general

La aplicación desarrollada en este proyecto es una herramienta Android para la medición, análisis, geolocalización y consulta histórica de ruido ambiental. Su finalidad es permitir al usuario observar en tiempo real el nivel sonoro captado por el micrófono del dispositivo, visualizar su espectro de frecuencias, guardar muestras geolocalizadas, consultar un mapa de calor acústico y obtener una explicación automática de las mediciones mediante inteligencia artificial generativa.

La app combina varias áreas técnicas:

- captura de audio en tiempo real;
- procesamiento digital de señal;
- cálculo de niveles sonoros ponderados;
- análisis espectral mediante FFT;
- clasificación de sonidos con TensorFlow Lite/YAMNet;
- almacenamiento local con Room;
- visualización geográfica con MapLibre;
- explicación textual con Firebase AI/Gemini;
- interfaz declarativa con Jetpack Compose.

El nombre actual de la aplicación en recursos es:

```text
TFG JORGE GANDARA
```

## 2. Objetivos funcionales

La aplicación persigue los siguientes objetivos funcionales:

1. Medir el nivel de presión sonora en tiempo real.
2. Mostrar el nivel actual, el nivel medio y el pico máximo.
3. Permitir seleccionar ponderaciones acústicas A, C y Z.
4. Mostrar el espectro de frecuencias en escala logarítmica.
5. Incorporar una curva de retención de picos en el espectro.
6. Detectar sonidos ambientales mediante YAMNet.
7. Guardar muestras acústicas geolocalizadas.
8. Almacenar información espectral y etiquetas detectadas.
9. Visualizar las mediciones sobre un mapa de calor.
10. Consultar el historial de mediciones.
11. Generar explicaciones comprensibles mediante IA.
12. Permitir configurar parámetros de análisis, como tamaño de buffer y offset de calibración.

## 3. Tecnologías principales

La aplicación está desarrollada en Kotlin para Android y usa las siguientes tecnologías:

| Área | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose |
| Arquitectura | MVVM con separación por capas inspirada en Clean Architecture |
| Persistencia | Room |
| Procesamiento FFT | JTransforms |
| Clasificación de audio | TensorFlow Lite Task Audio + YAMNet |
| Mapa | MapLibre GL Android |
| Ubicación | Google Play Services Location |
| IA generativa | Firebase AI / Gemini |
| Concurrencia | Kotlin Coroutines + StateFlow |
| Preferencias | SharedPreferences encapsulado en `AppSettings` |

## 4. Requisitos y permisos

La app necesita varios permisos declarados en `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Los permisos realmente esenciales para el uso principal son:

- `RECORD_AUDIO`: necesario para capturar audio del micrófono.
- `ACCESS_FINE_LOCATION` o `ACCESS_COARSE_LOCATION`: necesarios para geolocalizar muestras y centrar el mapa.
- `INTERNET`: necesario para el mapa y para las explicaciones IA.

Además, se declaran como características requeridas:

```xml
<uses-feature android:name="android.hardware.microphone" android:required="true" />
<uses-feature android:name="android.hardware.location.gps" android:required="true" />
```

La aplicación solicita permisos al inicio. Si el usuario los concede, se inicia el flujo normal de análisis y geolocalización. Si no se conceden, se muestra una pantalla indicando que son necesarios.

## 5. Arquitectura general

La estructura actual está organizada por capas:

```text
com.gandara.tfgjorgegandara
+-- data
|   +-- ai
|   +-- audio
|   +-- location
|   +-- local
|   +-- ml
|   +-- repository
|   +-- settings
+-- domain
|   +-- model
|   +-- repository
+-- dsp
+-- ui
|   +-- analyzer
|   +-- common
|   +-- history
|   +-- map
|   +-- settings
|   +-- theme
+-- MainActivity.kt
```

La idea general es:

```text
UI -> Domain -> Data
```

Donde:

- `ui` contiene pantallas Compose y ViewModels;
- `domain` contiene modelos limpios e interfaces;
- `data` contiene implementaciones concretas: Room, Firebase, TensorFlow Lite, ubicación, audio y preferencias;
- `dsp` contiene herramientas de procesado digital de señal que no dependen directamente de Android.

Esta arquitectura permite separar responsabilidades y reducir el acoplamiento entre la interfaz, la lógica de negocio y los detalles de infraestructura.

## 6. Capa de datos

### 6.1. `data/local`

Contiene la base de datos local con Room.

Entidades principales:

- `AudioSample`: muestra acústica guardada.
- `FrequencyBin`: energía espectral por banda.
- `SoundClassification`: etiquetas de sonido detectadas.
- `GeoTile`: agregación espacial por celda y franja temporal.

DAOs principales:

- `AudioSampleDao`
- `FrequencyBinDao`
- `SoundClassificationDao`
- `GeoTileDao`

Base de datos:

- `AppDatabase`

La base de datos se llama:

```text
noise_map_database
```

La versión actual es:

```text
6
```

Existe una migración de versión 5 a 6 que añade el campo:

```text
aiExplanation
```

a la tabla `audio_samples`.

### 6.2. `data/repository`

Contiene implementaciones concretas de contratos de dominio:

- `RoomAudioRepository`
- `RoomHistoryRepository`
- `RepositoryProvider`

`RoomAudioRepository` se encarga de guardar muestras acústicas completas y consultar datos para el mapa.

`RoomHistoryRepository` se encarga de observar el histórico, cargar detalles, borrar muestras y actualizar explicaciones IA.

`RepositoryProvider` centraliza la creación de repositorios. Actualmente funciona como un service locator sencillo.

### 6.3. `data/ai`

Contiene:

- `NoiseExplanationService`

Esta clase implementa el contrato `NoiseExplanationRepository` y utiliza Firebase AI/Gemini para generar una explicación textual de una muestra acústica.

El modelo usado es:

```text
gemini-2.5-flash
```

La respuesta se configura con:

- temperatura baja;
- límite de tokens;
- presupuesto de pensamiento desactivado.

La explicación generada se guarda en la base de datos dentro de `AudioSample.aiExplanation`.

### 6.4. `data/ml`

Contiene:

- `SoundClassifierManager`

Esta clase carga el modelo:

```text
assets/yamnet.tflite
```

Utiliza TensorFlow Lite Task Audio para clasificar sonidos ambientales. La clasificación se usa para mostrar una etiqueta en la pantalla de análisis y para guardar etiquetas asociadas a las capturas.

### 6.5. `data/audio`

Contiene:

- `AudioCaptureManager`

Esta clase accede al micrófono mediante `AudioRecord`, configura el buffer de lectura y entrega muestras PCM al analizador.

Se ha ubicado en `data/audio` porque depende de APIs Android de hardware, por lo que se considera infraestructura.

### 6.6. `data/location`

Contiene:

- `LocationHelper`

Utiliza Google Play Services Fused Location Provider para:

- obtener la última ubicación conocida;
- solicitar una ubicación actual;
- emitir actualizaciones periódicas mediante `Flow`.

### 6.7. `data/settings`

Contiene:

- `AppSettings`
- `AppSettingsState`

Gestiona preferencias de usuario mediante SharedPreferences:

- tamaño del buffer del espectro;
- offset de calibración.

Los tamaños disponibles de buffer son:

```text
1024, 2048, 4096, 8192
```

El offset de calibración se limita entre:

```text
60 y 120
```

## 7. Capa de dominio

### 7.1. Modelos

La carpeta `domain/model` contiene modelos independientes de Room, Compose y Android:

- `AudioSampleRecord`
- `FullAudioSample`
- `FrequencyBandEnergy`
- `SoundDetection`
- `HeatmapTile`
- `ThirdOctaveBands`
- `WeightingType`

Estos modelos permiten que la UI y los repositorios trabajen con conceptos de negocio sin depender directamente de entidades Room.

### 7.2. Repositorios de dominio

La carpeta `domain/repository` contiene contratos:

- `AudioRepository`
- `HistoryRepository`
- `NoiseExplanationRepository`

Estos contratos permiten que los ViewModels dependan de abstracciones y no de implementaciones concretas.

## 8. Procesamiento digital de señal

La carpeta `dsp` contiene herramientas de análisis acústico y procesamiento de señal.

### 8.1. `FFTCalculator`

Calcula el espectro de frecuencias a partir del audio capturado. También calcula niveles con ponderación A, C y Z.

### 8.2. `WindowingFunctions`

Aplica funciones de ventana, como Hann, para reducir fugas espectrales antes del cálculo FFT.

### 8.3. `ThirdOctaveCalculator`

Agrupa el espectro FFT en bandas de tercio de octava. También calcula la frecuencia dominante de una muestra.

### 8.4. `SpectrumWeighting`

Aplica ponderación A/C/Z al espectro visual para que la representación gráfica sea coherente con la ponderación seleccionada.

## 9. Pantallas de la aplicación

La navegación principal contiene cuatro pantallas:

```text
Analizador
Mapa
Historial
Ajustes
```

La navegación se gestiona en `MainActivity` mediante Navigation Compose.

## 10. Pantalla Analizador

Archivos principales:

- `ui/analyzer/AnalyzerScreen.kt`
- `ui/analyzer/AnalyzerViewModel.kt`
- `ui/analyzer/LogarithmicSpectrumAnalyzer.kt`

### Funciones principales

La pantalla de análisis muestra:

- nivel actual en dB;
- nivel medio;
- nivel pico;
- ponderación seleccionada;
- espectro logarítmico;
- curva de retención de picos;
- etiqueta de sonido detectada;
- botón de captura de muestra.

### Ponderaciones acústicas

El usuario puede seleccionar:

- `A`: aproxima la percepción del oído humano y es habitual para ruido ambiental.
- `C`: mantiene más peso en bajas frecuencias y es útil para sonidos fuertes o graves.
- `Z`: no aplica corrección perceptiva y muestra la señal de forma más plana.

La pantalla incluye un botón de ayuda con explicación de estas ponderaciones.

### Captura de muestras

Cuando el usuario pulsa el botón de captura:

1. se inicia una sesión de varios segundos;
2. se acumulan niveles dB y espectro;
3. se detectan etiquetas de sonido;
4. se calcula el promedio de la captura;
5. se calcula el pico máximo;
6. se calcula la frecuencia dominante;
7. se agrupa el espectro en tercios de octava;
8. se guarda la muestra en Room con ubicación.

## 11. Pantalla Mapa

Archivos principales:

- `ui/map/MapScreen.kt`
- `ui/map/MapViewModel.kt`

La pantalla de mapa usa MapLibre para visualizar puntos acústicos como mapa de calor.

### Funciones principales

- Mostrar mapa vectorial.
- Pintar capa de calor a partir de mediciones guardadas.
- Filtrar por banda de frecuencia.
- Filtrar por rango temporal.
- Centrar el mapa en la ubicación actual.

### Filtros temporales

Opciones disponibles:

- 24 h
- 7 días
- 30 días
- Todo

### Filtro de frecuencia

El mapa permite seleccionar:

- nivel global;
- bandas de tercio de octava.

El índice `-1` representa el nivel global.

## 12. Pantalla Historial

Archivos principales:

- `ui/history/HistoryScreen.kt`
- `ui/history/HistoryViewModel.kt`

La pantalla de historial permite consultar las muestras guardadas.

### Funciones principales

- Listar muestras en orden temporal descendente.
- Desplegar detalles de una muestra.
- Volver a plegar una muestra si se pulsa de nuevo.
- Consultar ubicación, nivel pico, clasificación IA y resolución espectral.
- Borrar muestras.
- Generar o actualizar explicación IA.

### Explicación IA

Cuando se solicita una explicación:

1. se cargan los detalles de la muestra;
2. se construye un resumen con nivel, pico, frecuencia dominante, etiquetas y bandas con más energía;
3. se envía a Gemini;
4. se recibe una explicación breve en castellano;
5. se guarda en la base de datos.

## 13. Pantalla Ajustes

Archivo principal:

- `ui/settings/SettingsScreen.kt`

Permite configurar:

- tamaño del buffer del espectro;
- offset de calibración;
- información de uso de la aplicación.

### Tamaño de buffer

Un buffer más pequeño ofrece más respuesta temporal, pero menor resolución frecuencial. Un buffer mayor ofrece mejor resolución frecuencial, pero puede ser menos inmediato.

Opciones:

```text
1024
2048
4096
8192
```

### Offset de calibración

El offset permite ajustar el nivel mostrado para aproximarlo a una referencia externa.

## 14. Tema visual

La app usa un estilo visual oscuro con acentos naranjas y componentes neumórficos.

Archivos:

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/NeumorphicModifier.kt`

El color principal de acento es:

```text
PowerOrange
```

El espectro usa:

- naranja para la curva actual;
- azul para el peak hold.

## 15. Flujo principal de datos

El flujo general desde el micrófono hasta el almacenamiento es:

```text
AudioCaptureManager
    -> AnalyzerViewModel
        -> FFTCalculator
        -> SpectrumWeighting
        -> ThirdOctaveCalculator
        -> SoundClassifierManager
        -> AudioRepository
            -> RoomAudioRepository
                -> Room DAOs
                    -> SQLite
```

El flujo para histórico e IA es:

```text
HistoryScreen
    -> HistoryViewModel
        -> HistoryRepository
            -> RoomHistoryRepository
                -> Room DAOs

HistoryViewModel
    -> NoiseExplanationRepository
        -> NoiseExplanationService
            -> Firebase AI / Gemini
```

El flujo para mapa es:

```text
MapScreen
    -> MapViewModel
        -> AudioRepository
            -> RoomAudioRepository
                -> Room DAOs
        -> HeatMapPoint
            -> MapLibre HeatmapLayer
```

## 16. Persistencia de datos

Cada muestra acústica se guarda con:

- fecha/hora;
- latitud;
- longitud;
- nivel medio;
- nivel pico;
- frecuencia dominante;
- ponderación usada;
- explicación IA opcional.

Además se guardan:

- bandas de frecuencia asociadas a la muestra;
- clasificaciones sonoras detectadas;
- datos agregados por celda geográfica.

## 17. Inteligencia artificial

La aplicación usa dos formas distintas de IA:

### 17.1. Clasificación local con YAMNet

YAMNet se ejecuta en el dispositivo mediante TensorFlow Lite. Su función es detectar sonidos ambientales como tráfico, sirenas, voces u otros eventos.

Ventajas:

- no requiere conexión para clasificar;
- trabaja en tiempo real;
- no envía audio al exterior.

### 17.2. Explicación textual con Gemini

Gemini se usa para generar una explicación comprensible de una muestra guardada.

La explicación no se genera continuamente, sino bajo demanda desde el historial.

## 18. Gestión de permisos

La app solicita permisos al iniciar. Si se conceden:

- se inicia la ubicación;
- se muestra el `NavHost`;
- el analizador puede comenzar a procesar audio.

Si no se conceden:

- se muestra una pantalla de espera;
- el usuario puede volver a solicitar permisos.

Esta lógica evita que el analizador se inicialice antes de tener permisos, previniendo errores y la necesidad de reiniciar la aplicación.

## 19. Limitaciones actuales

Aunque la separación de responsabilidades ha mejorado, quedan posibles mejoras:

1. Convertir `AppSettings` en un repositorio con interfaz de dominio.
2. Crear una interfaz de dominio para el clasificador YAMNet.
3. Reducir todavía más el tamaño de `AnalyzerViewModel` con casos de uso.
4. Sustituir `RepositoryProvider` por inyección de dependencias formal con Hilt.
5. Separar componentes internos grandes de `MapScreen`.

## 20. Estado de la arquitectura

La app utiliza MVVM con una organización por capas inspirada en Clean Architecture.

Puntos fuertes actuales:

- Room está encapsulado en `data/local` y `data/repository`.
- Firebase AI está encapsulado en `data/ai`.
- TensorFlow Lite está encapsulado en `data/ml`.
- Captura de audio está en `data/audio`.
- Ubicación está en `data/location`.
- Preferencias están en `data/settings`.
- Los modelos de dominio son independientes de Room.
- El histórico ya no accede directamente a la base de datos.
- El mapa usa repositorios y modelos intermedios.

La arquitectura no es purista al 100 %, pero es coherente, mantenible y adecuada para una aplicación Android de complejidad media.

## 21. Posible frase para la memoria

La aplicación se ha estructurado siguiendo un patrón MVVM con separación por capas inspirada en Clean Architecture. La capa de presentación se encarga de la interfaz y el estado visual, la capa de dominio define modelos e interfaces independientes de frameworks, y la capa de datos encapsula los detalles de persistencia, captura de audio, ubicación, clasificación local e integración con IA generativa. Esta organización facilita el mantenimiento, reduce el acoplamiento entre módulos y permite evolucionar la aplicación sin afectar de forma directa a la interfaz de usuario.
