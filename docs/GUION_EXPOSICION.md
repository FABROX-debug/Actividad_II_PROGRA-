# Guion de exposición — Simulador Time-Sharing

Duración: unos 12–15 min (unos 2 min por persona) y luego la demo libre / preguntas.
Antes de empezar: abrir `run.bat` y dejar la velocidad en **0.25x**.

---

## 1. El problema (Persona 1)
**Mostrar:** `docs/img/01_memoria_procesador.png` y `docs/img/02_cola_fifo_prioridades.png`.

- En una PC corren decenas de programas "a la vez", pero el procesador (en nuestro caso **uno solo**) solo puede ejecutar uno en cada instante.
- Analogía del profe: un **banco con una sola ventanilla**. La gente llega, hace cola, y algunos tienen preferencia.
- Los procesos llegan a una **cola**, se **cargan en memoria** y luego usan el **procesador**.
- La solución es el **time-sharing**: dar a cada proceso una porción de tiempo (cuota) y turnarse muy rápido. No es paralelo (eso necesita 2+ procesadores); es **concurrente**.

## 2. El proceso y sus estados (Persona 2)
**Mostrar:** `src/Proceso.java`, `src/Estado.java`, `docs/img/04_transicion_estados.png`, y en el simulador la **tabla** y la **leyenda de estados**.

- Cada proceso tiene: **Id, Nombre, Tamaño, Estado, Prioridad, Cuota**, y además cuánto tiempo de CPU necesita (Total) y cuánto le falta (Resta).
- En el código, un `Proceso` es un **nodo** de una lista enlazada (tiene el puntero `NEXT`).
- Estados: **READY → RUN → WAIT → READY**, y desde RUN puede terminar en **FINISH** o **CANCEL**.
  - READY: listo, espera su turno.
  - RUN: está usando el procesador.
  - WAIT: se le acabó la cuota, vuelve al final de la cola.
  - FINISH: terminó. CANCEL: lo cancelaron.

## 3. Prioridad por terciles y cuota (Persona 3)
**Mostrar:** `AssignPriority()` en `src/Planificador.java` y el panel **"AssignPriority() — terciles"** del simulador.

**Demo:** pulsar **"4 tareas ejemplo"**.
- Se busca el tamaño mínimo **A** y el máximo **B**; **H = (B − A) / 3** divide el rango en tres partes:
  - pequeño → prioridad 1 → cuota **100 ms**
  - mediano → prioridad 2 → cuota **200 ms**
  - grande → prioridad 3 → cuota **300 ms**
- **Cuota = prioridad × 100 ms**: más grande, más tiempo por turno.
- **Es dinámica:** pulsar **"+1 proceso"** varias veces hasta que llegue uno grande → en la bitácora aparece "P# cambia de prioridad 3 → 2". Lo que antes era grande ahora es mediano (lo que dijo el profe).
- Caso borde: si todos miden lo mismo (A = B), H sería 0 → les damos prioridad 2 a todos.

## 4. La lista enlazada INI / FIN (Persona 4)
**Mostrar:** `src/ListaProcesos.java`, `docs/img/03_lista_enlazada_INI_FIN.png`, y en el simulador las colas **①** y **③** dibujadas como nodos.

- La tabla de procesos es **dinámica** (entran y salen procesos), por eso usamos una **lista enlazada simple con dos punteros: INI y FIN**. No usamos ArrayList.
- **Se encola por FIN** (`AdicionarProceso`) y **se atiende por INI** (`SacarInicio`): es una cola **FIFO**.
- `ObtenerMaximaPrioridad()` y `ObtenerMinimaPrioridad()` recorren la lista desde INI; el resultado se ve en pantalla.
- **Dos correcciones que hicimos al pseudocódigo** (decirlas con seguridad):
  1. Devolvía `RETURN PP`, pero la variable que guarda el resultado es **AUX** → devolvemos `AUX`.
  2. En el dibujo los enlaces van de FIN hacia INI; si luego se recorre desde INI con NEXT, solo se visita **un nodo**. Por eso enlazamos **INI → … → FIN**, que es lo coherente con una cola FIFO.

## 5. Hilos y round robin (Persona 5)
**Mostrar:** `HiloCPU()` y `HiloLlegadas()` en `src/Planificador.java`, y en el simulador los paneles **②** y **④**.

- Usamos **hilos** como en clase (`new Thread(new Runnable(){...})`):
  - **HiloCPU**: saca el proceso de **INI**, lo pone en RUN, lo deja trabajar su **cuota** (temporizador) y, si no terminó, lo pasa a WAIT y lo **vuelve a encolar por FIN**. Esto es **round robin**.
  - **HiloLlegadas**: genera procesos nuevos (checkbox "Llegadas automáticas").
  - La pantalla es un tercer hilo que solo dibuja.
- Como los hilos comparten la lista, usamos **`synchronized`** para que no la modifiquen al mismo tiempo.

**Demo:**
1. Pulsar **"Paso a paso"** varias veces: se ve un turno a la vez (RUN → WAIT → READY) en la tabla y en la bitácora.
2. Pulsar **"+5 procesos"**: la memoria (2048 KB) se llena → aparece "¡Memoria llena!" y los procesos esperan en la **cola de entrada ①**.
3. Pulsar **Iniciar**. Cuando uno termina (FINISH) libera memoria y entra el siguiente automáticamente.
4. Seleccionar una fila y pulsar **"Cancelar seleccionado"** → CANCEL y libera memoria.

## 6. Time-sharing vs serial y conclusiones (Persona 6)
**Mostrar:** el panel **⑤ Línea de tiempo**, `docs/img/06_procesamiento_serial.png` y `docs/img/07_time_sharing.png`.

- **Serial:** cada proceso corre completo, uno tras otro → Tiempo Total = T1 + T2 + … + Tn. Si llega uno pequeño detrás de uno grande, espera muchísimo.
- **Time sharing:** franjas intercaladas, como en la imagen del profe. Las franjas rojas (Task 2, la más grande) son más anchas porque su cuota es mayor.
- El time-sharing **tarda un poco más en total** por los **cambios de contexto** (franjas grises: guardar un proceso y cargar otro), pero **todos avanzan** y el usuario siente que todo corre a la vez.
- **Conclusiones:**
  - Con un solo procesador hay **concurrencia**, no paralelismo.
  - La prioridad por tamaño decide **cuánto** tiempo recibe cada uno en cada turno.
  - La lista enlazada INI/FIN permite que entren y salgan procesos dinámicamente.

---

## Preguntas probables del profe

- **¿Es paralelo?** No. Hay un solo procesador; es concurrente. Paralelo necesita 2 o más procesadores.
- **¿Los procesos son reales?** Son simulados (objetos `Proceso`). Lo real son los hilos de Java, la lista enlazada y el algoritmo. El reparto real de la CPU lo hace el sistema operativo y un programa no puede reemplazarlo.
- **¿Por qué round robin y no siempre el de mayor prioridad?** Si siempre entrara el de mayor prioridad, los pequeños esperarían para siempre (inanición). Con round robin todos avanzan; la prioridad define el tamaño del turno.
- **¿Qué pasa si llega un proceso que no cabe en memoria?** Espera en la cola de entrada (FIFO) hasta que otro termine o se cancele y libere espacio.
- **¿Para qué sirve `synchronized`?** Para que el hilo de la CPU, el de llegadas y la pantalla no lean ni modifiquen la lista al mismo tiempo y la corrompan.
- **¿Por qué la cuota cambia mientras corre?** El nuevo valor se aplica en su **siguiente** turno; el turno actual termina con la cuota con la que entró.
- **¿Qué es el gris en la línea de tiempo?** El cambio de contexto (30 ms): el costo de pasar de un proceso a otro.
