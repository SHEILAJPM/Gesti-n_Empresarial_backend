package desarrolloWeb.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planillas", uniqueConstraints = @UniqueConstraint(name = "uk_planillas_periodo", columnNames = {"anio", "mes"}))
public class Planilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoPlanilla estado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "generada_por", nullable = false, length = 50)
    private String generadaPor;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "aprobada_por", length = 50)
    private String aprobadaPor;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "total_bruto", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalBruto;

    @Column(name = "total_descuentos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(name = "total_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalNeto;

    @Column(name = "total_essalud", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEssalud;

    @OneToMany(mappedBy = "planilla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nombres ASC")
    private List<PlanillaDetalle> detalles = new ArrayList<>();

    public void agregar(PlanillaDetalle detalle) {
        detalle.setPlanilla(this);
        detalles.add(detalle);
    }

    /* Recalcula los totales a partir de los detalles */
    public void totalizar() {
        totalBruto = sumar(detalles.stream().map(PlanillaDetalle::getRemuneracionBruta).toList());
        totalDescuentos = sumar(detalles.stream().map(PlanillaDetalle::getDescuentoPension).toList());
        totalNeto = sumar(detalles.stream().map(PlanillaDetalle::getNeto).toList());
        totalEssalud = sumar(detalles.stream().map(PlanillaDetalle::getEssalud).toList());
    }

    private static BigDecimal sumar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public EstadoPlanilla getEstado() {
        return estado;
    }

    public void setEstado(EstadoPlanilla estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getGeneradaPor() {
        return generadaPor;
    }

    public void setGeneradaPor(String generadaPor) {
        this.generadaPor = generadaPor;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getAprobadaPor() {
        return aprobadaPor;
    }

    public void setAprobadaPor(String aprobadaPor) {
        this.aprobadaPor = aprobadaPor;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public BigDecimal getTotalBruto() {
        return totalBruto;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public BigDecimal getTotalNeto() {
        return totalNeto;
    }

    public BigDecimal getTotalEssalud() {
        return totalEssalud;
    }

    public List<PlanillaDetalle> getDetalles() {
        return detalles;
    }
}
