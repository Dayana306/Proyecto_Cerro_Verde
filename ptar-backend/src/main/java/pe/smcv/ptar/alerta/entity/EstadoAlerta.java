package pe.smcv.ptar.alerta.entity;

/** Ciclo de vida: ABIERTA -> EN_REVISION -> CERRADA (CERRADA es definitivo). */
public enum EstadoAlerta {
    ABIERTA,
    EN_REVISION,
    CERRADA
}
