package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/* Línea de la planilla para un trabajador: copia sus datos del momento para que la boleta no cambie */
@Entity
@Table(name = "planilla_detalles")
public class PlanillaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planilla_id", nullable = false)
    private Planilla planilla;

    @Column(name = "trabajador_id", nullable = false)
    private Long trabajadorId;

    @Column(nullable = false, length = 150)
    private String nombres;

    @Column(name = "documento_identidad", nullable = false, length = 12)
    private String documentoIdentidad;

    @Column(length = 100)
    private String cargo;

    @Column(length = 100)
    private String area;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "sueldo_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal sueldoBase;

    @Column(name = "dias_laborados", nullable = false)
    private Integer diasLaborados;

    @Column(name = "remuneracion_bruta", nullable = false, precision = 12, scale = 2)
    private BigDecimal remuneracionBruta;

    @Column(name = "sistema_pension", nullable = false, length = 3)
    private String sistemaPension;

    @Column(length = 30)
    private String afp;

    @Column(name = "tasa_pension", nullable = false, precision = 6, scale = 4)
    private BigDecimal tasaPension;

    @Column(name = "descuento_pension", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoPension;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal essalud;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal neto;

    // Pago creado al aprobar la planilla
    @Column(name = "pago_id")
    private Long pagoId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Planilla getPlanilla() {
        return planilla;
    }

    public void setPlanilla(Planilla planilla) {
        this.planilla = planilla;
    }

    public Long getTrabajadorId() {
        return trabajadorId;
    }

    public void setTrabajadorId(Long trabajadorId) {
        this.trabajadorId = trabajadorId;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public BigDecimal getSueldoBase() {
        return sueldoBase;
    }

    public void setSueldoBase(BigDecimal sueldoBase) {
        this.sueldoBase = sueldoBase;
    }

    public Integer getDiasLaborados() {
        return diasLaborados;
    }

    public void setDiasLaborados(Integer diasLaborados) {
        this.diasLaborados = diasLaborados;
    }

    public BigDecimal getRemuneracionBruta() {
        return remuneracionBruta;
    }

    public void setRemuneracionBruta(BigDecimal remuneracionBruta) {
        this.remuneracionBruta = remuneracionBruta;
    }

    public String getSistemaPension() {
        return sistemaPension;
    }

    public void setSistemaPension(String sistemaPension) {
        this.sistemaPension = sistemaPension;
    }

    public String getAfp() {
        return afp;
    }

    public void setAfp(String afp) {
        this.afp = afp;
    }

    public BigDecimal getTasaPension() {
        return tasaPension;
    }

    public void setTasaPension(BigDecimal tasaPension) {
        this.tasaPension = tasaPension;
    }

    public BigDecimal getDescuentoPension() {
        return descuentoPension;
    }

    public void setDescuentoPension(BigDecimal descuentoPension) {
        this.descuentoPension = descuentoPension;
    }

    public BigDecimal getEssalud() {
        return essalud;
    }

    public void setEssalud(BigDecimal essalud) {
        this.essalud = essalud;
    }

    public BigDecimal getNeto() {
        return neto;
    }

    public void setNeto(BigDecimal neto) {
        this.neto = neto;
    }

    public Long getPagoId() {
        return pagoId;
    }

    public void setPagoId(Long pagoId) {
        this.pagoId = pagoId;
    }
}
