# Enunciado consolidado — Trabajo de Aplicación: Simulador de Time-Sharing

Fuentes: notas de clase del profesor (sección "Gestión de Procesos" y "TRABAJO DE APLICACIÓN –
Sea el sistema") + transcripción de su video de indicaciones (`docs/transcripcion_audio.txt`, `docs/notas_clase.md`).

## 1. El sistema (`docs/img/01_memoria_procesador.png`, `docs/img/02_cola_fifo_prioridades.png`)
Una **cola (lista FIFO) con prioridades** de procesos que entran a **memoria** y luego al
**procesador**. Hay **un solo procesador** (el profe: "vamos a asumir un procesador y varias tareas").
Analogía del profesor: un banco con **una sola ventanilla/cajero** atendiendo a personas que llegan;
algunas tienen preferencia.

## 2. Estructura de un proceso
Id (PID), Name, Tamaño en Bytes, Status, Cuota de tiempo (ms), Prioridad, … (docs/img/02_cola_fifo_prioridades.png).

## 3. Estados y transiciones (docs/img/04_transicion_estados.png)
- READY → RUN (le toca el procesador)
- RUN → WAIT (se acabó su cuota / espera) → READY
- RUN → FINISH (terminó)
- RUN → CANCEL (cancelado)

## 4. Asignación de prioridad por terciles (pseudocódigo del profe)
```
PROCEDURE AssignPriority()
  A = TL[1].Size ; B = TL[1].Size
  FOR i = 1 TO N
    IF TL[i].Size < A  THEN A = TL[i].Size
    IF B < TL[i].Size  THEN B = TL[i].Size
  END-FOR
  H = (B-A)/3          // hitos A, A+H, A+2H, B
  FOR i = 1 TO N
    IF      TL[i].Size < A+1H  THEN TL[i].Priority = 1
    ELSEIF  TL[i].Size < A+2H  THEN TL[i].Priority = 2
    ELSE                            TL[i].Priority = 3
  END-FOR
END
```
- Cuota = prioridad × 100 ms ("lo multiplico por 100 milisegundos, ya tengo la cuota").
- **Más grande = más tiempo** (el proceso grande tiene franjas más anchas en el time-sharing).
- **Dinámico:** "¿qué pasaría si llegan otros programas? Llegó uno bien grande, entonces el que valía
  70 se convierte en pequeño. La prioridad puede variar." → recalcular al llegar cada proceso.
- Caso borde: si A == B (todos iguales o un solo proceso), H = 0 → asignar prioridad 1 o 2 a todos sin dividir entre cero.

## 5. Lista dinámica — lista enlazada simple con INI y FIN (docs/img/03_lista_enlazada_INI_FIN.png)
```
PROCEDURE AdicionarProceso()
  PTR = NEW(NODO); PTR^.NEXT = FIN; FIN = PTR
PROCEDURE ObtenerMaximaPrioridad()   // recorre y devuelve el nodo con mayor prioridad
PROCEDURE ObtenerMinimaPrioridad()   // recorre y devuelve el nodo con menor prioridad
```
**Correcciones al pseudocódigo del profe (explicarlas en la exposición):**
- Devuelve `RETURN PP` pero debe devolver `AUX`.
- En el dibujo los enlaces van de FIN (más nuevo) hacia INI (más antiguo, NEXT=null). Si se recorre
  desde INI con NEXT solo se visita un nodo. Implementar enlaces INI→…→FIN (cola FIFO normal:
  se encola por FIN, se atiende por INI), que es lo coherente con "cola FIFO".

## 6. Lo que debe verse (palabras del profesor)
- "Como si fuera un juego, un simulador. Yo pondría una pantalla de los procesos que están entrando."
- "Un proceso entró, se carga en la memoria… si un programa es más grande que otro… ya me llenó la memoria."
- "El proceso se ejecuta y pongo un temporizador ahí, de acuerdo al dato que tienen."
- "Pasó su tiempo y automáticamente entra otro proceso."
- Línea de tiempo tipo **time-sharing** con franjas de colores intercaladas (docs/img/07_time_sharing.png), comparada con el
  procesamiento serial (docs/img/06_procesamiento_serial.png).

## 7. Libertad de recursos
"Utilicen cualquier recurso… no se compliquen, es simplemente hacer un diseño."

## 8. Pendiente de confirmar con el profesor
1. ¿Es la nota del parcial o hay examen aparte (semana 8 del sílabo)?
2. Fecha de entrega y qué se entrega (código, informe, exposición).
3. ¿Siguiente proceso por FIFO (round robin) o por máxima prioridad?
4. ¿De dónde sale el tiempo total de ejecución de cada proceso?
