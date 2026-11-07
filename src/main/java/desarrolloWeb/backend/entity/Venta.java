package desarrolloWeb.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas", indexes = @Index(name = "idx_ventas_fecha", columnList = "fecha"))
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 7)
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", nullable = false, unique = true, length = 20)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private EstadoVenta estado;

    @Embedded
    private ComprobanteSunat sunat = new ComprobanteSunat();

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 7)
    private FormaPago formaPago = FormaPago.CONTADO;

    @Column(length = 250)
    private String observaciones;

    // Detalle del comprobante; las ventas registradas antes de la V9 no tienen líneas
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<VentaItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<VentaCuota> cuotas = new ArrayList<>();

    public Venta() {
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<VentaItem> getItems() {
        return items;
    }

    public List<VentaCuota> getCuotas() {
        return cuotas;
    }

    public void agregarItem(VentaItem item) {
        item.setVenta(this);
        item.setOrden(items.size() + 1);
        items.add(item);
    }

    public void agregarCuota(VentaCuota cuota) {
        cuota.setVenta(this);
        cuota.setNumero(cuotas.size() + 1);
        cuotas.add(cuota);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
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

    public EstadoVenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    public ComprobanteSunat getSunat() {
        if (sunat == null) {
            sunat = new ComprobanteSunat();
        }
        return sunat;
    }

    public void setSunat(ComprobanteSunat sunat) {
        this.sunat = sunat;
    }
}
