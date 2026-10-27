package pe.smcv.ptar.monitoreo.dto;

import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.monitoreo.entity.Documento;

import java.time.LocalDateTime;

public record DocumentoResponse(
        Integer idDocumento,
        String nombreOriginal,
        String tipoContenido,
        Long tamanoBytes,
        String descripcion,
        Estado estado,
        LocalDateTime fechaSubida,
        Integer idMonitoreo,
        String subidoPor,
        String urlDescarga) {

    public static DocumentoResponse desde(Documento d) {
        return new DocumentoResponse(d.getIdDocumento(), d.getNombreOriginal(), d.getTipoContenido(),
                d.getTamanoBytes(), d.getDescripcion(), d.getEstado(), d.getFechaSubida(),
                d.getMonitoreo().getIdMonitoreo(), d.getUsuario().getNombreCompleto(),
                "/api/documentos/" + d.getIdDocumento() + "/descarga");
    }
}
