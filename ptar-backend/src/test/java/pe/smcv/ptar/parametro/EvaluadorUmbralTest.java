package pe.smcv.ptar.parametro;

import org.junit.jupiter.api.Test;
import pe.smcv.ptar.parametro.service.EvaluadorUmbral;
import pe.smcv.ptar.parametro.service.EvaluadorUmbral.Nivel;
import pe.smcv.ptar.parametro.service.EvaluadorUmbral.Tipo;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Regla de alertas de pH con umbral de ejemplo: alerta 6.5-8.5, crítico 6.0-9.0. */
class EvaluadorUmbralTest {

    private static final BigDecimal LMIN = bd("6.5"), LMAX = bd("8.5"), CMIN = bd("6.0"), CMAX = bd("9.0");

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static EvaluadorUmbral.Resultado eval(String valor) {
        return EvaluadorUmbral.evaluar(bd(valor), LMIN, LMAX, CMIN, CMAX).orElse(null);
    }

    @Test
    void valorNormalNoGeneraAlerta() {
        assertNull(eval("7.2"));
    }

    @Test
    void valorIgualAlLimiteNoGeneraAlerta() {
        assertNull(eval("8.5"));
        assertNull(eval("6.5"));
    }

    @Test
    void sobreLimitePeroBajoCriticoEsMedia() {
        EvaluadorUmbral.Resultado r = eval("8.7");
        assertEquals(Nivel.MEDIA, r.nivel());
        assertEquals(Tipo.MAXIMO_EXCEDIDO, r.tipo());
    }

    @Test
    void sobreCriticoEsAlta() {
        EvaluadorUmbral.Resultado r = eval("9.3");
        assertEquals(Nivel.ALTA, r.nivel());
        assertEquals(Tipo.MAXIMO_EXCEDIDO, r.tipo());
    }

    @Test
    void bajoMinimoCriticoEsAltaPorMinimo() {
        EvaluadorUmbral.Resultado r = eval("5.8");
        assertEquals(Nivel.ALTA, r.nivel());
        assertEquals(Tipo.MINIMO_EXCEDIDO, r.tipo());
    }

    @Test
    void soloLimiteMaximo() {
        // Turbidez: solo máximo 50 (alerta) y 100 (crítico)
        assertNull(EvaluadorUmbral.evaluar(bd("0"), null, bd("50"), null, bd("100")).orElse(null));
        assertEquals(Nivel.MEDIA, EvaluadorUmbral.evaluar(bd("60"), null, bd("50"), null, bd("100")).get().nivel());
        assertEquals(Nivel.ALTA, EvaluadorUmbral.evaluar(bd("120"), null, bd("50"), null, bd("100")).get().nivel());
    }

    @Test
    void validacionDeUmbral() {
        assertNull(EvaluadorUmbral.validar(LMIN, LMAX, CMIN, CMAX));
        assertNotNull(EvaluadorUmbral.validar(null, null, null, null));
        assertNotNull(EvaluadorUmbral.validar(bd("9"), bd("8"), null, null));
        assertNotNull(EvaluadorUmbral.validar(LMIN, LMAX, bd("7"), CMAX)); // crítico mínimo dentro del rango normal
        assertNotNull(EvaluadorUmbral.validar(LMIN, LMAX, CMIN, bd("8")));
    }
}
