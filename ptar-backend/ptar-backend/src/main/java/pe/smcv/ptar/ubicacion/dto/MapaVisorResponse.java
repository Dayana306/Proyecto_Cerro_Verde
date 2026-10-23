package pe.smcv.ptar.ubicacion.dto;

import java.util.List;

/** Todo lo que necesita el visor 2D (RF-08) en una sola llamada. mapaBase puede ser null. */
public record MapaVisorResponse(
        Mapa2dDto.Response mapaBase,
        List<ZonaDto.Response> zonas,
        List<PuntoVisor> puntos) {

    /** Punto con su estado de alertas, para pintarlo de color en el mapa. */
    public record PuntoVisor(PuntoDto.Response punto, long alertasAbiertas, String nivelMaximo) {}
}
