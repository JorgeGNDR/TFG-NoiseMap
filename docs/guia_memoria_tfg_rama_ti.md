# Guía de memoria TFG - Rama TI

Esta guía está adaptada al enfoque indicado para la rama de Tecnologías de la Información. La memoria debe demostrar que el trabajo no es solo una aplicación funcionando, sino un **proyecto software completo**: con proceso, requisitos, diseño, desarrollo, pruebas, mantenimiento y relación con las competencias del grado.

La guía está pensada para tu aplicación Android de análisis de ruido ambiental:

- analizador acústico en tiempo real;
- espectro y ponderaciones A/C/Z;
- captura de muestras geolocalizadas;
- mapa de calor;
- histórico;
- clasificación local con YAMNet;
- explicación con Gemini;
- arquitectura MVVM con separación por capas.

---

# 0. Aspectos que debe cuidar la memoria

Según el seminario, en la rama TI conviene cuidar especialmente:

1. Proceso software.
2. Especificación de requisitos.
3. Arquitectura y diseño.
4. Desarrollo e implementación.
5. Pruebas y calidad.
6. Mantenimiento y gestión de versiones.
7. Relación con competencias del Grado en Ingeniería Informática.

Tu memoria debería reflejar todos ellos, aunque no todos tengan el mismo peso.

---

# 1. Estructura recomendada de la memoria

Te recomiendo esta estructura:

```text
Resumen
Abstract
Palabras clave

1. Introducción y motivación
2. Objetivos
3. Proceso software y planificación
4. Especificación de requisitos
5. Trabajo relacionado y estado del arte
6. Análisis del problema
7. Solución propuesta
8. Diseño de la solución
   8.1. Arquitectura software
   8.2. Diseño detallado
   8.3. Diseño de interfaz
   8.4. Modelo de datos
9. Desarrollo e implantación de la solución
10. Pruebas, evaluación y calidad del software
11. Mantenimiento y gestión de versiones
12. Relación con competencias del grado
13. Conclusiones y trabajo futuro
Bibliografía
Anexos
```

Esta estructura se ajusta bien al texto del seminario y además encaja con tu proyecto.

---

# 2. Resumen

## Objetivo del apartado

Explicar en pocas líneas:

- qué problema aborda el TFG;
- qué solución se ha desarrollado;
- qué tecnologías principales usa;
- qué resultado se ha obtenido.

## Qué contar en tu caso

Menciona:

- app Android;
- ruido ambiental;
- análisis en tiempo real;
- mapa de calor;
- histórico;
- clasificación YAMNet;
- explicación Gemini;
- arquitectura MVVM.

## Idea de redacción

> Este Trabajo de Fin de Grado presenta el desarrollo de una aplicación Android para el análisis de ruido ambiental. La aplicación permite capturar audio en tiempo real, calcular niveles sonoros, visualizar el espectro de frecuencias, guardar mediciones geolocalizadas y representarlas sobre un mapa de calor. Además, integra clasificación local de sonidos mediante TensorFlow Lite/YAMNet y generación de explicaciones mediante IA generativa.

Escríbelo al final, cuando la memoria esté cerrada.

---

# 3. Introducción y motivación

## Qué espera el seminario

Debe transmitir la importancia del tema, partiendo del entorno global y centrándose en el problema específico. Puede incluir pinceladas de estado del arte y de la propuesta.

## Qué contar

### 3.1. Contexto general

Habla del ruido ambiental:

- fenómeno cotidiano;
- presente en entornos urbanos, domésticos y laborales;
- puede afectar al bienestar, descanso y concentración;
- no siempre es fácil medirlo o interpretarlo.

### 3.2. Problema específico

El usuario puede percibir ruido, pero normalmente no tiene una herramienta integrada para:

- medirlo;
- guardarlo;
- verlo sobre un mapa;
- consultar histórico;
- entender qué significa la medición.

### 3.3. Motivación práctica

Tu app aporta:

- análisis inmediato;
- registro geolocalizado;
- visualización en mapa;
- explicación comprensible.

### 3.4. Motivación técnica

El proyecto combina:

- Android;
- procesamiento digital de señal;
- bases de datos;
- geolocalización;
- mapas;
- IA local;
- IA generativa;
- arquitectura software.

### 3.5. Motivación personal/académica

Puedes explicar que el trabajo permite integrar competencias del grado en un proyecto completo y útil.

## Frase útil

> La motivación principal del proyecto es desarrollar una herramienta móvil que facilite al usuario la medición, registro e interpretación del ruido ambiental, integrando análisis acústico, geolocalización e inteligencia artificial en una única aplicación.

## Qué evitar

No conviertas la introducción en una lista de librerías ni en una explicación de código.

---

# 4. Objetivos

## Qué espera el seminario

Los objetivos deben indicar qué aportará la solución. Deben poder comprobarse al final.

## Objetivo general recomendado

> Diseñar e implementar una aplicación Android para la medición, análisis, almacenamiento geolocalizado, visualización e interpretación de ruido ambiental mediante técnicas de procesado digital de señal e inteligencia artificial.

## Objetivos específicos

### OE1. Captura y análisis de audio

Implementar captura de audio desde el micrófono y análisis en tiempo real.

Evidencia:

- `data/audio/AudioCaptureManager.kt`
- `ui/analyzer/AnalyzerViewModel.kt`

### OE2. Cálculo de niveles y ponderaciones acústicas

Calcular niveles sonoros con ponderaciones A, C y Z.

Evidencia:

- `dsp/FFTCalculator.kt`
- `domain/model/WeightingType.kt`
- selector A/C/Z.

### OE3. Visualización del espectro

Mostrar el espectro de frecuencias y retención de picos.

Evidencia:

- `LogarithmicSpectrumAnalyzer.kt`
- `SpectrumWeighting.kt`

### OE4. Registro geolocalizado de muestras

Guardar mediciones con ubicación, nivel, espectro y clasificación.

Evidencia:

- `RoomAudioRepository.kt`
- `AudioSample`
- `FrequencyBin`
- `SoundClassification`

### OE5. Mapa de calor

Representar mediciones en un mapa de calor con filtros temporales y frecuenciales.

Evidencia:

- `MapScreen.kt`
- `MapViewModel.kt`
- MapLibre.

### OE6. Histórico

Permitir consultar, desplegar, plegar, borrar y explicar muestras.

Evidencia:

- `HistoryScreen.kt`
- `HistoryViewModel.kt`
- `HistoryRepository.kt`

### OE7. Inteligencia artificial

Integrar clasificación local de sonidos y explicación generativa.

Evidencia:

- `SoundClassifierManager.kt`
- `NoiseExplanationService.kt`
- `yamnet.tflite`

### OE8. Configuración

Permitir modificar buffer y offset.

Evidencia:

- `SettingsScreen.kt`
- `AppSettings.kt`

### OE9. Arquitectura mantenible

Organizar el proyecto con MVVM y separación por capas.

Evidencia:

- `ui`
- `domain`
- `data`
- `dsp`

## Tabla objetivo-resultado

Incluye una tabla como:

| Objetivo | Resultado implementado |
|---|---|
| Captura de audio | Analizador en tiempo real |
| Ponderaciones | Selector A/C/Z |
| Espectro | Gráfica logarítmica con peak hold |
| Guardado | Room con muestras, bandas y etiquetas |
| Mapa | Heatmap MapLibre |
| Histórico | Lista desplegable con IA |
| IA | YAMNet + Gemini |
| Ajustes | Buffer + offset |
| Arquitectura | MVVM + capas |

Esta tabla debe reaparecer conceptualmente en conclusiones.

---

# 5. Proceso software y planificación

## Por qué incluirlo

El seminario indica que todo proyecto de la rama TI debería seguir un proceso software documentado. Este apartado demuestra que no has improvisado la app, sino que has seguido fases.

## Qué proceso puedes declarar

Para tu proyecto encaja bien un proceso **iterativo incremental**.

No hace falta inventar Scrum completo si no lo has usado. Puedes decir:

> El desarrollo siguió un proceso iterativo e incremental, en el que se fueron implementando módulos funcionales independientes y refinando la arquitectura conforme crecían las funcionalidades.

## Fases propuestas

| Fase | Descripción | Entregable |
|---|---|---|
| F1 | Investigación inicial | Selección de tecnologías |
| F2 | Captura de audio | Primer analizador funcional |
| F3 | FFT y ponderaciones | Cálculo dB y espectro |
| F4 | Persistencia | Base de datos Room |
| F5 | Geolocalización y mapa | Mapa de calor |
| F6 | Histórico | Consulta de muestras |
| F7 | IA local | Integración YAMNet |
| F8 | IA generativa | Explicaciones Gemini |
| F9 | Ajustes | Buffer y offset |
| F10 | Refactorización | MVVM + capas |
| F11 | Pruebas | Validación funcional |
| F12 | Documentación | Memoria y documentación técnica |

## Iteraciones reales que puedes destacar

Algunas mejoras que demuestran proceso incremental:

- permisos: se corrigió que tras aceptar permisos hubiera que reiniciar;
- mapa: se añadió filtro temporal;
- histórico: se añadió explicación IA y plegado de muestras;
- ajustes: se añadió configuración de buffer y offset;
- arquitectura: se movieron clases a `data`, `domain`, `dsp`;
- analizador: se extrajeron herramientas del ViewModel.

## Qué figura incluir

Un diagrama sencillo de fases:

```text
Investigación -> Analizador -> Persistencia -> Mapa -> Histórico -> IA -> Ajustes -> Refactorización -> Pruebas
```

---

# 6. Especificación de requisitos

## Qué espera el seminario

Se recomienda usar técnicas estándar o adaptadas:

- IEEE 830;
- casos de uso;
- historias de usuario;
- pruebas de aceptación.

Para tu memoria, lo más práctico es combinar:

- requisitos funcionales;
- requisitos no funcionales;
- casos de uso principales;
- pruebas de aceptación.

## 6.1. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-01 | La app debe capturar audio desde el micrófono. |
| RF-02 | La app debe calcular niveles sonoros en tiempo real. |
| RF-03 | La app debe permitir seleccionar ponderación A, C o Z. |
| RF-04 | La app debe mostrar el espectro de frecuencias. |
| RF-05 | La app debe guardar muestras geolocalizadas. |
| RF-06 | La app debe almacenar información espectral. |
| RF-07 | La app debe almacenar etiquetas de sonido detectadas. |
| RF-08 | La app debe mostrar un mapa de calor. |
| RF-09 | La app debe permitir filtrar el mapa por tiempo y frecuencia. |
| RF-10 | La app debe mostrar un histórico de mediciones. |
| RF-11 | La app debe permitir borrar muestras. |
| RF-12 | La app debe generar explicaciones IA de una muestra. |
| RF-13 | La app debe permitir configurar buffer y offset. |

## 6.2. Requisitos no funcionales

| ID | Requisito |
|---|---|
| RNF-01 | La app debe ejecutarse en Android. |
| RNF-02 | La interfaz debe ser clara y usable en móvil. |
| RNF-03 | El análisis debe realizarse de forma fluida. |
| RNF-04 | Los datos deben persistirse localmente. |
| RNF-05 | La clasificación local no debe depender de conexión. |
| RNF-06 | La arquitectura debe facilitar mantenimiento. |
| RNF-07 | La app debe gestionar permisos en tiempo de ejecución. |

## 6.3. Casos de uso

Incluye 4 o 5, no demasiados.

### CU-01. Analizar ruido en tiempo real

Actor: usuario.

Flujo:

1. El usuario abre la app.
2. Concede permisos.
3. La app muestra dB y espectro.
4. El usuario cambia ponderación si lo desea.

### CU-02. Capturar muestra

1. El usuario pulsa el botón de captura.
2. La app acumula datos.
3. La app obtiene ubicación.
4. La app guarda la muestra.

### CU-03. Consultar mapa

1. El usuario abre mapa.
2. La app carga datos.
3. El usuario cambia filtros.
4. El mapa se actualiza.

### CU-04. Consultar histórico

1. El usuario abre historial.
2. Selecciona una muestra.
3. La app despliega detalles.
4. El usuario puede plegarla o borrarla.

### CU-05. Generar explicación IA

1. El usuario selecciona una muestra.
2. Pulsa explicar.
3. La app envía resumen a Gemini.
4. Se muestra y guarda explicación.

## 6.4. Pruebas de aceptación

Ejemplo:

| Requisito | Prueba de aceptación |
|---|---|
| RF-05 | Tras capturar, la muestra aparece en historial. |
| RF-08 | Al abrir mapa, se visualizan mediciones guardadas. |
| RF-12 | Al pulsar explicar, aparece texto IA. |
| RF-13 | Al cambiar buffer, el analizador se reinicia con el nuevo tamaño. |

---

# 7. Trabajo relacionado y estado del arte

## Qué espera el seminario

Debe analizar trabajos y soluciones relacionadas, y explicar críticamente por qué son menos adecuadas que tu propuesta. También debe resumir tecnologías existentes necesarias para entender el TFG.

Puedes separar:

- trabajo relacionado: apps y soluciones existentes;
- contexto tecnológico: tecnologías usadas.

## 7.1. Aplicaciones similares

### Apps de sonómetro

Ejemplos:

- Sound Meter;
- Decibel X;
- Sound Meter Pro;
- NIOSH Sound Level Meter.

Analiza:

- medición dB;
- calibración;
- gráficas;
- histórico;
- limitaciones.

### Mapas de ruido

Ejemplos:

- NoiseCapture;
- mapas municipales;
- proyectos colaborativos.

Analiza:

- geolocalización;
- mapas;
- participación ciudadana;
- necesidad de backend.

### Herramientas técnicas

Ejemplos:

- analizadores FFT;
- software de audio;
- apps profesionales.

Analiza:

- potencia técnica;
- menor accesibilidad;
- poca integración con mapa/IA.

## 7.2. Tabla comparativa

| Solución | dB | Espectro | Mapa | Histórico | IA local | Explicación IA | Ajustes |
|---|---:|---:|---:|---:|---:|---:|---:|
| Sound Meter | Sí | Limitado | No | Limitado | No | No | Básicos |
| Decibel X | Sí | Parcial | Limitado | Sí | No | No | Sí |
| NIOSH SLM | Sí | Técnico | No | Sí | No | No | Sí |
| NoiseCapture | Sí | Limitado | Sí | Sí | No | No | Parcial |
| App desarrollada | Sí | Sí | Sí | Sí | Sí | Sí | Sí |

Verifica funcionalidades exactas antes de entregar.

## 7.3. Alternativas tecnológicas

Justifica:

- Android nativo frente a web;
- Room frente a ficheros;
- MapLibre frente a otras soluciones;
- TensorFlow Lite frente a clasificación remota;
- Gemini frente a reglas manuales.

## 7.4. Conclusión

La conclusión debe decir:

> La propuesta del TFG integra en una misma herramienta funcionalidades que suelen aparecer separadas: medición, análisis espectral, mapa, histórico, clasificación local y explicación generativa.

---

# 8. Análisis del problema

## Qué debe demostrar

Que has entendido el problema antes de diseñar la solución.

## 8.1. Problema principal

Formulación recomendada:

> El usuario puede percibir ruido en su entorno, pero no dispone normalmente de una herramienta integrada que permita medirlo, registrarlo, contextualizarlo geográficamente e interpretarlo de forma sencilla.

## 8.2. Necesidades

Lista:

- medición inmediata;
- visualización frecuencial;
- registro histórico;
- geolocalización;
- mapa;
- identificación de sonidos;
- explicación comprensible;
- configuración.

## 8.3. Usuarios

Define perfiles:

- usuario general;
- estudiante/investigador;
- usuario interesado en ruido urbano;
- usuario no experto.

## 8.4. Limitaciones

Incluye:

- micrófono no certificado;
- calibración dependiente de dispositivo;
- ruido variable;
- error GPS;
- IA imperfecta;
- conexión para Gemini.

## 8.5. DAFO

Incluye una tabla DAFO.

| Fortalezas | Debilidades |
|---|---|
| App integrada | No certificada |
| Análisis en tiempo real | Depende del móvil |
| Mapa e histórico | Requiere permisos |
| IA local y generativa | Gemini requiere conexión |

| Oportunidades | Amenazas |
|---|---|
| Interés por ruido urbano | Apps existentes |
| Uso educativo | Restricciones Android |
| Ampliación con exportación | Variabilidad hardware |

---

# 9. Solución propuesta

## Qué debe incluir

- qué es la solución;
- cómo se divide;
- fases de desarrollo;
- implantación;
- validación.

## 9.1. Descripción

La solución es una aplicación Android con cuatro pantallas:

- Analizador;
- Mapa;
- Historial;
- Ajustes.

Y subsistemas:

- audio;
- DSP;
- persistencia;
- ubicación;
- mapa;
- IA;
- configuración.

## 9.2. Fases

Incluye tabla de fases igual que en proceso software, pero enfocada a desarrollo de la solución.

## 9.3. Implantación

La solución se implanta instalando la app en dispositivo/emulador Android.

Requiere:

- permisos;
- modelo YAMNet en assets;
- configuración Firebase;
- conexión para mapa/Gemini.

## 9.4. Validación

Se valida con:

- compilación;
- pruebas manuales;
- pruebas de permisos;
- pruebas funcionales;
- verificación de guardado;
- verificación de IA.

---

# 10. Diseño de la solución

## 10.1. Arquitectura software

Explica:

```text
MVVM + capas inspiradas en Clean Architecture
```

Diagrama:

```text
ui -> domain -> data
      dsp
```

Capas:

| Capa | Responsabilidad |
|---|---|
| `ui` | Pantallas y ViewModels |
| `domain` | Modelos e interfaces |
| `data` | Room, audio, ubicación, IA, ajustes |
| `dsp` | Cálculos acústicos |

## 10.2. Diseño detallado de subsistemas

### Analizador

Componentes:

- `AudioCaptureManager`;
- `FFTCalculator`;
- `SpectrumWeighting`;
- `ThirdOctaveCalculator`;
- `AnalyzerViewModel`;
- `AnalyzerScreen`.

### Mapa

Componentes:

- `MapScreen`;
- `MapViewModel`;
- `AudioRepository`;
- MapLibre.

### Histórico

Componentes:

- `HistoryScreen`;
- `HistoryViewModel`;
- `HistoryRepository`;
- `RoomHistoryRepository`;
- `NoiseExplanationRepository`.

### Persistencia

Componentes:

- `AppDatabase`;
- DAOs;
- entidades;
- repositorios.

### IA

Componentes:

- `SoundClassifierManager`;
- `NoiseExplanationService`.

## 10.3. Diseño de interfaz

El seminario recomienda incluir bocetos o diseño de UI.

Puedes incluir:

- capturas finales;
- boceto simple de navegación;
- descripción de pantallas.

Pantallas:

- Analizador;
- Mapa;
- Historial;
- Ajustes.

## 10.4. Modelo de datos

Incluye diagrama:

```text
AudioSample 1---N FrequencyBin
AudioSample 1---N SoundClassification
GeoTile
```

Explica cada tabla.

---

# 11. Desarrollo e implantación de la solución

## Qué espera el seminario

Demostrar el trabajo técnico. Explicar problemas, decisiones y particularidades. No incluir mucho código.

## 11.1. Desarrollo del analizador

Cuenta:

- captura;
- FFT;
- ponderaciones;
- espectro;
- peak hold;
- captura de muestra.

Código breve:

```kotlin
val results = fftCalculator.calculateWeightings(
    audioBuffer,
    SAMPLE_RATE,
    _uiState.value.offset
)
```

## 11.2. Desarrollo de persistencia

Cuenta:

- Room;
- entidades;
- DAOs;
- migración `aiExplanation`;
- repositorios.

Código:

```kotlin
repository.saveCompleteAudioSample(...)
```

## 11.3. Desarrollo del mapa

Cuenta:

- MapLibre;
- heatmap;
- filtros;
- ubicación.

## 11.4. Desarrollo del histórico

Cuenta:

- listado;
- detalle;
- plegado;
- borrado;
- explicación.

## 11.5. Desarrollo de IA

Cuenta:

- YAMNet local;
- Gemini bajo demanda;
- prompt;
- guardado.

## 11.6. Refactorización arquitectónica

Muy importante:

- se movieron responsabilidades;
- `utils` desapareció;
- `settings` pasó a `data/settings`;
- audio pasó a `data/audio`;
- YAMNet a `data/ml`;
- Gemini a `data/ai`;
- modelos e interfaces a `domain`.

Esto demuestra mantenimiento y evolución.

## 11.7. Implantación

Explica:

- construcción con Gradle;
- instalación en dispositivo/emulador;
- permisos;
- assets;
- Firebase;
- ejecución.

Comando:

```powershell
.\gradlew.bat assembleDebug
```

---

# 12. Pruebas, evaluación y calidad

## Qué espera el seminario

Debe demostrar que se han alcanzado los objetivos. Si es posible, mostrar que la solución es mejor que alternativas del estado del arte.

## 12.1. Pruebas de compilación

```powershell
.\gradlew.bat assembleDebug
```

Resultado:

```text
BUILD SUCCESSFUL
```

## 12.2. Pruebas funcionales

Tabla:

| Prueba | Procedimiento | Resultado esperado |
|---|---|---|
| Permisos | Abrir app | Solicita audio y ubicación |
| Analizador | Conceder permisos | Muestra dB y espectro |
| Ponderación | Cambiar A/C/Z | Cambia nivel |
| Ayuda | Pulsar `?` | Muestra explicación |
| Captura | Pulsar botón | Guarda muestra |
| Historial | Abrir historial | Lista muestras |
| Plegado | Pulsar muestra abierta | Se pliega |
| IA | Explicar muestra | Genera texto |
| Mapa | Abrir mapa | Muestra heatmap |
| Ajustes | Cambiar buffer | Actualiza análisis |

## 12.3. Pruebas de aceptación

Relaciona con requisitos:

| Requisito | Prueba | Estado |
|---|---|---|
| RF-05 | Capturar y consultar muestra | Superada |
| RF-08 | Abrir mapa con muestras | Superada |
| RF-12 | Generar explicación IA | Superada |

## 12.4. Comparación con estado del arte

El seminario pide demostrar que la solución es mejor que una o varias del estado del arte. En tu caso no hace falta decir que es mejor en precisión. Puedes decir que es más completa funcionalmente.

Tabla:

| Característica | Apps dB básicas | NoiseCapture | App TFG |
|---|---:|---:|---:|
| dB en tiempo real | Sí | Sí | Sí |
| Espectro | Limitado | Limitado | Sí |
| Mapa | No | Sí | Sí |
| Histórico | Limitado | Sí | Sí |
| IA local | No | No | Sí |
| Explicación IA | No | No | Sí |
| Ajustes buffer/offset | Parcial | Parcial | Sí |

Conclusión:

> La solución no pretende superar a aplicaciones profesionales en precisión certificada, sino integrar en una sola app funcionalidades que aparecen dispersas en otras soluciones.

## 12.5. Calidad del software

Puedes mencionar:

- arquitectura por capas;
- separación de responsabilidades;
- uso de repositorios;
- documentación en `docs`;
- compilación continua manual;
- control de warnings;
- refactorización progresiva.

---

# 13. Mantenimiento y gestión de versiones

## Qué espera el seminario

Documentar si se han usado herramientas para gestión de versiones y mantenimiento.

## Qué puedes contar

Si has usado Git, explica:

- repositorio;
- commits;
- ramas si las hubo;
- control de cambios.

Si no has usado Git formalmente, puedes centrarte en mantenibilidad:

- estructura por capas;
- documentación;
- separación de responsabilidades;
- migraciones Room;
- arquitectura modular.

## Mantenimiento técnico de la app

Aspectos que facilitan mantenimiento:

- `data` separa infraestructura;
- `domain` contiene contratos;
- `ui` contiene pantallas;
- `dsp` contiene cálculos;
- documentación en Markdown;
- Room con migraciones;
- ajustes centralizados;
- repositorios.

## Fragmento interesante

Migración Room:

```kotlin
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE audio_samples ADD COLUMN aiExplanation TEXT")
    }
}
```

Esto demuestra evolución mantenible de la base de datos.

---

# 14. Relación con los estudios y competencias del grado

## Por qué incluirlo

El seminario menciona explícitamente la relación con los estudios y competencias del Grado en Ingeniería Informática, rama Ingeniería de Computadores.

## Cómo enfocarlo

Relaciona el TFG con competencias:

### Desarrollo software

- Kotlin;
- Android;
- Compose;
- MVVM.

### Arquitectura y diseño

- separación por capas;
- repositorios;
- diseño modular.

### Bases de datos

- Room;
- entidades;
- DAOs;
- migraciones.

### Sistemas y dispositivos

- captura de audio;
- permisos;
- sensores/ubicación;
- ejecución en dispositivo móvil.

### Procesamiento de datos

- FFT;
- espectro;
- tercios de octava;
- normalización.

### Inteligencia artificial

- TensorFlow Lite;
- YAMNet;
- Gemini.

### Calidad y mantenimiento

- pruebas;
- documentación;
- refactorización;
- estructura mantenible.

## Frase útil

> El proyecto integra competencias propias del Grado en Ingeniería Informática, como el diseño y desarrollo de software, la gestión de datos, la interacción con hardware del dispositivo, el procesamiento de información y la aplicación de técnicas de inteligencia artificial.

---

# 15. Conclusiones y trabajo futuro

## Qué espera el seminario

Debe resumir lo conseguido y cómo puede derivar en nuevos proyectos. Además, debe responder a objetivos.

## 15.1. Revisión de objetivos

Tabla:

| Objetivo | Cumplido | Evidencia |
|---|---:|---|
| Captura audio | Sí | Analizador |
| Ponderaciones | Sí | A/C/Z |
| Espectro | Sí | Gráfica |
| Guardado | Sí | Room |
| Mapa | Sí | Heatmap |
| Histórico | Sí | Lista desplegable |
| IA | Sí | YAMNet + Gemini |
| Ajustes | Sí | Buffer + offset |
| Arquitectura | Sí | MVVM + capas |

## 15.2. Valoración global

Explica que se ha conseguido una app funcional e integrada.

## 15.3. Limitaciones

Incluye:

- no sonómetro certificado;
- micrófono variable;
- calibración aproximada;
- Gemini requiere conexión;
- YAMNet no es infalible;
- faltan pruebas con sonómetro real.

## 15.4. Trabajo futuro

Ideas:

- calibración con sonómetro;
- exportación CSV/GeoJSON;
- backend colaborativo;
- estadísticas por zona;
- alertas;
- más pruebas;
- Hilt;
- comparación experimental.

---

# 16. Bibliografía

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

# 17. Anexos

## Anexo A. Manual de usuario

Pasos:

1. Conceder permisos.
2. Usar analizador.
3. Cambiar ponderación.
4. Capturar muestra.
5. Ver mapa.
6. Consultar histórico.
7. Generar explicación.
8. Cambiar ajustes.

## Anexo B. Estructura del proyecto

Árbol:

```text
data
domain
dsp
ui
```

## Anexo C. Fragmentos de código relevantes

Incluye:

- interfaz de repositorio;
- captura de audio;
- guardado de muestra;
- migración Room;
- permisos;
- prompt Gemini.

---

# 18. Orden recomendado de redacción

No escribas en orden. Mejor:

1. Objetivos.
2. Requisitos.
3. Estado del arte.
4. Análisis del problema.
5. Solución propuesta.
6. Diseño.
7. Desarrollo.
8. Pruebas.
9. Conclusiones.
10. Motivación.
11. Introducción.
12. Resumen.

---

# 19. Errores a evitar

## Error 1. Decir que la app es un sonómetro profesional

Mejor:

> herramienta de análisis y estimación.

## Error 2. Decir que se aplica Clean Architecture completa

Mejor:

> organización por capas inspirada en Clean Architecture.

## Error 3. Poner demasiado código

Solo fragmentos relevantes.

## Error 4. No relacionar conclusiones con objetivos

Cada conclusión debe responder a algo planteado al inicio.

## Error 5. No explicar proceso software

Incluye fases e iteraciones.

## Error 6. No comparar con soluciones existentes

Incluye estado del arte con tabla comparativa.

---

# 20. Checklist final

## Proceso software

- [ ] Hay fases.
- [ ] Hay entregables.
- [ ] Se explica evolución.

## Requisitos

- [ ] Hay RF y RNF.
- [ ] Hay casos de uso o historias.
- [ ] Hay pruebas de aceptación.

## Arquitectura

- [ ] Se explica MVVM.
- [ ] Se explican capas.
- [ ] Se muestran subsistemas.

## Diseño

- [ ] Hay modelo de datos.
- [ ] Hay diseño de UI o capturas.
- [ ] Hay diagrama de arquitectura.

## Desarrollo

- [ ] Se explican decisiones técnicas.
- [ ] Se explican problemas.
- [ ] No hay exceso de código.

## Pruebas

- [ ] Hay pruebas funcionales.
- [ ] Hay relación con requisitos.
- [ ] Hay comparación con estado del arte.

## Mantenimiento

- [ ] Se habla de estructura mantenible.
- [ ] Se habla de versiones o evolución.

## Conclusiones

- [ ] Responden a objetivos.
- [ ] Reconocen limitaciones.
- [ ] Proponen trabajo futuro.

