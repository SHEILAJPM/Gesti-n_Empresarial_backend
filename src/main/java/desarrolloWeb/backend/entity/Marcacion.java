
package desarrolloWeb.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marcaciones")
public class Marcacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trabajador_id", nullable = false)
    private Long trabajadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMarcacion tipo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // RRHH o PORTAL
    @Column(nullable = false, length = 10)
    private String origen = "RRHH";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "precision_metros", precision = 8, scale = 2)
    private BigDecimal precisionMetros;

    @Column(name = "distancia_metros", precision = 8, scale = 2)
    private BigDecimal distanciaMetros;

    public Marcacion() {
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

    public void setTrabajadorId(Long trabajadorId) {
        this.trabajadorId = trabajadorId;
    }

    public TipoMarcacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoMarcacion tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
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
}
