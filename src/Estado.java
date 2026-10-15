// Estados de un proceso (diagrama docs/img/04_transicion_estados.png)
//   READY -> RUN -> WAIT -> READY
//   RUN -> FINISH | CANCEL
public enum Estado {
    READY, RUN, WAIT, FINISH, CANCEL
}
