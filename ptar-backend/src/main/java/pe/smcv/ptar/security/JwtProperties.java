package pe.smcv.ptar.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

//Se enlaza con app.jwt de application.yml
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
