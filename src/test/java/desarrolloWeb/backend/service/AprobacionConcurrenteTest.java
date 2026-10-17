package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.PlanillaRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/* Sin @Transactional a propósito: cada hilo abre su propia transacción, como dos usuarios reales */
@SpringBootTest
@DisplayName("Aprobar la misma planilla dos veces a la vez crea los pagos una sola vez")
class AprobacionConcurrenteTest {

    @Autowired
    private PlanillaService planillaService;
    @Autowired
    private TrabajadorRepository trabajadorRepository;
    @Autowired
    private PlanillaRepository planillaRepository;
    @Autowired
    private PagoRepository pagoRepository;

    private Long trabajadorId;
    private Long planillaId;

    @BeforeEach
    void prepararPlanillaEnBorrador() {
        Trabajador t = new Trabajador();
        t.setNombres("Ana Concurrencia");
        t.setDocumentoIdentidad("70990001");
        t.setCargo("Analista");
        t.setArea("Contabilidad");
        t.setEstado(Trabajador.ACTIVO);
        t.setFechaIngreso(LocalDate.of(2025, 1, 1));
        t.setSueldo(new BigDecimal("2000.00"));
        t.setSistemaPension("ONP");
        trabajadorId = trabajadorRepository.save(t).getId();

        YearMonth mesPasado = YearMonth.now().minusMonths(1);
        planillaId = planillaService.generar(mesPasado.getYear(), mesPasado.getMonthValue(), "rrhh1").id();
    }

    @AfterEach
    void limpiar() {
        planillaRepository.deleteById(planillaId);
        pagoRepository.deleteAll(pagoRepository.findByTrabajador_Id(trabajadorId));
        trabajadorRepository.deleteById(trabajadorId);
    }

    @Test
    void dosAprobacionesSimultaneas() throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        Callable<Boolean> aprobar = () -> {
            largada.await();
            try {
                planillaService.aprobar(planillaId, LocalDate.now(), "contabilidad1");
                return true;
            } catch (RuntimeException rechazada) {
                return false;
            }
        };

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> resultados = List.of(hilos.submit(aprobar), hilos.submit(aprobar));
            largada.countDown();
            long aprobadas = 0;
            for (Future<Boolean> r : resultados) {
                if (r.get()) {
                    aprobadas++;
                }
            }
            assertEquals(1, aprobadas, "solo una de las dos aprobaciones debe pasar");
        } finally {
            hilos.shutdown();
        }

        assertEquals(1, pagoRepository.findByTrabajador_Id(trabajadorId).size(), "el sueldo se programa una sola vez");
    }
}
