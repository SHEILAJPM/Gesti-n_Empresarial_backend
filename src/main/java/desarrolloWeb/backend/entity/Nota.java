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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/* Nota de crédito o débito sobre una venta (factura o boleta) */
@Entity
@Table(name = "notas")
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private TipoNota tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDate fecha;

    // Catálogo 09 (crédito) o 10 (débito)
    @Column(name = "motivo_codigo", nullable = false, length = 2)
    private String motivoCodigo;

    @Column(nullable = false, length = 250)
    private String sustento;

    @Column(name = "monto_sin_igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSinIgv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "tasa_aplicada", nullable = false, precision = 5, scale = 4)
    private BigDecimal tasaAplicada;

    @Column(name = "registrada_por", length = 50)
    private String registradaPor;

    @Embedded
    private ComprobanteSunat sunat = new ComprobanteSunat();

    @OneToMany(mappedBy = "nota", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<NotaItem> items = new ArrayList<>();

    public void agregarItem(NotaItem item) {
        item.setNota(this);
        item.setOrden(items.size() + 1);
        items.add(item);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoNota getTipo() { return tipo; }
    public void setTipo(TipoNota tipo) { this.tipo = tipo; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMotivoCodigo() { return motivoCodigo; }
    public void setMotivoCodigo(String motivoCodigo) { this.motivoCodigo = motivoCodigo; }
    public String getSustento() { return sustento; }
    public void setSustento(String sustento) { this.sustento = sustento; }
    public BigDecimal getMontoSinIgv() { return montoSinIgv; }
    public void setMontoSinIgv(BigDecimal montoSinIgv) { this.montoSinIgv = montoSinIgv; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public BigDecimal getTasaAplicada() { return tasaAplicada; }
    public void setTasaAplicada(BigDecimal tasaAplicada) { this.tasaAplicada = tasaAplicada; }
    public String getRegistradaPor() { return registradaPor; }
    public void setRegistradaPor(String registradaPor) { this.registradaPor = registradaPor; }
    public List<NotaItem> getItems() { return items; }

    public ComprobanteSunat getSunat() {
        if (sunat == null) {
            sunat = new ComprobanteSunat();
        }
        return sunat;
    }
}
