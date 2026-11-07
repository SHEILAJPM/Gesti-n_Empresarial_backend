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

/* Guía de remisión electrónica remitente (09). Puede venir de una venta o ser un traslado independiente. */
@Entity
@Table(name = "guias")
public class Guia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_traslado", nullable = false)
    private LocalDate fechaTraslado;

    // Catálogo 20 de SUNAT
    @Column(name = "motivo_codigo", nullable = false, length = 2)
    private String motivoCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    // Catálogo 06: 6 RUC, 1 DNI
    @Column(name = "destinatario_tipo_documento", nullable = false, length = 3)
    private String destinatarioTipoDocumento;

    @Column(name = "destinatario_documento", nullable = false, length = 15)
    private String destinatarioDocumento;

    @Column(name = "destinatario_nombre", nullable = false, length = 150)
    private String destinatarioNombre;

    @Column(name = "peso_bruto_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal pesoBrutoKg;

    @Column(name = "numero_bultos")
    private Integer numeroBultos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private ModalidadTraslado modalidad;

    @Column(name = "transportista_ruc", length = 11)
    private String transportistaRuc;

    @Column(name = "transportista_razon_social", length = 150)
    private String transportistaRazonSocial;

    @Column(name = "vehiculo_placa", length = 10)
    private String vehiculoPlaca;

    @Column(name = "conductor_dni", length = 8)
    private String conductorDni;

    @Column(name = "conductor_nombres", length = 150)
    private String conductorNombres;

    @Column(name = "conductor_licencia", length = 15)
    private String conductorLicencia;

    @Column(name = "partida_ubigeo", nullable = false, length = 6)
    private String partidaUbigeo;

    @Column(name = "partida_direccion", nullable = false, length = 200)
    private String partidaDireccion;

    @Column(name = "llegada_ubigeo", nullable = false, length = 6)
    private String llegadaUbigeo;

    @Column(name = "llegada_direccion", nullable = false, length = 200)
    private String llegadaDireccion;

    @Column(length = 250)
    private String observaciones;

    @Column(name = "registrada_por", length = 50)
    private String registradaPor;

    @Embedded
    private ComprobanteSunat sunat = new ComprobanteSunat();

    @OneToMany(mappedBy = "guia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<GuiaItem> items = new ArrayList<>();

    public void agregarItem(GuiaItem item) {
        item.setGuia(this);
        item.setOrden(items.size() + 1);
        items.add(item);
    }

    public ComprobanteSunat getSunat() {
        if (sunat == null) {
            sunat = new ComprobanteSunat();
        }
        return sunat;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    public LocalDate getFechaTraslado() { return fechaTraslado; }
    public void setFechaTraslado(LocalDate fechaTraslado) { this.fechaTraslado = fechaTraslado; }
    public String getMotivoCodigo() { return motivoCodigo; }
    public void setMotivoCodigo(String motivoCodigo) { this.motivoCodigo = motivoCodigo; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public String getDestinatarioTipoDocumento() { return destinatarioTipoDocumento; }
    public void setDestinatarioTipoDocumento(String v) { this.destinatarioTipoDocumento = v; }
    public String getDestinatarioDocumento() { return destinatarioDocumento; }
    public void setDestinatarioDocumento(String v) { this.destinatarioDocumento = v; }
    public String getDestinatarioNombre() { return destinatarioNombre; }
    public void setDestinatarioNombre(String v) { this.destinatarioNombre = v; }
    public BigDecimal getPesoBrutoKg() { return pesoBrutoKg; }
    public void setPesoBrutoKg(BigDecimal pesoBrutoKg) { this.pesoBrutoKg = pesoBrutoKg; }
    public Integer getNumeroBultos() { return numeroBultos; }
    public void setNumeroBultos(Integer numeroBultos) { this.numeroBultos = numeroBultos; }
    public ModalidadTraslado getModalidad() { return modalidad; }
    public void setModalidad(ModalidadTraslado modalidad) { this.modalidad = modalidad; }
    public String getTransportistaRuc() { return transportistaRuc; }
    public void setTransportistaRuc(String v) { this.transportistaRuc = v; }
    public String getTransportistaRazonSocial() { return transportistaRazonSocial; }
    public void setTransportistaRazonSocial(String v) { this.transportistaRazonSocial = v; }
    public String getVehiculoPlaca() { return vehiculoPlaca; }
    public void setVehiculoPlaca(String v) { this.vehiculoPlaca = v; }
    public String getConductorDni() { return conductorDni; }
    public void setConductorDni(String v) { this.conductorDni = v; }
    public String getConductorNombres() { return conductorNombres; }
    public void setConductorNombres(String v) { this.conductorNombres = v; }
    public String getConductorLicencia() { return conductorLicencia; }
    public void setConductorLicencia(String v) { this.conductorLicencia = v; }
    public String getPartidaUbigeo() { return partidaUbigeo; }
    public void setPartidaUbigeo(String v) { this.partidaUbigeo = v; }
    public String getPartidaDireccion() { return partidaDireccion; }
    public void setPartidaDireccion(String v) { this.partidaDireccion = v; }
    public String getLlegadaUbigeo() { return llegadaUbigeo; }
    public void setLlegadaUbigeo(String v) { this.llegadaUbigeo = v; }
    public String getLlegadaDireccion() { return llegadaDireccion; }
    public void setLlegadaDireccion(String v) { this.llegadaDireccion = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getRegistradaPor() { return registradaPor; }
    public void setRegistradaPor(String registradaPor) { this.registradaPor = registradaPor; }
    public List<GuiaItem> getItems() { return items; }
}
