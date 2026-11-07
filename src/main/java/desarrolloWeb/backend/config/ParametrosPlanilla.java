package desarrolloWeb.backend.config;

import desarrolloWeb.backend.util.CalculadoraPlanilla;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/* Valores legales que cambian con el tiempo (planilla.* en application.properties):
   UIT (cada año), remuneración mínima vital y remuneración máxima asegurable de la SBS (cada trimestre). */
@Component
public class ParametrosPlanilla {

    private final CalculadoraPlanilla.Parametros parametros;

    public ParametrosPlanilla() {
        this.parametros = CalculadoraPlanilla.Parametros.REFERENCIALES;
    }

    @Autowired
    public ParametrosPlanilla(@Value("${planilla.uit:5500.00}") BigDecimal uit,
                              @Value("${planilla.rmv:1130.00}") BigDecimal rmv,
                              @Value("${planilla.rma:12234.34}") BigDecimal rma) {
        this.parametros = new CalculadoraPlanilla.Parametros(uit, rmv, rma);
    }

    public CalculadoraPlanilla.Parametros vigentes() {
        return parametros;
    }
}
