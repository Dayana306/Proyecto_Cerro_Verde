package pe.smcv.ptar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PtarApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtarApplication.class, args);
    }
}
