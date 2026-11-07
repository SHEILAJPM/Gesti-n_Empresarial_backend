package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.GuiaRequest;
import desarrolloWeb.backend.dto.GuiaResponse;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Guia;
import desarrolloWeb.backend.entity.GuiaItem;
import desarrolloWeb.backend.entity.ModalidadTraslado;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.GuiaRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.MotivoTraslado;
import desarrolloWeb.backend.util.UnidadMedida;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/* Guías de remisión remitente (serie T001). Sustentan el traslado de bienes: por una venta, una compra,
   entre locales de la empresa, etc. Si vienen de una venta, el destinatario y los bienes se toman de ella. */
@Service
public class GuiaService {

    static final String SERIE = "T001";

    private final GuiaRepository guiaRepository;
    private final VentaRepository ventaRepository;
    private final FacturacionService facturacionService;
    private final Clock reloj;

    @Autowired
    public GuiaService(GuiaRepository guiaRepository, VentaRepository ventaRepository,
                       FacturacionService facturacionService) {
        this(guiaRepository, ventaRepository, facturacionService, Clock.system(PortalService.ZONA));
    }

    GuiaService(GuiaRepository guiaRepository, VentaRepository ventaRepository, FacturacionService facturacionService,
                Clock reloj) {
        this.guiaRepository = guiaRepository;
        this.ventaRepository = ventaRepository;
        this.facturacionService = facturacionService;
        this.reloj = reloj;
    }

    @Transactional
    public GuiaResponse emitir(GuiaRequest datos, String usuario) {
        String motivo = MotivoTraslado.validar(datos.motivoCodigo());
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate traslado = datos.fechaTraslado() != null ? datos.fechaTraslado() : hoy;
        if (traslado.isBefore(hoy)) {
            throw new IllegalArgumentException("El traslado no puede empezar antes de la emisión de la guía (hoy)");
        }

        Venta venta = null;
        if (datos.ventaId() != null) {
            venta = ventaRepository.findById(datos.ventaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta " + datos.ventaId()));
            if (venta.getEstado() != EstadoVenta.EMITIDA) {
                throw new ReglaNegocioException("La venta " + venta.getNumeroComprobante() + " está anulada");
            }
        }

        Guia guia = new Guia();
        guia.setNumero(siguienteNumero());
        guia.setFechaEmision(hoy);
        guia.setFechaTraslado(traslado);
        guia.setMotivoCodigo(motivo);
        guia.setVenta(venta);
        asignarDestinatario(guia, datos, venta);
        guia.setPesoBrutoKg(datos.pesoBrutoKg().setScale(3, RoundingMode.HALF_UP));
        guia.setNumeroBultos(datos.numeroBultos());
        asignarTransporte(guia, datos);
        guia.setPartidaUbigeo(datos.partidaUbigeo());
        guia.setPartidaDireccion(datos.partidaDireccion().trim());
        guia.setLlegadaUbigeo(datos.llegadaUbigeo());
        guia.setLlegadaDireccion(datos.llegadaDireccion().trim());
        guia.setObservaciones(vacioANulo(datos.observaciones()));
        guia.setRegistradaPor(usuario);
        construirItems(datos.items(), venta).forEach(guia::agregarItem);

        Guia guardada = guiaRepository.save(guia);
        facturacionService.emitir(guardada);
        return toResponse(guardada, true);
    }

    @Transactional(readOnly = true)
    public List<GuiaResponse> listar() {
        return guiaRepository.listarConVenta().stream().map(g -> toResponse(g, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<GuiaResponse> listarPorVenta(Long ventaId) {
        return guiaRepository.findByVentaIdOrderByIdAsc(ventaId).stream().map(g -> toResponse(g, false)).toList();
    }

    @Transactional(readOnly = true)
    public GuiaResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id), true);
    }

    @Transactional
    public GuiaResponse consultarSunat(Long id) {
        Guia g = obtenerEntidad(id);
        facturacionService.consultar(g);
        return toResponse(g, true);
    }

    @Transactional
    public GuiaResponse reenviarSunat(Long id) {
        Guia g = obtenerEntidad(id);
        facturacionService.emitir(g);
        return toResponse(g, true);
    }

    private Guia obtenerEntidad(Long id) {
        return guiaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la guía " + id));
    }

    private String siguienteNumero() {
        Long maximo = guiaRepository.maximoCorrelativo(SERIE + "-", SERIE.length() + 2);
        int local = (maximo == null ? 0 : Math.toIntExact(maximo)) + 1;
        return SERIE + "-" + facturacionService.siguienteCorrelativo("09", SERIE, local);
    }

    /* El destinatario se escribe o, si la guía viene de una venta, es el cliente de esa venta */
    private static void asignarDestinatario(Guia guia, GuiaRequest datos, Venta venta) {
        String documento = vacioANulo(datos.destinatarioDocumento());
        String nombre = vacioANulo(datos.destinatarioNombre());
        if (documento == null && venta != null) {
            documento = venta.getCliente().getDocumento();
            nombre = venta.getCliente().getNombre();
        }
        if (documento == null || nombre == null) {
            throw new IllegalArgumentException("Indica el documento y el nombre del destinatario");
        }
        guia.setDestinatarioDocumento(documento);
        guia.setDestinatarioNombre(nombre);
        guia.setDestinatarioTipoDocumento(documento.length() == 11 ? "6" : "1");
    }

    private static void asignarTransporte(Guia guia, GuiaRequest datos) {
        guia.setModalidad(datos.modalidad());
        if (datos.modalidad() == ModalidadTraslado.PUBLICO) {
            String ruc = vacioANulo(datos.transportistaRuc());
            String razon = vacioANulo(datos.transportistaRazonSocial());
            if (ruc == null || razon == null) {
                throw new IllegalArgumentException("Transporte por empresa: indica el RUC y la razón social del transportista");
            }
            guia.setTransportistaRuc(ruc);
            guia.setTransportistaRazonSocial(razon);
        } else {
            String placa = vacioANulo(datos.vehiculoPlaca());
            String dni = vacioANulo(datos.conductorDni());
            String nombres = vacioANulo(datos.conductorNombres());
            String licencia = vacioANulo(datos.conductorLicencia());
            if (placa == null || dni == null || nombres == null || licencia == null) {
                throw new IllegalArgumentException("Vehículo propio: indica la placa y el DNI, nombre y licencia del conductor");
            }
            guia.setVehiculoPlaca(placa.toUpperCase());
            guia.setConductorDni(dni);
            guia.setConductorNombres(nombres);
            guia.setConductorLicencia(licencia.toUpperCase());
        }
    }

    /* Los bienes que se envían; si no llegan y hay venta, se copian sus líneas (sin los servicios: no se trasladan) */
    private static List<GuiaItem> construirItems(List<GuiaRequest.Item> items, Venta venta) {
        List<GuiaItem> lista = new ArrayList<>();
        if (items != null && !items.isEmpty()) {
            for (GuiaRequest.Item i : items) {
                lista.add(item(vacioANulo(i.codigo()), i.descripcion().trim(), UnidadMedida.validar(i.unidad()), i.cantidad()));
            }
        } else if (venta != null) {
            for (VentaItem i : venta.getItems()) {
                if (!"ZZ".equals(i.getUnidad()) && !"HUR".equals(i.getUnidad())) {
                    lista.add(item(i.getCodigo(), i.getDescripcion(), i.getUnidad(), i.getCantidad()));
                }
            }
        }
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un bien a trasladar");
        }
        return lista;
    }

    private static GuiaItem item(String codigo, String descripcion, String unidad, BigDecimal cantidad) {
        GuiaItem g = new GuiaItem();
        g.setCodigo(codigo);
        g.setDescripcion(descripcion);
        g.setUnidad(unidad);
        g.setCantidad(cantidad.setScale(3, RoundingMode.HALF_UP));
        return g;
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private GuiaResponse toResponse(Guia g, boolean completo) {
        List<GuiaResponse.Item> items = !completo ? List.of() : g.getItems().stream()
                .map(i -> new GuiaResponse.Item(i.getOrden(), i.getCodigo(), i.getDescripcion(), i.getUnidad(), i.getCantidad()))
                .toList();
        return new GuiaResponse(g.getId(), g.getNumero(), g.getFechaEmision(), g.getFechaTraslado(), g.getMotivoCodigo(),
                MotivoTraslado.CATALOGO.get(g.getMotivoCodigo()), g.getVenta() == null ? null : g.getVenta().getId(),
                g.getVenta() == null ? null : g.getVenta().getNumeroComprobante(), g.getDestinatarioTipoDocumento(),
                g.getDestinatarioDocumento(), g.getDestinatarioNombre(), g.getPesoBrutoKg(), g.getNumeroBultos(),
                g.getModalidad(), g.getTransportistaRuc(), g.getTransportistaRazonSocial(), g.getVehiculoPlaca(),
                g.getConductorDni(), g.getConductorNombres(), g.getConductorLicencia(), g.getPartidaUbigeo(),
                g.getPartidaDireccion(), g.getLlegadaUbigeo(), g.getLlegadaDireccion(), g.getObservaciones(),
                g.getRegistradaPor(), facturacionService.respuesta(g), completo ? items.size() : 0, items);
    }
}
