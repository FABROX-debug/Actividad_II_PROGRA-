import java.awt.Color;

//============================================================
// Planificador Time-Sharing (round robin FIFO, 1 procesador)
//
// Hilos (como en clase: new Thread(new Runnable(){...})):
//   - HiloCPU:      saca el proceso de INI de la cola de listos, lo ejecuta
//                   durante su cuota y lo vuelve a encolar por FIN.
//   - HiloLlegadas: genera procesos nuevos cada cierto tiempo (opcional).
// La GUI solo lee el estado. Todo acceso compartido es "synchronized".
//============================================================
public class Planificador {
    public static final int MEMORIA_TOTAL = 2048;   // KB
    public static final int TICK = 10;              // ms simulados por paso del reloj
    public static final int CAMBIO_CONTEXTO = 30;   // ms simulados para cambiar de proceso
    public static final int MAX = 500;              // máximo de procesos en la tabla
    public static final int MAX_SEG = 20000;        // máximo de franjas en la línea de tiempo
    public static final int MAX_LOG = 300;

    private static final Color[] COLORES = {
        new Color(0, 162, 232), new Color(237, 28, 36), new Color(255, 221, 0), new Color(34, 177, 76),
        new Color(163, 73, 164), new Color(255, 127, 39), new Color(0, 168, 168), new Color(255, 105, 180),
        new Color(153, 102, 51), new Color(63, 72, 204), new Color(181, 230, 29), new Color(112, 146, 190)
    };
    private static final String[] NOMBRES = {
        "Editor", "Navegador", "Compilador", "Reproductor", "Antivirus", "Calculadora",
        "Juego", "Terminal", "Correo", "Explorador", "Impresion", "Backup"
    };

    // TL = Task List: tabla de TODOS los procesos, índices 1..N (como en el pseudocódigo)
    public final Proceso[] TL = new Proceso[MAX + 1];
    public int N = 0;

    public final ListaProcesos ColaEntrada = new ListaProcesos();  // esperan espacio en memoria
    public final ListaProcesos ColaListos = new ListaProcesos();   // en memoria, round robin

    public Proceso Actual = null;     // proceso en RUN (null = CPU libre)
    public int CuotaActual = 0;       // cuota con la que entró al procesador
    public int UsadoCuota = 0;        // ms usados de esa cuota
    public boolean CambioContexto = false;
    public int MemoriaUsada = 0;      // KB
    public long Reloj = 0;            // ms simulados

    // Últimos terciles calculados (para mostrarlos en pantalla)
    public boolean HayTerciles = false;
    public int UltA, UltB;
    public double UltH;

    // Franjas de la línea de tiempo (proceso null = cambio de contexto)
    public final Proceso[] SegProc = new Proceso[MAX_SEG];
    public final long[] SegIni = new long[MAX_SEG];
    public final long[] SegFin = new long[MAX_SEG];
    public int NSeg = 0;

    // Bitácora circular de eventos
    public final String[] LOG = new String[MAX_LOG];
    public int NLog = 0;

    public volatile boolean Pausado = true;
    public volatile boolean PasoUnico = false;  // ejecuta un solo turno y vuelve a pausar
    public volatile boolean AutoLlegadas = false;
    public volatile double Velocidad = 0.25;   // 1.0 = tiempo real
    private int Generacion = 0;                // cambia al Reiniciar

    //------------------------------------------------------------
    public Planificador() {
        Thread CPU = new Thread(new Runnable() {
            public void run() {
                HiloCPU();
            }
        });
        CPU.setDaemon(true);
        CPU.start();
        //-------------------------------------------------
        Thread Llegadas = new Thread(new Runnable() {
            public void run() {
                HiloLlegadas();
            }
        });
        Llegadas.setDaemon(true);
        Llegadas.start();
    }

    //============================================================
    // Llega un proceso: se encola por FIN en la cola de entrada,
    // se recalculan prioridades de todos y se intenta cargar en memoria.
    //============================================================
    public synchronized Proceso NuevoProceso(String name, int size) {
        if (N == MAX) {
            return null;
        }
        N++;
        String nombre = (name != null) ? name : NOMBRES[(N - 1) % NOMBRES.length];
        Proceso P = new Proceso(N, nombre, size, COLORES[(N - 1) % COLORES.length], Reloj);
        TL[N] = P;
        ColaEntrada.AdicionarProceso(P);
        Log("P" + N + " " + nombre + " llega (" + size + " KB, necesita " + P.TiempoTotal + " ms de CPU)");
        AssignPriority();
        CargarEnMemoria();
        return P;
    }

    public Proceso NuevoProcesoAleatorio() {
        int A = 16, B = 1024;   // rango de tamaños en KB
        return NuevoProceso(null, A + (int) (Math.random() * (B - A + 1)));
    }

    //============================================================
    // AssignPriority() — pseudocódigo del profe sobre TL[1..N]
    // (solo procesos vivos). Se ejecuta en cada llegada/salida: la
    // prioridad es dinámica.
    //============================================================
    public synchronized void AssignPriority() {
        int A = 0, B = 0;
        boolean primero = true;
        for (int i = 1; i <= N; i++) {
            if (!TL[i].Vivo()) {
                continue;
            }
            if (primero) {
                A = TL[i].Size;
                B = TL[i].Size;
                primero = false;
            }
            if (TL[i].Size < A) {
                A = TL[i].Size;
            }
            if (B < TL[i].Size) {
                B = TL[i].Size;
            }
        }
        if (primero) {          // No hay procesos vivos
            HayTerciles = false;
            return;
        }
        double H = (B - A) / 3.0;
        // Hitos: A+0H, A+1H, A+2H, A+3H = B
        for (int i = 1; i <= N; i++) {
            if (!TL[i].Vivo()) {
                continue;
            }
            int prioridad;
            if (A == B) {                       // Todos iguales: H = 0, sin terciles
                prioridad = 2;
            } else if (TL[i].Size < A + 1 * H) {
                prioridad = 1;
            } else if (TL[i].Size < A + 2 * H) {
                prioridad = 2;
            } else {
                prioridad = 3;
            }
            if (TL[i].Priority != 0 && TL[i].Priority != prioridad) {
                Log("   P" + i + " cambia de prioridad " + TL[i].Priority + " -> " + prioridad
                    + " (cuota " + (prioridad * 100) + " ms)");
            }
            TL[i].Priority = prioridad;
            TL[i].Quota = prioridad * 100;
        }
        HayTerciles = true;
        UltA = A;
        UltB = B;
        UltH = H;
    }

    //------------------------------------------------------------
    // Mientras el primero de la cola de entrada quepa, pasa a memoria (FIFO)
    private void CargarEnMemoria() {
        while (ColaEntrada.INI != null && MemoriaUsada + ColaEntrada.INI.Size <= MEMORIA_TOTAL) {
            Proceso P = ColaEntrada.SacarInicio();
            P.EnMemoria = true;
            MemoriaUsada += P.Size;
            ColaListos.AdicionarProceso(P);
            Log("P" + P.Id + " cargado en memoria (" + MemoriaUsada + "/" + MEMORIA_TOTAL + " KB)");
        }
        // Si el primero no cabe, se queda esperando (memoria llena) hasta que alguien libere
    }

    private void LiberarMemoria(Proceso P) {
        if (P.EnMemoria) {
            P.EnMemoria = false;
            MemoriaUsada -= P.Size;
        }
    }

    //============================================================
    // CANCEL: desde cualquier estado vivo
    //============================================================
    public synchronized void Cancelar(Proceso P) {
        if (P == null || !P.Vivo()) {
            return;
        }
        Estado antes = P.Status;
        if (P == Actual) {
            Actual = null;              // el HiloCPU detecta que ya no es el actual
        } else if (!ColaEntrada.Eliminar(P)) {
            ColaListos.Eliminar(P);
        }
        LiberarMemoria(P);
        P.Status = Estado.CANCEL;
        P.Fin = Reloj;
        Log("P" + P.Id + " " + antes + " -> CANCEL (libera " + P.Size + " KB)");
        AssignPriority();
        CargarEnMemoria();
    }

    //============================================================
    public synchronized void Reiniciar() {
        Generacion++;
        for (int i = 1; i <= N; i++) {
            TL[i] = null;
        }
        N = 0;
        ColaEntrada.Limpiar();
        ColaListos.Limpiar();
        Actual = null;
        CuotaActual = 0;
        UsadoCuota = 0;
        CambioContexto = false;
        MemoriaUsada = 0;
        Reloj = 0;
        NSeg = 0;
        NLog = 0;
        HayTerciles = false;
        Pausado = true;
        PasoUnico = false;
    }

    //============================================================
    // HILO CPU — Round Robin
    //============================================================
    private void HiloCPU() {
        while (true) {
            if (Pausado) {
                Dormir(50);
                continue;
            }
            //-------------------------------------------------
            // 1) Despacho: se atiende por INI
            Proceso P;
            int gen;
            int cuota;
            synchronized (this) {
                gen = Generacion;
                CargarEnMemoria();
                // Los que cedieron el procesador (WAIT) vuelven a READY
                for (Proceso Q = ColaListos.INI; Q != null; Q = Q.NEXT) {
                    if (Q.Status == Estado.WAIT) {
                        Q.Status = Estado.READY;
                        Log("P" + Q.Id + " WAIT -> READY");
                    }
                }
                P = ColaListos.SacarInicio();
                cuota = 0;
                if (P != null) {
                    P.Status = Estado.RUN;
                    Actual = P;
                    cuota = P.Quota;
                    CuotaActual = cuota;
                    UsadoCuota = 0;
                    NuevaFranja(P);
                    Log("P" + P.Id + " READY -> RUN (cuota " + cuota + " ms, le faltan " + P.TiempoRestante + " ms)");
                }
            }
            if (P == null) {            // CPU libre
                if (PasoUnico) {
                    PasoUnico = false;
                    Pausado = true;
                }
                Dormir(50);
                continue;
            }
            //-------------------------------------------------
            // 2) Ejecución durante la cuota (temporizador)
            while (true) {
                if (Pausado) {
                    Dormir(50);
                    continue;
                }
                Dormir(Math.max(1, (long) (TICK / Velocidad)));
                synchronized (this) {
                    if (gen != Generacion || Actual != P) {     // Reiniciado o cancelado
                        break;
                    }
                    int dt = Math.min(TICK, Math.min(P.TiempoRestante, cuota - UsadoCuota));
                    Reloj += dt;
                    UsadoCuota += dt;
                    P.TiempoRestante -= dt;
                    SegFin[NSeg - 1] = Reloj;
                    if (P.TiempoRestante <= 0) {
                        P.Status = Estado.FINISH;
                        P.Fin = Reloj;
                        LiberarMemoria(P);
                        Actual = null;
                        Log("P" + P.Id + " RUN -> FINISH (libera " + P.Size + " KB)");
                        AssignPriority();
                        CargarEnMemoria();
                        break;
                    }
                    if (UsadoCuota >= cuota) {
                        P.Status = Estado.WAIT;
                        ColaListos.AdicionarProceso(P);     // vuelve a la cola por FIN
                        Actual = null;
                        Log("P" + P.Id + " RUN -> WAIT (cuota agotada, vuelve al FIN de la cola)");
                        break;
                    }
                }
            }
            //-------------------------------------------------
            // 3) Cambio de contexto (solo si hay OTRO proceso esperando)
            boolean hayCambio;
            synchronized (this) {
                hayCambio = gen == Generacion && ColaListos.INI != null
                        && !(ColaListos.INI == P && P.NEXT == null);
                if (hayCambio) {
                    CambioContexto = true;
                    NuevaFranja(null);
                }
            }
            if (hayCambio) {
                int hecho = 0;
                while (hecho < CAMBIO_CONTEXTO) {
                    if (Pausado) {
                        Dormir(50);
                        continue;
                    }
                    Dormir(Math.max(1, (long) (TICK / Velocidad)));
                    synchronized (this) {
                        if (gen != Generacion) {
                            break;
                        }
                        Reloj += TICK;
                        hecho += TICK;
                        SegFin[NSeg - 1] = Reloj;
                    }
                }
                synchronized (this) {
                    CambioContexto = false;
                }
            }
            //-------------------------------------------------
            // 4) Modo "Paso a paso": un turno y pausa
            if (PasoUnico) {
                PasoUnico = false;
                Pausado = true;
            }
        }
    }

    //============================================================
    // HILO LLEGADAS — procesos nuevos cada 0.8 a 3.2 s simulados
    //============================================================
    private void HiloLlegadas() {
        double acumulado = 0;
        double proxima = 1500;
        while (true) {
            Dormir(100);
            if (Pausado || !AutoLlegadas) {
                continue;
            }
            acumulado += 100 * Velocidad;
            if (acumulado >= proxima) {
                NuevoProcesoAleatorio();
                acumulado = 0;
                proxima = 800 + Math.random() * 2400;
            }
        }
    }

    //------------------------------------------------------------
    private void NuevaFranja(Proceso P) {
        if (NSeg < MAX_SEG) {
            SegProc[NSeg] = P;
            SegIni[NSeg] = Reloj;
            SegFin[NSeg] = Reloj;
            NSeg++;
        }
    }

    private void Log(String s) {
        LOG[NLog % MAX_LOG] = String.format("[%6d ms] %s", Reloj, s);
        NLog++;
    }

    private static void Dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException E) {
        }
    }
}
