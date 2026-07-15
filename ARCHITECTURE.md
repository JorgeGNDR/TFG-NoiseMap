# Arquitectura de NoiseMap

El proyecto sigue una arquitectura MVVM organizada mediante los principios de Clean Architecture.

## Capas

```text
presentation  ->  domain  <-  data
       \            ^          /
        +---------- di --------+
```

- `presentation`: pantallas Compose, estado visual y ViewModels.
- `domain`: modelos, contratos, casos de uso y procesamiento acústico puro.
- `data`: acceso al micrófono, ubicación, TensorFlow Lite, Firebase, preferencias y Room.
- `di`: punto de composición que conecta interfaces y casos de uso con sus implementaciones.

La capa `domain` no depende de Android ni de las otras capas. La capa `presentation` consume interfaces y casos de uso del dominio, pero no conoce las implementaciones de `data`.

## Flujo de una medición

```text
AnalyzerScreen
    -> AnalyzerViewModel
    -> SaveAudioSampleUseCase
    -> AudioRepository
    -> RoomAudioRepository
    -> Room
```

El audio capturado también atraviesa los servicios definidos por el dominio:

```text
AudioCaptureSource -> FFT/ponderaciones/tercios de octava
                   -> remuestreo -> SoundClassifier -> YAMNet
```

## Flujo del mapa

```text
MapScreen
    -> MapViewModel
    -> GetHeatmapDataUseCase
    -> AudioRepository
    -> consultas Room agregadas por posición, tiempo y frecuencia
```

## Flujo del historial

```text
HistoryScreen
    -> HistoryViewModel
    -> HistoryUseCases
    -> HistoryRepository / NoiseExplanationRepository
    -> Room / Firebase AI
```

## Responsabilidad de AppContainer

`di/AppContainer.kt` es el único lugar que construye implementaciones concretas y las entrega a la presentación mediante interfaces o casos de uso. No contiene lógica de negocio.
