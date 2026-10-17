package desarrolloWeb.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String tipoBeneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trabajador_id")
    private Trabajador trabajador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(nullable = false, length = 250)
    private String concepto;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fechaProgramada;

    private LocalDateTime fechaPago;

    @Column(nullable = false, length = 20)
    private String estado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoBeneficiario() {
        return tipoBeneficiario;
    }

    public void setTipoBeneficiario(String tipoBeneficiario) {
        this.tipoBeneficiario = tipoBeneficiario;
    }

    public Long getTrabajadorId() {
        return trabajador == null ? null : trabajador.getId();
    }

    @JsonIgnore
    public Trabajador getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(Trabajador trabajador) {
        this.trabajador = trabajador;
    }

    public Long getProveedorId() {
        return proveedor == null ? null : proveedor.getId();
    }

    @JsonIgnore
    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Double getMonto() {
        return monto == null ? null : monto.doubleValue();
    }

    public void setMonto(Double monto) {
        this.monto = monto == null
                ? null
                : BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(LocalDate fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
