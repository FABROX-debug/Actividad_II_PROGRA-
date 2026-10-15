//============================================================
// Lista enlazada simple con 2 punteros: INI y FIN (docs/img/03)
//
// Correcciones al pseudocódigo del profe:
//  1) Los enlaces van INI -> ... -> FIN (cola FIFO): se encola por FIN y se
//     atiende por INI. Con los enlaces al revés (FIN -> ... -> INI), recorrer
//     desde INI con NEXT solo visitaría un nodo.
//  2) ObtenerMaxima/MinimaPrioridad devuelven AUX (no "PP").
//============================================================
public class ListaProcesos {
    public Proceso INI = null;
    public Proceso FIN = null;
    private int Cantidad = 0;

    //------------------------------------------------------------
    // Encola al final (por FIN)
    public void AdicionarProceso(Proceso PTR) {
        PTR.NEXT = null;
        if (FIN == null) {          // Lista vacía
            INI = PTR;
            FIN = PTR;
        } else {
            FIN.NEXT = PTR;
            FIN = PTR;
        }
        Cantidad++;
    }

    //------------------------------------------------------------
    // Atiende al primero (por INI)
    public Proceso SacarInicio() {
        if (INI == null) {
            return null;
        }
        Proceso PTR = INI;
        INI = INI.NEXT;
        if (INI == null) {
            FIN = null;
        }
        PTR.NEXT = null;
        Cantidad--;
        return PTR;
    }

    //------------------------------------------------------------
    // Quita un nodo cualquiera (para CANCEL)
    public boolean Eliminar(Proceso P) {
        Proceso ANT = null;
        Proceso PTR = INI;
        while (PTR != null && PTR != P) {
            ANT = PTR;
            PTR = PTR.NEXT;
        }
        if (PTR == null) {
            return false;
        }
        if (ANT == null) {
            INI = PTR.NEXT;
        } else {
            ANT.NEXT = PTR.NEXT;
        }
        if (FIN == PTR) {
            FIN = ANT;
        }
        PTR.NEXT = null;
        Cantidad--;
        return true;
    }

    //------------------------------------------------------------
    public Proceso ObtenerMaximaPrioridad() {
        Proceso PTR = INI;
        Proceso AUX = INI;
        while (PTR != null) {
            if (AUX.Priority < PTR.Priority) {
                AUX = PTR;
            }
            PTR = PTR.NEXT;
        }
        return AUX;
    }

    //------------------------------------------------------------
    public Proceso ObtenerMinimaPrioridad() {
        Proceso PTR = INI;
        Proceso AUX = INI;
        while (PTR != null) {
            if (PTR.Priority < AUX.Priority) {
                AUX = PTR;
            }
            PTR = PTR.NEXT;
        }
        return AUX;
    }

    //------------------------------------------------------------
    public int Cantidad() {
        return Cantidad;
    }

    public void Limpiar() {
        INI = null;
        FIN = null;
        Cantidad = 0;
    }
}
