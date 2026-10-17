package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Marcación rechazada (fuera del radio o GPS impreciso); queda para revisión de RRHH */
@Entity
@Table(name = "intentos_marcacion")
public class IntentoMarcacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trabajador_id", nullable = false)
    private Long trabajadorId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMarcacion tipo;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "precision_metros", nullable = false, precision = 8, scale = 2)
    private BigDecimal precisionMetros;

    @Column(name = "distancia_metros", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanciaMetros;

    @Column(nullable = false, length = 200)
    private String motivo;

    public Long getId() {
        return id;
    }

    public Long getTrabajadorId() {
        return trabajadorId;
    }

    public void setTrabajadorId(Long trabajadorId) {
        this.trabajadorId = trabajadorId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public TipoMarcacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoMarcacion tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public BigDecimal getPrecisionMetros() {
        return precisionMetros;
    }

    public void setPrecisionMetros(BigDecimal precisionMetros) {
        this.precisionMetros = precisionMetros;
    }

    public BigDecimal getDistanciaMetros() {
        return distanciaMetros;
    }

    public void setDistanciaMetros(BigDecimal distanciaMetros) {
        this.distanciaMetros = distanciaMetros;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
