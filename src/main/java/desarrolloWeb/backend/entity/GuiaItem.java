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

/* Bien trasladado en una guía (sin precios: la guía sustenta el traslado, no la venta) */
@Entity
@Table(name = "guia_items")
public class GuiaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_id", nullable = false)
    private Guia guia;

    @Column(nullable = false)
    private Integer orden;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 250)
    private String descripcion;

    @Column(nullable = false, length = 3)
    private String unidad;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    public Long getId() { return id; }
    public Guia getGuia() { return guia; }
    public void setGuia(Guia guia) { this.guia = guia; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
}
