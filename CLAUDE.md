# Simulador de Time-Sharing — Programación Concurrente y Paralela

Trabajo grupal (6 personas): simulador visual de gestión de procesos con time-sharing
en un solo procesador. Enunciado consolidado: `docs/ENUNCIADO.md`. Imágenes del profe: `docs/img/`.
Notas de clase y transcripción del profe: `docs/notas_clase.md`, `docs/transcripcion_audio.txt`.

## Stack
- Java 23 + Swing. Sin librerías externas. Código en `src/`, compilar y correr con `run.bat`.
- Concurrencia con hilos como en clase (`new Thread(new Runnable(){...})`): un hilo de llegadas
  y un hilo de CPU; la GUI solo pinta. Acceso a la lista compartida con `synchronized`.
- Sin `ArrayList` ni colecciones de `java.util` (el profe lo pide): lista enlazada propia y arrays.

## Reglas del dominio (ya decididas, no cambiar sin acordarlo en grupo)
- Proceso: Id, Name, Size (bytes), Status, Priority, Quota (ms), TiempoTotal, TiempoRestante, color.
- Estados: READY → RUN → WAIT (cuota agotada) → READY; RUN → FINISH | CANCEL.
- `AssignPriority()` por terciles del tamaño (pseudocódigo del profe), recalculada para todos
  los procesos vivos en cada llegada. Si A == B → todos prioridad 2 (evita H = 0).
- Cuota = Priority × 100 ms. TiempoTotal proporcional al tamaño.
- Lista enlazada simple con INI y FIN, enlazada INI → … → FIN: se encola por FIN, se atiende
  por INI. Correcciones al pseudocódigo del profe (explicarlas en la exposición):
  `RETURN PP` → `RETURN AUX`, y dirección de enlaces invertida respecto al dibujo.
- Planificación: round robin FIFO (la prioridad solo define la cuota). Un solo procesador.
- Memoria con capacidad fija: si un proceso no cabe, espera en la cola hasta que se libere.

## Estilo
- Nombres en español como el profe: `AdicionarProceso`, `ObtenerMaximaPrioridad`,
  `ObtenerMinimaPrioridad`, `AssignPriority`. Comentarios breves en español.
- Código simple y legible: se expone y se explica línea a línea.
