import java.awt.Color;

//============================================================
// Un Proceso es un NODO de la lista enlazada.
// Estructura del profe: Id, Name, Tamaño, Status, Cuota, Prioridad (docs/img/02)
//============================================================
public class Proceso {
    public int Id;
    public String Name;
    public int Size;              // Tamaño en KB
    public Estado Status;
    public int Priority;          // 1, 2 o 3 (terciles del tamaño); 0 = aún sin asignar
    public int Quota;             // Cuota de tiempo en ms = Priority * 100
    public int TiempoTotal;       // ms de CPU que necesita para terminar
    public int TiempoRestante;    // ms de CPU que le faltan
    public boolean EnMemoria;
    public long Llegada;          // Reloj (ms) en que llegó
    public long Fin = -1;         // Reloj (ms) en que terminó o fue cancelado
    public Color ColorProceso;
    public Proceso NEXT;          // Enlace al siguiente nodo

    public Proceso(int id, String name, int size, Color color, long llegada) {
        Id = id;
        Name = name;
        Size = size;
        ColorProceso = color;
        Llegada = llegada;
        Status = Estado.READY;
        // Más grande = más trabajo: el tiempo de CPU es proporcional al tamaño (múltiplo de 10 ms)
        TiempoTotal = ((40 + 2 * size) / 10) * 10;
        TiempoRestante = TiempoTotal;
        NEXT = null;
    }

    public boolean Vivo() {
        return Status != Estado.FINISH && Status != Estado.CANCEL;
    }
}
