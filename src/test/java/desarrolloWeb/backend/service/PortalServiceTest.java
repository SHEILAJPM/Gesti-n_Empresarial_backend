package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.MarcarUbicacionRequest;
import desarrolloWeb.backend.entity.FotoAsistencia;
import desarrolloWeb.backend.entity.IntentoMarcacion;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.entity.Sede;
import desarrolloWeb.backend.entity.TipoMarcacion;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.entity.Usuario;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.FotoAsistenciaRepository;
import desarrolloWeb.backend.repository.IntentoMarcacionRepository;
import desarrolloWeb.backend.repository.MarcacionRepository;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.PlanillaDetalleRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import desarrolloWeb.backend.repository.UsuarioRepository;
import desarrolloWeb.backend.util.Distancia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.transaction.support.TransactionOperations;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portal del trabajador: asistencia por GPS")
class PortalServiceTest {

    // Plaza de Armas de Lima; 0.000072° de latitud son unos 8 m y 0.000405° unos 45 m
    private static final double LAT = -12.0464;
    private static final double LON = -77.0428;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 8, 2, 30);
    private static final String FOTO = jpeg(640, 480, new Color(170, 150, 130));

    @Mock private FotoAsistenciaRepository fotoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TrabajadorRepository trabajadorRepository;
    @Mock private MarcacionRepository marcacionRepository;
    @Mock private IntentoMarcacionRepository intentoRepository;
    @Mock private PlanillaDetalleRepository detalleRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private SedeService sedeService;

    private PortalService portal;
    private Trabajador trabajador;
    private final List<Marcacion> guardadas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(ZonedDateTime.of(AHORA, PortalService.ZONA).toInstant(), PortalService.ZONA);
        portal = new PortalService(usuarioRepository, trabajadorRepository, marcacionRepository, intentoRepository,
                detalleRepository, pagoRepository, fotoRepository, sedeService, TransactionOperations.withoutTransaction(), reloj);

        Usuario usuario = new Usuario("vendedor1", "x", Rol.VENDEDOR);
        usuario.setTrabajadorId(7L);
        trabajador = new Trabajador(7L, "Rosa Díaz", "70000007", "Vendedora", "Ventas", Trabajador.ACTIVO);

        Sede sede = new Sede();
        sede.setNombre("Sede central");
        sede.setLatitud(BigDecimal.valueOf(LAT));
        sede.setLongitud(BigDecimal.valueOf(LON));
        sede.setRadioMetros(12);
        sede.setPrecisionMaxima(25);

        lenient().when(usuarioRepository.findByUsername("vendedor1")).thenReturn(Optional.of(usuario));
        lenient().when(trabajadorRepository.findById(7L)).thenReturn(Optional.of(trabajador));
        lenient().when(sedeService.activa()).thenReturn(Optional.of(sede));
        lenient().when(marcacionRepository.findByTrabajadorIdAndFechaHoraBetweenOrderByFechaHoraAsc(eq(7L), any(), any()))
                .thenAnswer(inv -> {
                    LocalDateTime desde = inv.getArgument(1);
                    LocalDateTime hasta = inv.getArgument(2);
                    return guardadas.stream()
                            .filter(m -> !m.getFechaHora().isBefore(desde) && !m.getFechaHora().isAfter(hasta))
                            .toList();
                });
        lenient().when(marcacionRepository.save(any(Marcacion.class))).thenAnswer(inv -> {
            Marcacion m = inv.getArgument(0);
            m.setId((long) guardadas.size() + 1);
            guardadas.add(m);
            return m;
        });
        lenient().when(fotoRepository.save(any(FotoAsistencia.class))).thenAnswer(inv -> {
            FotoAsistencia f = inv.getArgument(0);
            f.setId(99L);
            return f;
        });
    }

    private static String jpeg(int ancho, int alto, Color color) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, ancho, alto);
        g.dispose();
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "jpg", salida);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void laMarcacionGuardaLaFotoTomadaEnEseMomento() {
        Marcacion m = portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO));

        ArgumentCaptor<FotoAsistencia> foto = ArgumentCaptor.forClass(FotoAsistencia.class);
        verify(fotoRepository).save(foto.capture());
        assertEquals(99L, m.getFotoId());
        assertEquals(7L, foto.getValue().getTrabajadorId());
        assertEquals(AHORA, foto.getValue().getFechaHora());
        assertEquals("image/jpeg", foto.getValue().getTipoMime());
    }

    @Test
    void sinFotoNoSeMarcaNiQuedaIntento() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, " ")));

        assertTrue(error.getMessage().contains("foto"), error.getMessage());
        verify(marcacionRepository, never()).save(any());
        verify(intentoRepository, never()).save(any());
    }

    @Test
    void fotoQueNoEsImagenSeRechaza() {
        String texto = Base64.getEncoder().encodeToString("no soy una foto".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, texto)));
        verify(fotoRepository, never()).save(any());
    }

    @Test
    void fotoConLaCamaraTapadaSeRechaza() {
        String negra = jpeg(640, 480, Color.BLACK);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, negra)));
        assertTrue(error.getMessage().contains("oscura"), error.getMessage());
    }

    @Test
    void fotoMuyPequenaSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, jpeg(120, 90, Color.GRAY))));
    }

    @Test
    void elIntentoFueraDelRadioConservaLaFotoComoEvidencia() {
        assertThrows(ReglaNegocioException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT + 0.000405, LON, 5.0, FOTO)));

        ArgumentCaptor<IntentoMarcacion> intento = ArgumentCaptor.forClass(IntentoMarcacion.class);
        verify(intentoRepository).save(intento.capture());
        assertEquals(99L, intento.getValue().getFotoId());
    }

    @Test
    void elTrabajadorSoloVeLaFotoDeSusPropiasMarcaciones() {
        Marcacion ajena = new Marcacion();
        ajena.setTrabajadorId(8L);
        ajena.setFotoId(50L);
        when(marcacionRepository.findById(31L)).thenReturn(Optional.of(ajena));

        assertThrows(RecursoNoEncontradoException.class, () -> portal.miFoto("vendedor1", 31L));
        verify(fotoRepository, never()).findById(any());
    }

    @Test
    void haversineCalculaMetrosReales() {
        double ocho = Distancia.metros(LAT, LON, LAT + 0.000072, LON);
        assertTrue(ocho > 7.5 && ocho < 8.5, "≈ 8 m, fue " + ocho);
    }

    @Test
    void dentroDelRadioRegistraEntradaConLaHoraDelServidor() {
        Marcacion m = portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT + 0.000072, LON, 6.0, FOTO));

        assertEquals(TipoMarcacion.ENTRADA, m.getTipo());
        assertEquals(AHORA, m.getFechaHora());
        assertEquals("PORTAL", m.getOrigen());
        assertTrue(m.getDistanciaMetros().doubleValue() < 12);
    }

    @Test
    void lasMarcacionesDelDiaAlternanEntradaYSalida() {
        portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO));
        guardadas.get(0).setFechaHora(AHORA.minusHours(4));
        Marcacion segunda = portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO));

        assertEquals(TipoMarcacion.SALIDA, segunda.getTipo());
    }

    @Test
    void unDobleToqueNoRegistraDosMarcaciones() {
        portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO)));

        assertTrue(error.getMessage().contains("Espera un minuto"), error.getMessage());
        assertEquals(1, guardadas.size());
        verify(trabajadorRepository, org.mockito.Mockito.times(2)).bloquear(7L);
    }

    @Test
    void fueraDelRadioSeRechazaYQuedaElIntento() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT + 0.000405, LON, 5.0, FOTO)));

        assertTrue(error.getMessage().contains("máximo 12 m"), error.getMessage());
        ArgumentCaptor<IntentoMarcacion> intento = ArgumentCaptor.forClass(IntentoMarcacion.class);
        verify(intentoRepository).save(intento.capture());
        assertTrue(intento.getValue().getDistanciaMetros().doubleValue() > 40);
        verify(marcacionRepository, never()).save(any());
    }

    @Test
    void gpsImprecisoSeRechazaAunqueElPuntoCaigaDentro() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 80.0, FOTO)));

        assertTrue(error.getMessage().contains("±80"), error.getMessage());
        verify(intentoRepository).save(any(IntentoMarcacion.class));
    }

    @Test
    void trabajadorDadoDeBajaNoPuedeMarcar() {
        trabajador.setEstado(Trabajador.CESADO);

        assertThrows(ReglaNegocioException.class, () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO)));
    }

    @Test
    void sinSedeConfiguradaNoSePuedeMarcar() {
        when(sedeService.activa()).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> portal.marcar("vendedor1", new MarcarUbicacionRequest(LAT, LON, 5.0, FOTO)));
        assertTrue(error.getMessage().contains("ubicación"));
    }

    @Test
    void cuentaSinFichaNoTienePortal() {
        when(usuarioRepository.findByUsername("contabilidad1"))
                .thenReturn(Optional.of(new Usuario("contabilidad1", "x", Rol.CONTABILIDAD)));

        assertThrows(ReglaNegocioException.class, () -> portal.perfil("contabilidad1"));
    }
}
