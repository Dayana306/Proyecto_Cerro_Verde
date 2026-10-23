package pe.smcv.ptar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PtarApplication {

    /** Zona horaria única para todo el sistema (fechas en BD = hora de Perú). */
    public static final String ZONA_HORARIA = "America/Lima";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        SpringApplication.run(PtarApplication.class, args);
    }
}
