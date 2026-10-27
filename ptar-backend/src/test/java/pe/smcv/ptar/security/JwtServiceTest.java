package pe.smcv.ptar.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "secreto_de_prueba_con_mas_de_32_caracteres_ok";

    private static UsuarioPrincipal usuario() {
        return new UsuarioPrincipal(1, "admin@smcv.local", "Admin Sistema", "ADMINISTRADOR", "hash", true);
    }

    @Test
    void generaTokenYRecuperaElCorreo() {
        JwtService service = new JwtService(new JwtProperties(SECRETO, 60));
        String token = service.generarToken(usuario());
        assertEquals("admin@smcv.local", service.extraerCorreo(token));
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        JwtService emisor = new JwtService(new JwtProperties("otro_secreto_distinto_de_32_caracteres_xx", 60));
        JwtService validador = new JwtService(new JwtProperties(SECRETO, 60));
        String token = emisor.generarToken(usuario());
        assertThrows(JwtException.class, () -> validador.extraerCorreo(token));
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService service = new JwtService(new JwtProperties(SECRETO, -1));
        String token = service.generarToken(usuario());
        assertThrows(JwtException.class, () -> service.extraerCorreo(token));
    }

    @Test
    void exigeSecretoDeAlMenos32Caracteres() {
        assertThrows(IllegalStateException.class, () -> new JwtService(new JwtProperties("corto", 60)));
    }
}
