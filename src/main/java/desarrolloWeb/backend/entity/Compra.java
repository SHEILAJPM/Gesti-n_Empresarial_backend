package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "compras",
        uniqueConstraints = @UniqueConstraint(name = "uk_compras_proveedor_numero",
                columnNames = {"proveedor_id", "numero_comprobante"}),
        indexes = @Index(name = "idx_compras_fecha", columnList = "fecha"))
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "numero_comprobante", nullable = false, length = 20)
    private String numeroComprobante;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "monto_sin_igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSinIgv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "tasa_aplicada", nullable = false, precision = 5, scale = 4)
    private BigDecimal tasaAplicada;

    public Compra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMontoSinIgv() {
        return montoSinIgv;
    }

    public void setMontoSinIgv(BigDecimal montoSinIgv) {
        this.montoSinIgv = montoSinIgv;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getTasaAplicada() {
        return tasaAplicada;
    }

    public void setTasaAplicada(BigDecimal tasaAplicada) {
        this.tasaAplicada = tasaAplicada;
    }
}
