package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/* Foto tomada con la cámara del celular al marcar asistencia desde el portal.
   Está en su propia tabla para que listar marcaciones no cargue las imágenes. */
@Entity
@Table(name = "fotos_asistencia")
public class FotoAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trabajador_id", nullable = false)
    private Long trabajadorId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "tipo_mime", nullable = false, length = 30)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    @Column(nullable = false)
    private byte[] contenido;

    public FotoAsistencia() {
    }

    public FotoAsistencia(Long trabajadorId, LocalDateTime fechaHora, String tipoMime, byte[] contenido) {
        this.trabajadorId = trabajadorId;
        this.fechaHora = fechaHora;
        this.tipoMime = tipoMime;
        this.contenido = contenido;
        this.tamanoBytes = contenido.length;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTrabajadorId() {
        return trabajadorId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public Integer getTamanoBytes() {
        return tamanoBytes;
    }

    public byte[] getContenido() {
        return contenido;
    }
}
