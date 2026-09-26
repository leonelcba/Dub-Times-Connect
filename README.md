# Dub Times Connect Android — v0.4

Primera versión con lectura real desde Discord y caché SQLite local.

## Cambios
- Corrige texto escrito en diálogos claros: ahora usa texto oscuro y placeholder gris.
- `Sincronizar Discord` descarga los posts activos y archivados del foro configurado.
- Importa proyectos, versiones, personajes, estudiantes, estados, sugerencias y entradas.
- Reconoce colectivos (Walla/Wallas/Todos/Todas/Gente y repartos separados por comas).
- SQLite queda como caché local para abrir los datos aunque no se sincronice en ese momento.
- La sincronización de esta versión es **Discord → Android (lectura)**. No modifica Discord.

## Configuración inicial
Al pulsar `Sincronizar Discord` por primera vez, la app pide:
1. Token del bot.
2. ID del servidor (guild).
3. ID del foro de proyectos que usa Dub Times Connect de Windows.

Para esta prueba privada el token se guarda localmente en el teléfono. No distribuyas una APK con un token incorporado ni compartas el token.

## Compilar
El workflow `.github/workflows/build-apk.yml` compila el APK debug en GitHub Actions.
