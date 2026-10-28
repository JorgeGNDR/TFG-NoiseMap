# NoiseMap

**Análisis y mapeo del ruido ambiental desde un dispositivo Android.**

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4)
![TFG](https://img.shields.io/badge/TFG-Ingeniería_Informática_%C2%B7_UPV-005A9C)

NoiseMap es una aplicación Android para el análisis y mapeo del ruido ambiental que combina procesamiento digital de audio, clasificación automática de sonidos e inteligencia artificial generativa. Permite estudiar el entorno sonoro en tiempo real mediante la medición de niveles con ponderaciones A, C y Z, el análisis del espectro de frecuencias y su distribución en bandas de tercio de octava.

La aplicación integra YAMNet, un modelo de aprendizaje automático ejecutado localmente con TensorFlow Lite, para identificar sonidos ambientales a partir de la señal del micrófono. Esta clasificación se realiza en el propio dispositivo, sin necesidad de enviar el audio a un servicio externo, y complementa las mediciones acústicas con información sobre las posibles fuentes sonoras.

Las mediciones se organizan en sesiones geolocalizadas y se representan en mapas de ruido con filtros temporales y de frecuencia, facilitando la exploración de patrones espaciales y temporales. Además, la integración de Gemini a través de Firebase AI Logic permite generar explicaciones en lenguaje natural a partir de los resultados de cada muestra, acercando la información técnica a usuarios sin conocimientos especializados en acústica.
Proyecto desarrollado por **Jorge Gándara Sanchis** como **Trabajo de Fin de Grado en Ingeniería Informática en la Universitat Politècnica de València (UPV)**.

## Contenido

- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Instalación y configuración](#instalación-y-configuración)
- [Uso](#uso)
- [Procesamiento del audio](#procesamiento-del-audio)
- [Datos y conectividad](#datos-y-conectividad)
- [Pruebas](#pruebas)
- [Autor](#autor)

## Funcionalidades

### Analizador en tiempo real

- Visualización del nivel sonoro actual, la media energética y el pico.
- Selección de ponderación frecuencial **A, C o Z**.
- Espectro de frecuencias con visualización de máximos.
- Identificación de sonidos ambientales mediante **YAMNet**, ejecutado en el dispositivo con TensorFlow Lite.
- Ajuste del tamaño del búfer y del offset de calibración.

### Sesiones de medición

- Captura de sesiones geolocalizadas de hasta **2 minutos**.
- Guardado periódico en segmentos de aproximadamente **5 segundos**.
- Registro del nivel medio y máximo, la frecuencia dominante, las bandas de tercio de octava y los sonidos detectados.
- Almacenamiento de la ponderación y el offset utilizados en cada muestra.

### Mapa de ruido

- Representación espacial de las mediciones sobre cartografía de **MapLibre y MapTiler**.
- Consulta del nivel global o de una banda de tercio de octava.
- Filtros por periodo, fecha concreta, rango de fechas y franja horaria.
- Superficie de color interpolada a partir de los niveles medidos, con transparencia en zonas alejadas de las muestras.

### Historial e interpretación

- Consulta de sesiones y del detalle de sus muestras.
- Eliminación individual, por selección múltiple o por sesión.
- Generación de explicaciones breves en español mediante **Gemini**, integrado a través de **Firebase AI Logic**.

## Tecnologías

| Área | Tecnologías |
| --- | --- |
| Lenguaje y compilación | Kotlin, Gradle Kotlin DSL, KSP |
| Interfaz | Jetpack Compose, Material 3, Navigation Compose |
| Estado y concurrencia | ViewModel, StateFlow, Kotlin Coroutines |
| Persistencia | Room y SharedPreferences |
| Captura y procesamiento | AudioRecord, JTransforms, FFT y ventana de Hann |
| Clasificación de sonidos | TensorFlow Lite y YAMNet |
| Mapas y ubicación | MapLibre Native, MapTiler y Google Play Services Location |
| Explicaciones con IA | Firebase AI Logic y Gemini 2.5 Flash |
| Pruebas | JUnit y AndroidX Test |

## Arquitectura

El código se organiza en tres capas, con **ViewModel** para gestionar el estado de las pantallas y un contenedor de dependencias manual en `AppContainer`.

- **`presentation`**: pantallas Compose, ViewModels, navegación visual y renderizado del mapa.
- **`domain`**: modelos, contratos de repositorios, casos de uso y lógica de procesamiento acústico.
- **`data`**: captura del micrófono, ubicación, base de datos Room, clasificación de sonidos, ajustes y acceso a Gemini.

```text
app/
├── src/main/
│   ├── assets/
│   │   └── yamnet.tflite
│   ├── java/com/gandara/tfgjorgegandara/
│   │   ├── data/
│   │   │   ├── ai/
│   │   │   ├── audio/
│   │   │   ├── local/
│   │   │   ├── location/
│   │   │   ├── ml/
│   │   │   ├── repository/
│   │   │   └── settings/
│   │   ├── di/
│   │   ├── domain/
│   │   ├── presentation/
│   │   └── MainActivity.kt
│   └── res/
├── src/test/                  # Pruebas unitarias
├── src/androidTest/           # Pruebas instrumentadas
└── schemas/                   # Esquemas de Room
```

## Instalación y configuración

### Requisitos

- Android Studio compatible con **Android Gradle Plugin 8.13.2**.
- Android SDK **36**.
- **JDK 21**, según la configuración del daemon de Gradle incluida en el repositorio. El código de la aplicación tiene configurado el destino JVM 11.
- Dispositivo o emulador con **Android 8.0 / API 26 o superior**. Para probar el micrófono y la geolocalización, es preferible un dispositivo físico con Google Play Services.
- Conexión a Internet para descargar dependencias y utilizar los mapas y las explicaciones con IA.

### 1. Clonar el repositorio

```bash
git clone https://github.com/JorgeGNDR/TFG-NoiseMap.git
cd TFG-NoiseMap
```

Abre la carpeta del proyecto en Android Studio.

### 2. Configurar las propiedades locales

En el archivo `local.properties` de la raíz, añade la ruta de tu Android SDK y tu clave de MapTiler:

```properties
sdk.dir=/ruta/a/tu/Android/Sdk
MAPTILER_API_KEY=TU_CLAVE_DE_MAPTILER
```

Android Studio puede generar automáticamente `sdk.dir`. Si el archivo ya existe, conserva su contenido y añade la propiedad de MapTiler. También se incluye [local.properties.example](local.properties.example) como punto de partida.

La aplicación utiliza la clave `MAPTILER_API_KEY` para cargar el estilo cartográfico `dataviz-v4`. Sin una clave válida, el mapa base puede no cargarse. `local.properties` está excluido del control de versiones.

### 3. Configurar Firebase para las explicaciones con IA

La integración utiliza **Firebase AI Logic con el backend de Gemini Developer API** y el modelo `gemini-2.5-flash`.

El repositorio incluye `app/google-services.json`. Si vas a utilizar tu propio proyecto de Firebase:

1. Registra una aplicación Android con el identificador `com.gandara.tfgjorgegandara`.
2. Configura Firebase AI Logic con el backend de Gemini Developer API en ese proyecto.
3. Sustituye `app/google-services.json` por el archivo de configuración de tu aplicación.
4. Sincroniza de nuevo el proyecto con Gradle.

La configuración de compilación lee ese archivo y genera los recursos necesarios para Firebase. Las explicaciones requieren una configuración válida del servicio y conexión a Internet.

El modelo de clasificación local `yamnet.tflite` ya está incluido en `app/src/main/assets/`.

### 4. Compilar y ejecutar

Sincroniza el proyecto y ejecuta el módulo **`app`** desde Android Studio, o genera una APK de depuración desde la terminal:

```bash
# macOS / Linux
chmod +x gradlew
./gradlew assembleDebug
```

```powershell
# Windows
.\gradlew.bat assembleDebug
```

La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Uso

1. **Concede acceso al micrófono** al abrir la aplicación. Para guardar mediciones geolocalizadas, concede también el permiso de ubicación y espera a que se obtenga una posición válida.
2. En **Analizador**, consulta los niveles y el espectro, y elige la ponderación A, C o Z.
3. Pulsa el **botón circular de captura** para iniciar una sesión. Vuelve a pulsarlo para finalizarla; la sesión también termina al alcanzar los dos minutos.
4. Abre **Mapa** para explorar las muestras guardadas y aplicar filtros de tiempo o de frecuencia.
5. En **Historial**, revisa las sesiones, consulta sus muestras y solicita una explicación con IA.
6. En **Ajustes**, selecciona un búfer de 1024, 2048 o 4096 muestras y modifica el offset si dispones de una referencia de calibración.

El analizador puede funcionar sin ubicación, pero el guardado de una sesión necesita una posición válida. Al finalizar una sesión, el último segmento se guarda si ha acumulado al menos tres segundos de datos.

## Procesamiento del audio

El micrófono captura audio **mono PCM de 16 bits a 44,1 kHz**. A partir de esa señal se calcula la FFT con ventana de Hann, los niveles ponderados A/C/Z y la distribución de energía en **31 bandas de tercio de octava**, con frecuencias centrales entre 16 Hz y 16 kHz.

La clasificación comparte la señal del analizador y la remuestrea al formato requerido por YAMNet. Las medias de nivel se calculan en el dominio energético, evitando promediar directamente los valores en decibelios.

Los niveles dependen del micrófono del dispositivo y del offset de calibración. Las mediciones y la interpolación del mapa tienen carácter orientativo; la aplicación no debe presentarse como un sonómetro certificado.

## Datos y conectividad

- **En el dispositivo**: procesamiento acústico, inferencia con YAMNet y almacenamiento de sesiones, muestras, coordenadas, bandas y clasificaciones en Room. Los ajustes se conservan mediante SharedPreferences.
- **Audio**: el flujo de medición procesa la señal en memoria y guarda resultados derivados; no genera archivos de grabación de audio.
- **Mapas**: la cartografía se solicita a MapTiler a través de MapLibre.
- **Explicaciones**: al solicitarlas, se envía a Gemini un resumen textual con niveles, frecuencia dominante, sonidos detectados y bandas destacadas. El prompt actual no incluye audio crudo ni coordenadas.

## Pruebas

Ejecuta las pruebas unitarias con:

```bash
./gradlew testDebugUnitTest
```

Para las pruebas instrumentadas, conecta un dispositivo o inicia un emulador:

```bash
./gradlew connectedDebugAndroidTest
```

En Windows, sustituye `./gradlew` por `.\gradlew.bat`.

La suite incluida comprueba las medias energéticas, la detección de tonos mediante FFT, la asignación a tercios de octava, el remuestreo, las operaciones y relaciones de Room y el comportamiento del color de la superficie acústica.

## Autor

**Jorge Gándara Sanchis**  
Trabajo de Fin de Grado · Ingeniería Informática  
Universitat Politècnica de València (UPV)

[Perfil de GitHub](https://github.com/JorgeGNDR) · [Repositorio del proyecto](https://github.com/JorgeGNDR/TFG-NoiseMap)
