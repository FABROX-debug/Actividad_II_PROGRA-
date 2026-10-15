import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

//============================================================
// Ventana del simulador. Se lee siguiendo el camino de un proceso:
//   1 Cola de entrada -> 2 Memoria -> 3 Cola de listos -> 4 Procesador -> 5 Línea de tiempo
// La GUI solo PINTA: los hilos del Planificador hacen el trabajo.
//============================================================
public class SimuladorGUI extends JFrame {
    private final Planificador PL = new Planificador();
    private final ModeloTabla Modelo = new ModeloTabla();
    private final JTable Tabla = new JTable(Modelo);
    private final JTextArea TxtLog = new JTextArea();
    private final PanelSistema PnlSistema = new PanelSistema();
    private final PanelTiempo PnlTiempo = new PanelTiempo();
    private final JLabel LblReloj = new JLabel();
    private final JButton BtnIniciar = new JButton("Iniciar");
    private int FilasMostradas = 0;
    private int LogMostrado = -1;

    private static final Font FUENTE = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font FUENTE_B = new Font("SansSerif", Font.BOLD, 14);
    private static final Font FUENTE_CH = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FUENTE_CH_B = new Font("SansSerif", Font.BOLD, 12);
    private static final Font TITULO = new Font("SansSerif", Font.BOLD, 15);

    // Colores de los estados (tabla, leyenda y bitácora usan los mismos)
    private static final Color C_READY = new Color(0, 90, 200);
    private static final Color C_RUN = new Color(0, 140, 0);
    private static final Color C_WAIT = new Color(220, 120, 0);
    private static final Color C_FINISH = Color.GRAY;
    private static final Color C_CANCEL = new Color(210, 0, 0);
    private static final Color GRIS_CAMBIO = new Color(150, 150, 150);
    private static final Color FONDO_RUN = new Color(215, 250, 215);
    private static final Color AZUL_TITULO = new Color(30, 60, 140);

    //------------------------------------------------------------
    public SimuladorGUI() {
        super("Simulador Time-Sharing — Gestión de Procesos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //------------------------- Barra de controles
        JButton btnEjemplo = new JButton("4 tareas ejemplo");
        JButton btnUno = new JButton("+1 proceso");
        JButton btnCinco = new JButton("+5 procesos");
        JButton btnPaso = new JButton("Paso a paso");
        JButton btnCancelar = new JButton("Cancelar seleccionado");
        JButton btnReiniciar = new JButton("Reiniciar");
        JCheckBox chkAuto = new JCheckBox("Llegadas automáticas");
        String[] velocidades = {"0.1x", "0.25x", "0.5x", "1x", "2x"};
        final double[] valores = {0.1, 0.25, 0.5, 1.0, 2.0};
        JComboBox<String> cmbVelocidad = new JComboBox<String>(velocidades);
        cmbVelocidad.setSelectedIndex(1);

        btnEjemplo.setToolTipText("Reinicia y carga 4 tareas como en las imágenes del profe (azul, rojo, amarillo, verde)");
        btnUno.setToolTipText("Llega 1 proceso de tamaño aleatorio (16 a 1024 KB); se recalculan las prioridades");
        btnCinco.setToolTipText("Llegan 5 procesos: sirve para llenar la memoria");
        BtnIniciar.setToolTipText("Inicia o pausa la simulación");
        btnPaso.setToolTipText("Ejecuta UN solo turno (un proceso usa su cuota) y vuelve a pausar");
        btnCancelar.setToolTipText("Selecciona una fila de la tabla: el proceso pasa a CANCEL y libera su memoria");
        btnReiniciar.setToolTipText("Borra todo y deja el reloj en 0");
        chkAuto.setToolTipText("Un hilo genera procesos nuevos cada cierto tiempo");
        cmbVelocidad.setToolTipText("1x = tiempo real; 0.25x = 4 veces más lento (mejor para explicar)");

        btnEjemplo.addActionListener(e -> {
            PL.Reiniciar();
            PL.NuevoProceso("Task 1", 400);
            PL.NuevoProceso("Task 2", 900);
            PL.NuevoProceso("Task 3", 220);
            PL.NuevoProceso("Task 4", 520);
        });
        btnUno.addActionListener(e -> PL.NuevoProcesoAleatorio());
        btnCinco.addActionListener(e -> {
            for (int i = 1; i <= 5; i++) {
                PL.NuevoProcesoAleatorio();
            }
        });
        BtnIniciar.addActionListener(e -> PL.Pausado = !PL.Pausado);
        btnPaso.addActionListener(e -> {
            PL.PasoUnico = true;
            PL.Pausado = false;
        });
        btnCancelar.addActionListener(e -> {
            int fila = Tabla.getSelectedRow();
            if (fila >= 0) {
                PL.Cancelar(PL.TL[fila + 1]);
            }
        });
        btnReiniciar.addActionListener(e -> PL.Reiniciar());
        chkAuto.addActionListener(e -> PL.AutoLlegadas = chkAuto.isSelected());
        cmbVelocidad.addActionListener(e -> PL.Velocidad = valores[cmbVelocidad.getSelectedIndex()]);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        Component[] items = {btnEjemplo, btnUno, btnCinco, BtnIniciar, btnPaso, btnCancelar, btnReiniciar, chkAuto};
        for (Component c : items) {
            c.setFont(FUENTE_B);
            controles.add(c);
        }
        JLabel lblVel = new JLabel("Velocidad:");
        lblVel.setFont(FUENTE);
        cmbVelocidad.setFont(FUENTE);
        controles.add(lblVel);
        controles.add(cmbVelocidad);
        controles.add(LblReloj);
        LblReloj.setFont(new Font("SansSerif", Font.BOLD, 16));

        //------------------------- Leyenda de estados
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2));
        leyenda.add(Etiqueta("Estados:", Color.BLACK, true));
        leyenda.add(Etiqueta("■ READY = listo, espera su turno", C_READY, false));
        leyenda.add(Etiqueta("■ RUN = usando el procesador", C_RUN, false));
        leyenda.add(Etiqueta("■ WAIT = agotó su cuota, vuelve a la cola", C_WAIT, false));
        leyenda.add(Etiqueta("■ FINISH = terminó", C_FINISH, false));
        leyenda.add(Etiqueta("■ CANCEL = cancelado", C_CANCEL, false));

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        controles.setAlignmentX(LEFT_ALIGNMENT);
        leyenda.setAlignmentX(LEFT_ALIGNMENT);
        norte.add(controles);
        norte.add(leyenda);

        //------------------------- Tabla + bitácora
        Tabla.setDefaultRenderer(Object.class, new Renderer());
        Tabla.setRowHeight(26);
        Tabla.setFont(FUENTE);
        Tabla.getTableHeader().setFont(FUENTE_B);
        Tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {45, 105, 55, 80, 50, 55, 55, 55, 105};
        for (int c = 0; c < anchos.length; c++) {
            Tabla.getColumnModel().getColumn(c).setPreferredWidth(anchos[c]);
        }
        TxtLog.setEditable(false);
        TxtLog.setFont(new Font("Monospaced", Font.PLAIN, 13));
        TxtLog.setLineWrap(true);
        TxtLog.setWrapStyleWord(true);
        JScrollPane spTabla = new JScrollPane(Tabla);
        spTabla.setPreferredSize(new Dimension(580, 300));
        spTabla.setBorder(Borde("Tabla de procesos (TL)  —  tamaño en KB, tiempos en ms"));
        JScrollPane spLog = new JScrollPane(TxtLog);
        spLog.setBorder(Borde("Bitácora de transiciones de estado"));
        spLog.setPreferredSize(new Dimension(580, 220));
        JSplitPane izquierda = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTabla, spLog);
        izquierda.setResizeWeight(0.5);

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, PnlSistema);
        centro.setResizeWeight(0.42);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(centro, BorderLayout.CENTER);
        getContentPane().add(PnlTiempo, BorderLayout.SOUTH);

        setSize(1400, 900);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Refresco de pantalla cada 50 ms
        new Timer(50, e -> Refrescar()).start();
    }

    //------------------------------------------------------------
    private void Refrescar() {
        synchronized (PL) {
            int n = PL.N;
            if (n < FilasMostradas) {
                FilasMostradas = n;
                Modelo.fireTableDataChanged();
            } else if (n > FilasMostradas) {
                int antes = FilasMostradas;
                FilasMostradas = n;
                Modelo.fireTableRowsInserted(antes, n - 1);
            }
            if (n > 0) {
                Modelo.fireTableRowsUpdated(0, n - 1);
            }
            if (PL.NLog != LogMostrado) {
                StringBuilder sb = new StringBuilder();
                int desde = Math.max(0, PL.NLog - Planificador.MAX_LOG);
                for (int i = desde; i < PL.NLog; i++) {
                    sb.append(PL.LOG[i % Planificador.MAX_LOG]).append('\n');
                }
                TxtLog.setText(sb.toString());
                TxtLog.setCaretPosition(TxtLog.getDocument().getLength());
                LogMostrado = PL.NLog;
            }
            LblReloj.setText("   Reloj: " + PL.Reloj + " ms");
        }
        BtnIniciar.setText(PL.Pausado ? "Iniciar" : "Pausar");
        PnlSistema.repaint();
        PnlTiempo.repaint();
    }

    //============================================================
    private class ModeloTabla extends AbstractTableModel {
        private final String[] COL = {"Id", "Nombre", "KB", "Estado", "Prior.", "Cuota", "Total", "Resta", "Ubicación"};

        public int getRowCount() { return FilasMostradas; }
        public int getColumnCount() { return COL.length; }
        public String getColumnName(int c) { return COL[c]; }

        public Object getValueAt(int r, int c) {
            Proceso P = PL.TL[r + 1];
            if (P == null) {
                return "";
            }
            switch (c) {
                case 0: return "P" + P.Id;
                case 1: return P.Name;
                case 2: return P.Size;
                case 3: return P.Status;
                case 4: return P.Priority;
                case 5: return P.Quota;
                case 6: return P.TiempoTotal;
                case 7: return P.TiempoRestante;
                default: return Ubicacion(P);
            }
        }
    }

    private String Ubicacion(Proceso P) {
        if (!P.Vivo()) return "—";
        if (P.Status == Estado.RUN) return "Procesador";
        if (P.EnMemoria) return "Memoria";
        return "Cola entrada";
    }

    //============================================================
    private class Renderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foco, r, c);
            Proceso P = PL.TL[r + 1];
            setFont(FUENTE);
            setHorizontalAlignment(c >= 2 && c <= 7 ? CENTER : LEFT);
            if (!sel) {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            if (P == null) {
                return this;
            }
            if (!sel) {
                if (P.Status == Estado.RUN) {
                    setBackground(FONDO_RUN);           // resalta al que usa la CPU
                } else if (!P.Vivo()) {
                    setBackground(new Color(240, 240, 240));
                }
            }
            if (c == 0) {
                setBackground(P.ColorProceso);
                setForeground(ColorTexto(P.ColorProceso));
                setFont(FUENTE_B);
            } else if (c == 3 && !sel) {
                setFont(FUENTE_B);
                setForeground(ColorEstado(P.Status));
            }
            return this;
        }
    }

    //============================================================
    // Panel: 1 cola de entrada, 2 memoria, 3 cola de listos, 4 procesador
    //============================================================
    private class PanelSistema extends JPanel {
        PanelSistema() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(800, 540));
        }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            synchronized (PL) {
                int y = 26;
                Titulo(g, 1, "Cola de entrada — llegan y esperan espacio en memoria (FIFO)", 14, y);
                y = DibujarLista(g, PL.ColaEntrada, y, w);
                y += 10;
                Titulo(g, 2, "Memoria", 14, y);
                y = DibujarMemoria(g, y, w);
                y += 16;
                Titulo(g, 3, "Cola de listos — en memoria; se atiende por INI y vuelve por FIN (round robin)", 14, y);
                y = DibujarLista(g, PL.ColaListos, y, w);
                Proceso max = PL.ColaListos.ObtenerMaximaPrioridad();
                Proceso min = PL.ColaListos.ObtenerMinimaPrioridad();
                g.setFont(FUENTE);
                g.setColor(Color.DARK_GRAY);
                g.drawString("ObtenerMaximaPrioridad() = " + (max == null ? "—" : "P" + max.Id + " (prioridad " + max.Priority + ")")
                        + "      ObtenerMinimaPrioridad() = " + (min == null ? "—" : "P" + min.Id + " (prioridad " + min.Priority + ")"),
                        14, y + 16);
                y += 44;
                DibujarProcesador(g, y);
                DibujarTerciles(g, y);
            }
        }

        //------------------------------------------------------------
        // Dibuja INI -> [P1] -> [P2] -> ... -> null, con FIN señalando al último
        private int DibujarLista(Graphics2D g, ListaProcesos L, int y, int w) {
            int top = y + 10, alto = 40, ancho = 66, gap = 26;
            int x = 14;
            g.setFont(FUENTE_B);
            g.setColor(new Color(0, 0, 160));
            g.drawString("INI", x, top + 25);
            x += 30;
            g.setColor(Color.BLACK);
            Flecha(g, x, top + alto / 2, x + 16);
            x += 20;
            int limite = w - 130;
            int ocultos = 0;
            int xUltimo = -1;
            for (Proceso P = L.INI; P != null; P = P.NEXT) {
                if (x + ancho > limite) {
                    ocultos++;
                    continue;
                }
                g.setColor(P.ColorProceso);
                g.fillRoundRect(x, top, ancho, alto, 8, 8);
                g.setColor(Color.BLACK);
                g.drawRoundRect(x, top, ancho, alto, 8, 8);
                g.setColor(ColorTexto(P.ColorProceso));
                g.setFont(FUENTE_B);
                g.drawString("P" + P.Id, x + 6, top + 17);
                g.setFont(FUENTE_CH);
                String est = P.Status == Estado.WAIT ? "WAIT" : "prior. " + P.Priority;
                g.drawString(est, x + 6, top + 33);
                xUltimo = x;
                x += ancho;
                g.setColor(Color.BLACK);
                Flecha(g, x, top + alto / 2, x + gap - 4);
                x += gap;
            }
            g.setFont(FUENTE);
            g.setColor(Color.GRAY);
            g.drawString(ocultos > 0 ? "… +" + ocultos + " más → null" : "null", x, top + 25);
            if (L.INI == null) {
                g.drawString("(vacía)", x + 50, top + 25);
            }
            // Puntero FIN
            if (L.FIN != null) {
                g.setColor(new Color(160, 0, 0));
                g.setFont(FUENTE_B);
                if (ocultos == 0 && xUltimo >= 0) {
                    g.drawString("FIN ↑", xUltimo + 16, top + alto + 18);
                } else {
                    g.drawString("FIN = P" + L.FIN.Id, x + 110, top + 25);
                }
            }
            return top + alto + 26;
        }

        //------------------------------------------------------------
        private int DibujarMemoria(Graphics2D g, int y, int w) {
            int total = Planificador.MEMORIA_TOTAL;
            g.setFont(FUENTE);
            g.setColor(Color.DARK_GRAY);
            g.drawString(PL.MemoriaUsada + " / " + total + " KB usados   (libre " + (total - PL.MemoriaUsada) + " KB)", 130, y);
            int top = y + 8, ancho = w - 28, alto = 44;
            g.setColor(new Color(235, 235, 235));
            g.fillRect(14, top, ancho, alto);
            int x = 14;
            for (int i = 1; i <= PL.N; i++) {
                Proceso P = PL.TL[i];
                if (!P.EnMemoria) {
                    continue;
                }
                int bw = (int) ((long) P.Size * ancho / total);
                g.setColor(P.ColorProceso);
                g.fillRect(x, top, bw, alto);
                g.setColor(Color.BLACK);
                g.drawRect(x, top, bw, alto);
                if (bw > 30) {
                    g.setColor(ColorTexto(P.ColorProceso));
                    g.setFont(FUENTE_B);
                    g.drawString("P" + P.Id, x + 5, top + 19);
                    g.setFont(FUENTE_CH);
                    if (bw > 60) {
                        g.drawString(P.Size + " KB", x + 5, top + 36);
                    }
                }
                x += bw;
            }
            g.setColor(Color.BLACK);
            g.drawRect(14, top, ancho, alto);
            if (PL.ColaEntrada.INI != null) {
                g.setColor(C_CANCEL);
                g.setFont(FUENTE_B);
                g.drawString("¡Memoria llena para P" + PL.ColaEntrada.INI.Id + " (" + PL.ColaEntrada.INI.Size
                        + " KB)! Espera en la cola de entrada hasta que alguien termine.", 14, top + alto + 20);
                return top + alto + 34;
            }
            return top + alto + 4;
        }

        //------------------------------------------------------------
        private void DibujarProcesador(Graphics2D g, int y) {
            int x = 14, ancho = 380, alto = 140;
            Titulo(g, 4, "Procesador (1 CPU)", x, y);
            Proceso sig = PL.ColaListos.INI;
            g.setFont(FUENTE);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Siguiente: " + (sig == null ? "—" : "P" + sig.Id), x + 230, y);
            int top = y + 10;
            Proceso P = PL.Actual;
            if (PL.CambioContexto) {
                g.setColor(new Color(225, 225, 225));
                g.fillRoundRect(x, top, ancho, alto, 14, 14);
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.BOLD, 17));
                g.drawString("Cambio de contexto (" + Planificador.CAMBIO_CONTEXTO + " ms)", x + 16, top + 36);
                g.setFont(FUENTE);
                g.drawString("Se guarda el estado del proceso que sale", x + 16, top + 66);
                g.drawString("y se carga el siguiente de la cola (INI).", x + 16, top + 88);
            } else if (P != null) {
                g.setColor(P.ColorProceso);
                g.fillRoundRect(x, top, ancho, alto, 14, 14);
                Color ct = ColorTexto(P.ColorProceso);
                g.setColor(ct);
                g.setFont(new Font("SansSerif", Font.BOLD, 19));
                g.drawString("P" + P.Id + "  " + P.Name + "  —  RUN", x + 16, top + 30);
                g.setFont(FUENTE);
                g.drawString("Temporizador de cuota: " + PL.UsadoCuota + " / " + PL.CuotaActual + " ms", x + 16, top + 56);
                Barra(g, x + 16, top + 62, ancho - 32, PL.UsadoCuota, PL.CuotaActual, ct);
                int hecho = P.TiempoTotal - P.TiempoRestante;
                g.setColor(ct);
                g.drawString("Avance total del proceso: " + hecho + " / " + P.TiempoTotal + " ms", x + 16, top + 102);
                Barra(g, x + 16, top + 108, ancho - 32, hecho, P.TiempoTotal, ct);
            } else {
                g.setColor(new Color(245, 240, 200));
                g.fillRoundRect(x, top, ancho, alto, 14, 14);
                g.setColor(Color.DARK_GRAY);
                g.setFont(FUENTE_B);
                g.drawString(PL.Pausado ? "Simulación en pausa — pulsa Iniciar" : "CPU libre (no hay procesos listos)", x + 16, top + 40);
            }
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, top, ancho, alto, 14, 14);
        }

        //------------------------------------------------------------
        private void DibujarTerciles(Graphics2D g, int y) {
            int x = 420;
            g.setFont(TITULO);
            g.setColor(AZUL_TITULO);
            g.drawString("AssignPriority() — terciles del tamaño", x, y);
            g.setFont(FUENTE);
            g.setColor(Color.BLACK);
            if (!PL.HayTerciles) {
                g.drawString("(sin procesos vivos)", x, y + 28);
                return;
            }
            int A = PL.UltA, B = PL.UltB;
            double H = PL.UltH;
            g.drawString("A (mín) = " + A + " KB      B (máx) = " + B + " KB", x, y + 28);
            g.drawString(String.format("H = (B − A) / 3 = %.1f KB", H), x, y + 50);
            if (A == B) {
                g.drawString("A = B → todos con prioridad 2 (evita H = 0)", x, y + 78);
                return;
            }
            g.setFont(FUENTE_B);
            g.drawString(String.format("Prioridad 1 (pequeño):  [%d , %.0f)   → cuota 100 ms", A, A + H), x, y + 78);
            g.drawString(String.format("Prioridad 2 (mediano):  [%.0f , %.0f)   → cuota 200 ms", A + H, A + 2 * H), x, y + 100);
            g.drawString(String.format("Prioridad 3 (grande):   [%.0f , %d]   → cuota 300 ms", A + 2 * H, B), x, y + 122);
            g.setFont(FUENTE);
            g.setColor(Color.GRAY);
            g.drawString("Se recalcula en cada llegada/salida: la prioridad es dinámica.", x, y + 148);
        }
    }

    //============================================================
    // Panel 5: línea de tiempo Time-Sharing vs Serial
    //============================================================
    private class PanelTiempo extends JPanel {
        PanelTiempo() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(1200, 215));
            setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int x0 = 240, ancho = w - x0 - 30;
            synchronized (PL) {
                long serial = 0;
                for (int i = 1; i <= PL.N; i++) {
                    if (PL.TL[i].Status != Estado.CANCEL) {
                        serial += PL.TL[i].TiempoTotal;
                    }
                }
                long escala = Math.max(1, Math.max(PL.Reloj, serial));

                Titulo(g, 5, "Línea de tiempo — mismo trabajo, dos formas de atenderlo", 14, 22);

                //--------------------- Time-sharing
                int yTS = 38;
                g.setFont(FUENTE_B);
                g.setColor(Color.BLACK);
                g.drawString("Time Sharing", 14, yTS + 15);
                g.setFont(FUENTE_CH);
                g.setColor(Color.DARK_GRAY);
                g.drawString("turnos intercalados", 14, yTS + 29);
                g.setColor(new Color(245, 245, 245));
                g.fillRect(x0, yTS, ancho, 32);
                long cambios = 0, tiempoCambios = 0;
                for (int k = 0; k < PL.NSeg; k++) {
                    int px = x0 + (int) (PL.SegIni[k] * ancho / escala);
                    int px2 = x0 + (int) (PL.SegFin[k] * ancho / escala);
                    Proceso P = PL.SegProc[k];
                    if (P == null) {
                        g.setColor(GRIS_CAMBIO);
                        cambios++;
                        tiempoCambios += PL.SegFin[k] - PL.SegIni[k];
                    } else {
                        g.setColor(P.ColorProceso);
                    }
                    g.fillRect(px, yTS, Math.max(1, px2 - px), 32);
                    if (P != null && px2 - px > 26) {
                        g.setColor(ColorTexto(P.ColorProceso));
                        g.setFont(FUENTE_CH_B);
                        g.drawString("P" + P.Id, px + 4, yTS + 21);
                    }
                }

                //--------------------- Serial
                int ySe = 84;
                g.setFont(FUENTE_B);
                g.setColor(Color.BLACK);
                g.drawString("Serial", 14, ySe + 15);
                g.setFont(FUENTE_CH);
                g.setColor(Color.DARK_GRAY);
                g.drawString("uno tras otro, sin turnos", 14, ySe + 29);
                g.setColor(new Color(245, 245, 245));
                g.fillRect(x0, ySe, ancho, 32);
                long acum = 0;
                for (int i = 1; i <= PL.N; i++) {
                    Proceso P = PL.TL[i];
                    if (P.Status == Estado.CANCEL) {
                        continue;
                    }
                    int px = x0 + (int) (acum * ancho / escala);
                    acum += P.TiempoTotal;
                    int px2 = x0 + (int) (acum * ancho / escala);
                    g.setColor(P.ColorProceso);
                    g.fillRect(px, ySe, Math.max(1, px2 - px), 32);
                    g.setColor(Color.BLACK);
                    g.drawLine(px, ySe, px, ySe + 32);
                    if (px2 - px > 26) {
                        g.setColor(ColorTexto(P.ColorProceso));
                        g.setFont(FUENTE_CH_B);
                        g.drawString("P" + P.Id, px + 4, ySe + 21);
                    }
                }

                //--------------------- Eje de tiempo
                int yEje = 130;
                g.setColor(Color.BLACK);
                g.drawLine(x0, yEje, x0 + ancho, yEje);
                g.setFont(FUENTE_CH);
                for (int t = 0; t <= 10; t++) {
                    int px = x0 + t * ancho / 10;
                    g.drawLine(px, yEje - 3, px, yEje + 3);
                    String s = (escala * t / 10) + " ms";
                    int sx = Math.min(px - g.getFontMetrics().stringWidth(s) / 2, w - g.getFontMetrics().stringWidth(s) - 4);
                    g.drawString(s, sx, yEje + 16);
                }

                //--------------------- Totales
                g.setFont(FUENTE_B);
                g.setColor(Color.BLACK);
                g.drawString("Serial:  Tiempo Total = T1 + T2 + ... + Tn = " + serial + " ms", 14, yEje + 44);
                g.drawString("Time Sharing:  reloj = " + PL.Reloj + " ms", 14, yEje + 66);
                g.setFont(FUENTE);
                g.setColor(Color.DARK_GRAY);
                g.drawString("(incluye " + cambios + " cambios de contexto = " + tiempoCambios
                        + " ms, en gris: es el costo de dar la sensación de que todos se ejecutan a la vez)", 330, yEje + 66);
            }
        }
    }

    //============================================================
    // Utilidades de dibujo
    //============================================================
    private static void Titulo(Graphics2D g, int numero, String texto, int x, int y) {
        g.setColor(AZUL_TITULO);
        g.fillOval(x, y - 15, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(FUENTE_B);
        g.drawString(String.valueOf(numero), x + 6, y);
        g.setColor(AZUL_TITULO);
        g.setFont(TITULO);
        g.drawString(texto, x + 28, y);
    }

    private static void Flecha(Graphics2D g, int x1, int y, int x2) {
        g.drawLine(x1, y, x2, y);
        g.drawLine(x2, y, x2 - 6, y - 4);
        g.drawLine(x2, y, x2 - 6, y + 4);
    }

    private static void Barra(Graphics2D g, int x, int y, int ancho, int valor, int max, Color borde) {
        g.setColor(new Color(255, 255, 255, 170));
        g.fillRect(x, y, ancho, 14);
        int lleno = max <= 0 ? 0 : (int) ((long) Math.min(valor, max) * ancho / max);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(x, y, lleno, 14);
        g.setColor(borde);
        g.drawRect(x, y, ancho, 14);
    }

    private static Color ColorTexto(Color c) {
        double lum = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
        return lum > 150 ? Color.BLACK : Color.WHITE;
    }

    private static Color ColorEstado(Estado e) {
        switch (e) {
            case RUN:    return C_RUN;
            case WAIT:   return C_WAIT;
            case READY:  return C_READY;
            case FINISH: return C_FINISH;
            default:     return C_CANCEL;
        }
    }

    private static JLabel Etiqueta(String texto, Color color, boolean negrita) {
        JLabel l = new JLabel(texto);
        l.setFont(negrita ? FUENTE_B : FUENTE);
        l.setForeground(color);
        return l;
    }

    private static javax.swing.border.Border Borde(String titulo) {
        return BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), titulo,
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, FUENTE_B, AZUL_TITULO);
    }

    //============================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimuladorGUI().setVisible(true));
    }
}
