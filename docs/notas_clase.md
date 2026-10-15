GESTIÓN DE PROCESOS

PROCESO = TAREA

Un Proceso es la instancia de un Programa

[]

[]

JERARQUÍA DE PROCESOS

[]

Un proceso tiene sub-procesos

Un sub-proceso tiene sub-sub-procesos

Luego:

Un sub-proceso es un proceso

Un sub-sub-proceso es un proceso

[]

Un Proceso tiene una estructura

-   Id

-   Name

-   Tamaño en Bytes

-   Status

-   Cuota de Tiempo en milisegundos

-   Prioridad

-   

Los procesos se organizan en una estructura de datos (FIFO) y compiten
entre si para usar el procesador (o procesadores)

[]

Cómo podría el SO asumir la prioridad de un proceso?

-   Tamaño en Bytes del Proceso

-   Estado del proceso (READY, WAIT, RUN, CANCEL,FINISH)

-   Tamaño de la Cuota de Tiempo asignado

■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

Asignar Prioridades al TaskList (TL)

┌ PROCEDURE AssignPriority()

│

│ A = TL[1].Size

│ B = TL[1].Size

│ ┌ FOR i = 1 TO N

│ │ ┌ IF TL[i].Size < A

│ │ │ A = TL[i].Size

│ │ └ END-IF

│ │ ┌IF B < TL[i].Size

│ │ │ B = TL[i].Size

│ │ └END-IF

│ └ END-FOR

│ H = (B-A)/3

│ //Se definen los hitos A=A+0H, A+1H, A+2H, A+3H=B

│ ┌ FOR i = 1 TO N

│ │ ┌ IF TL[i].Size < A+1H

│ │ │ TL[i].Priority = 1

│ │ ├ ELSEIF TL[i].Size < A+2H

│ │ │ TL[i].Priority = 2

│ │ ├ELSE

│ │ │ TL[i].Priority = 3

│ │ └ END-IF

│ └ END-FOR

│

└ END-PROCEDURE

La lista o tabla de procesos es una estructura dinámica porque los
nuevos procesos están ingresando para ser almacenados en memoria y ser
ejecutados por el procesador; y tambien existen procesos que han
finalizado su ejecución.

Se va implementar un modelo dinámico basado en una estructura de lista
enlazada simple que posee 2 punteros, al inicio y al final.

[]

PROCEDURE AdicionarProceso()

PTR= NEW(NODO)

PTR^.NEXT = FIN

FIN = PTR

END

PROCEDURE ObtenerMaximaPrioridad()

  PTR = INI

  AUX= INI

  WHILE PTR != NULL

  IF AUX^.PRIORITY < PTR^.PRIORITY

  AUX = PTR

  END

  PTR = PTR^.NEXT

  END

  RETURN PP

END

PROCEDURE ObtenerMinimaPrioridad()

  PTR = INI

  AUX= INI

  WHILE PTR != NULL

  IF PTR^.PRIORITY < AUX^.PRIORITY

  AUX = PTR

  END

  PTR = PTR^.NEXT

  END

  RETURN PP

END

PROCEDURE AssignPriority()

Transicion de Estados de Procesos

[]

¿Cómo es posible que varios programas se ejecuten de manera
concurrente????

[]

[]

[]

[]
