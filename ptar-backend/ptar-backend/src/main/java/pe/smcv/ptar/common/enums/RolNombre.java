package pe.smcv.ptar.common.enums;

/**
 * Nombres de rol sembrados en V2__datos_semilla.sql.
 * En Spring Security la autoridad es "ROLE_" + nombre, por eso se usa hasRole('ADMINISTRADOR').
 */
public enum RolNombre {
    ADMINISTRADOR,
    SUPERVISOR,
    GERENTE,
    AUDITOR
}
