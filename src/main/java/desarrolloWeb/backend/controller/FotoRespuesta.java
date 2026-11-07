package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.entity.FotoAsistencia;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

/* Respuesta HTTP con la imagen de una foto de asistencia. Nunca cambia, así que el navegador la puede guardar,
   pero solo en su caché privada (es un dato personal). */
final class FotoRespuesta {

    private FotoRespuesta() {
    }

    static ResponseEntity<byte[]> de(FotoAsistencia foto) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoMime()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(foto.getContenido());
    }
}
