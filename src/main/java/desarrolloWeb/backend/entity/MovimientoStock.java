package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Una línea del kárdex: cuánto entró o salió, por qué y con qué saldo quedó el producto */
@Entity
@Table(name = "movimientos_stock")
public class MovimientoStock {

    public enum Tipo { ENTRADA, SALIDA }

    /* manual = se registra a mano en Inventario; los demás los generan las ventas y las notas de crédito */
    public enum Motivo {
        SALDO_INICIAL("Inventario inicial", Tipo.ENTRADA, true),
        COMPRA("Ingreso por compra", Tipo.ENTRADA, true),
        DEVOLUCION_CLIENTE("Devolución del cliente", Tipo.ENTRADA, false),
        ANULACION_VENTA("Anulación de venta", Tipo.ENTRADA, false),
        VENTA("Venta", Tipo.SALIDA, false),
        DEVOLUCION_PROVEEDOR("Devolución al proveedor", Tipo.SALIDA, true),
        MERMA("Merma o deterioro", Tipo.SALIDA, true),
        CONSUMO_INTERNO("Consumo interno", Tipo.SALIDA, true),
        // Entrada o salida según el conteo físico
        AJUSTE("Ajuste por conteo físico", null, false),
        OTRO("Otro", null, true);

        private final String texto;
        private final Tipo tipo;
        private final boolean manual;

        Motivo(String texto, Tipo tipo, boolean manual) {
            this.texto = texto;
            this.tipo = tipo;
            this.manual = manual;
        }

        public String getTexto() { return texto; }

        public boolean admite(Tipo t) { return tipo == null || tipo == t; }

        public boolean isManual() { return manual; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Motivo motivo;

    // Siempre positiva: el tipo dice si suma o resta
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    // Stock del producto después de este movimiento
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal saldo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    // Documento que lo origina: F001-12, FC01-3, la guía del proveedor...
    @Column(length = 80)
    private String referencia;

    @Column(length = 200)
    private String observacion;

    @Column(name = "registrado_por", length = 50)
    private String registradoPor;

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Motivo getMotivo() { return motivo; }
    public void setMotivo(Motivo motivo) { this.motivo = motivo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
}
