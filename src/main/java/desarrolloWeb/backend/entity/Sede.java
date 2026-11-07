package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

/* Punto de la empresa para la asistencia por GPS */
@Entity
@Table(name = "sedes")
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    // Distancia máxima al punto para poder marcar
    @Column(name = "radio_metros", nullable = false)
    private Integer radioMetros;

    // Error máximo aceptado del GPS del celular
    @Column(name = "precision_maxima", nullable = false)
    private Integer precisionMaxima;

    // Horario: con él se calculan tardanzas (entrada después de hora_entrada + tolerancia) y faltas
    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada = LocalTime.of(8, 0);

    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida = LocalTime.of(17, 0);

    @Column(name = "tolerancia_minutos", nullable = false)
    private Integer toleranciaMinutos = 10;

    @Column(nullable = false)
    private boolean activa = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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

    public Integer getRadioMetros() {
        return radioMetros;
    }

    public void setRadioMetros(Integer radioMetros) {
        this.radioMetros = radioMetros;
    }

    public Integer getPrecisionMaxima() {
        return precisionMaxima;
    }

    public void setPrecisionMaxima(Integer precisionMaxima) {
        this.precisionMaxima = precisionMaxima;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public Integer getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public void setToleranciaMinutos(Integer toleranciaMinutos) {
        this.toleranciaMinutos = toleranciaMinutos;
    }
}
