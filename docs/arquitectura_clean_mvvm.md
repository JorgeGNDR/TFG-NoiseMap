# Evolucion de la arquitectura Clean + MVVM

Este documento resume la reorganizacion progresiva de la arquitectura de la aplicacion para separar responsabilidades, reducir acoplamientos y acercar el proyecto a un enfoque Clean Architecture con MVVM.

## Objetivo

El objetivo principal de la refactorizacion ha sido que la capa de presentacion no dependa directamente de detalles de infraestructura como Room, Firebase AI o TensorFlow Lite. Para ello se ha separado el codigo en tres zonas principales:

- `ui`: pantallas Compose y ViewModels.
- `domain`: modelos de negocio y contratos.
- `data`: implementaciones concretas de persistencia, IA y servicios externos.

La direccion deseada de dependencias es:

```text
ui -> domain
data -> domain
domain -> sin dependencias de Android, Room, Firebase ni Compose
```

## Estructura actual principal

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
```

## Capa `domain`

La capa `domain` contiene elementos independientes de frameworks. En ella se han ubicado modelos y contratos usados por la aplicacion.

### `domain/model`

Contiene modelos limpios sin anotaciones de Room ni dependencias Android:

- `AudioSampleRecord`: representa una medicion acustica.
- `FrequencyBandEnergy`: representa la energia de una banda de frecuencia.
- `SoundDetection`: representa una etiqueta detectada por el clasificador de sonidos.
- `FullAudioSample`: agrupa una muestra con su espectro y clasificaciones.
- `HeatmapTile`: representa un punto agregado para el mapa de calor.
- `ThirdOctaveBands`: contiene las frecuencias centrales de bandas de tercio de octava.

Estos modelos permiten que la UI trabaje con datos de negocio y no con entidades de base de datos.

### `domain/repository`

Contiene interfaces que definen lo que necesita la aplicacion, sin imponer como se implementa:

- `AudioRepository`: guardado de mediciones y consulta de datos para el mapa.
- `HistoryRepository`: consulta, detalle, borrado y actualizacion de explicaciones del historico.
- `NoiseExplanationRepository`: contrato para generar explicaciones de muestras acusticas.

Esta separacion permite que los ViewModels dependan de abstracciones y no de Room o Firebase.

## Capa `data`

La capa `data` contiene implementaciones concretas y dependencias externas.

### `data/local`

Contiene la persistencia local con Room:

- entidades Room como `AudioSample`, `FrequencyBin`, `SoundClassification` y `GeoTile`.
- DAOs como `AudioSampleDao`, `FrequencyBinDao`, `SoundClassificationDao` y `GeoTileDao`.
- `AppDatabase`, donde se define la base de datos local y sus migraciones.

Esta capa es la unica que debe conocer detalles SQL y anotaciones de Room.

### `data/repository`

Contiene implementaciones de los contratos de dominio:

- `RoomAudioRepository`: implementa `AudioRepository` usando DAOs de Room.
- `RoomHistoryRepository`: implementa `HistoryRepository` usando DAOs de Room y transforma entidades Room a modelos de dominio.
- `RepositoryProvider`: service locator simple que centraliza la creacion de repositorios.

`RepositoryProvider` es una solucion pragmatica para evitar repetir la construccion de dependencias en cada ViewModel. En una version mas avanzada podria sustituirse por Hilt o Koin.

### `data/ai`

Contiene la integracion con Firebase AI / Gemini:

- `NoiseExplanationService`: implementa `NoiseExplanationRepository` y genera explicaciones textuales de una medicion.

Al estar detras de una interfaz de dominio, la UI no depende directamente de Firebase AI.

### `data/audio`

Contiene la integracion con el hardware de audio del dispositivo:

- `AudioCaptureManager`: configura `AudioRecord`, captura audio del microfono y entrega buffers PCM al analizador.

Se ha movido fuera de `dsp` porque usa APIs Android de microfono. El paquete `dsp` queda reservado para procesamiento de senal puro.

### `data/ml`

Contiene la integracion con TensorFlow Lite:

- `SoundClassifierManager`: gestiona la carga del modelo YAMNet y la clasificacion de sonidos ambientales.

Se ha movido desde el paquete raiz `ml` a `data/ml` porque TensorFlow Lite es una dependencia de infraestructura, equivalente a Room o Firebase.

### `data/location`

Contiene la integracion con Google Play Services Fused Location Provider:

- `LocationHelper`: obtiene la ultima ubicacion conocida, solicita ubicacion actual y expone actualizaciones periodicas mediante `Flow`.

Se ha movido desde `utils` porque la ubicacion no es una utilidad generica, sino infraestructura Android.

### `data/settings`

Contiene la persistencia de preferencias de usuario:

- `AppSettings`: gestiona el tamano de buffer del espectro y el offset de calibracion mediante SharedPreferences y `StateFlow`.

Se ha movido desde el paquete raiz `settings` porque SharedPreferences es infraestructura Android.

## Capa `ui`

La capa `ui` contiene pantallas Compose y ViewModels.

### `ui/analyzer`

Contiene la pantalla principal de analisis acustico y su ViewModel:

- `AnalyzerScreen`: UI del analizador.
- `AnalyzerViewModel`: coordina captura, FFT, clasificacion, estado de pantalla y guardado de muestras.
- `LogarithmicSpectrumAnalyzer`: componente visual del espectro.

Tras la refactorizacion, `AnalyzerViewModel` usa `AudioRepository` como contrato de dominio, aunque todavia instancia servicios tecnicos como captura de audio y clasificador.

### `ui/map`

Contiene el mapa de calor:

- `MapScreen`: integra MapLibre en Compose.
- `MapViewModel`: solicita datos al `AudioRepository` y los transforma a puntos visuales.

Este modulo ya no accede directamente a Room.

### `ui/history`

Contiene el historico:

- `HistoryScreen`: lista y detalle de mediciones guardadas.
- `HistoryViewModel`: gestiona seleccion, borrado y explicaciones IA.

Antes accedia directamente a `AppDatabase` y a los DAOs de Room. Ahora depende de:

- `HistoryRepository`
- `NoiseExplanationRepository`

Con esto la UI del historico ha quedado desacoplada de Room y Firebase.

### `ui/settings`

Contiene la pantalla de ajustes. Actualmente consume `AppSettings` desde `data/settings`. Sigue siendo un singleton practico basado en SharedPreferences, pero ya no esta en un paquete raiz. En una version mas estricta podria exponerse mediante un contrato `SettingsRepository` en dominio.

### `ui/common`

Contiene ViewModels compartidos, como `LocationViewModel`.

### `ui/theme`

Contiene el tema visual de Compose, colores, tipografias y modificadores comunes.

## Paquetes tecnicos pendientes

### `dsp`

Contiene logica de procesado digital de senal:

- `FFTCalculator`
- `WindowingFunctions`
- `ThirdOctaveCalculator`
- `SpectrumWeighting`

`FFTCalculator`, `WindowingFunctions`, `ThirdOctaveCalculator` y `SpectrumWeighting` encapsulan calculos acusticos reutilizables. Esta extraccion reduce la responsabilidad del `AnalyzerViewModel`, que ahora delega el calculo de tercios de octava, frecuencia dominante y ponderacion visual del espectro.

## Cambios realizados

### Separacion de `AudioRepository`

Antes el repositorio estaba en `domain`, pero dependia directamente de Room. Se ha separado en:

- `domain/repository/AudioRepository`: interfaz.
- `data/repository/RoomAudioRepository`: implementacion Room.

Esto evita que `domain` conozca DAOs o entidades de persistencia.

### Limpieza del historico

Antes `HistoryViewModel` accedia directamente a:

- `AppDatabase`
- `AudioSampleDao`
- `FrequencyBinDao`
- `SoundClassificationDao`
- `NoiseExplanationService`

Ahora depende de:

- `HistoryRepository`
- `NoiseExplanationRepository`

La implementacion Room se ha movido a `RoomHistoryRepository` y Gemini queda encapsulado en `NoiseExplanationService`.

### Movimiento de Gemini

`NoiseExplanationService` se ha ubicado en `data/ai`, ya que Firebase AI es infraestructura externa.

### Movimiento de TensorFlow Lite

`SoundClassifierManager` se ha movido de `ml` a `data/ml`, ya que YAMNet/TensorFlow Lite forman parte de la infraestructura de clasificacion.

### Movimiento de ubicacion

`LocationHelper` se ha movido de `utils` a `data/location`. Con este cambio se elimina el paquete generico `utils` y se expresa mejor que la obtencion de ubicacion es una dependencia de infraestructura Android.

### Movimiento de captura de audio

`AudioCaptureManager` se ha movido de `dsp` a `data/audio`. Aunque participa en el analizador, su responsabilidad principal es acceder al microfono mediante APIs Android, por lo que es infraestructura de plataforma y no procesamiento digital puro.

### Movimiento de ajustes

`AppSettings` se ha movido de `settings` a `data/settings`. Esta clase persiste preferencias mediante SharedPreferences, por lo que pertenece a la capa de datos.

### Extraccion de herramientas del analizador

Se han extraido del `AnalyzerViewModel` varias operaciones de calculo:

- `ThirdOctaveCalculator`: agrupa el espectro FFT en bandas de tercio de octava y calcula la frecuencia dominante.
- `SpectrumWeighting`: aplica la ponderacion A/C/Z al espectro visual.
- `WeightingType`: se ha movido a `domain/model`, ya que representa un concepto acustico de dominio y no un detalle de UI.

### Eliminacion de paquetes vacios

Se han eliminado paquetes que ya no contenian codigo o que eran restos de estructura previa.

## Estado actual

La arquitectura actual es una version pragmatica de Clean Architecture + MVVM. No es una arquitectura purista al 100%, pero ya cumple los principios mas importantes:

- la UI no accede directamente a Room en mapa ni historico.
- el dominio contiene contratos y modelos limpios.
- Room, Firebase AI y TensorFlow Lite estan en `data`.
- los ViewModels coordinan estado y llamadas a repositorios.

## Mejoras futuras

Posibles pasos para continuar la limpieza:

1. Convertir `AppSettings` en un repositorio de ajustes con interfaz de dominio.
2. Seguir reduciendo el `AnalyzerViewModel` mediante casos de uso.
3. Crear una interfaz de dominio para el clasificador YAMNet.
4. Sustituir `RepositoryProvider` por Hilt para inyeccion de dependencias formal.
