# Dub Times Connect Android 1.0.1

Código Android actualizado. Esta versión reemplaza el antiguo prototipo de septiembre de 2026.

- ID de paquete: `com.dubtimes.connectviewer`
- Versión: 1.0.1 (`versionCode 1001`)
- Funciones: Proyectos, Alumnos · Hoy, Clases, Tareas, edición y sincronización de Supabase ya configurada en la aplicación.
- La vinculación se conserva al instalar una actualización firmada con el mismo certificado.

## APK
La pestaña **Actions** compila automáticamente un APK de pruebas. Para publicar un APK instalable encima de la aplicación existente, configurar estos secretos de repositorio con el certificado **original**, sin subirlo públicamente:
`DUBTIMES_ANDROID_KEYSTORE_B64`, `DUBTIMES_KEYSTORE_PASSWORD`, `DUBTIMES_KEY_ALIAS`, `DUBTIMES_KEY_PASSWORD`.

Sin estos secretos únicamente se entrega APK debug, que podría requerir instalación separada, no sobre la anterior.
