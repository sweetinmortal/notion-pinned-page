# Corrección de atajos de Notion

Módulo de Vector que abre el destino de los iconos creados con **Fijar página** en Notion para Android. Lee el enlace de cada icono al tocarlo y lo entrega al manejador de enlaces de Notion. El alcance del módulo es `notion.id`.

## Instalación

1. Instala [Vector 2.2](https://github.com/JingMatrix/Vector/releases/tag/v2.2) en Magisk con Zygisk habilitado y reinicia Android.
2. Instala el APK de la release de este repositorio. Activa **Corrección de atajos de Notion** en Vector y comprueba que Notion sea la única app seleccionada.
3. Abre un icono existente o crea uno con **Fijar página**. Cambiar de espacio de trabajo puede tomar unos segundos. Desactiva el módulo en Vector para volver al comportamiento original.

El APK requiere la API 102 de libxposed. Se probó con Notion y Lawnchair en Android 17. El parche usa puntos de entrada de Notion que pueden cambiar con una actualización de la app.

## Compilación

Usa JDK 17, Gradle 8.13 y Android SDK 36.

```text
gradle assembleDebug testDebugUnitTest
```

El APK queda en `build/outputs/apk/debug/NotionPinnedPage-debug.apk`.
