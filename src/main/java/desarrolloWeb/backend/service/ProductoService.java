package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ProductoRequest;
import desarrolloWeb.backend.dto.ProductoResponse;
import desarrolloWeb.backend.entity.Producto;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.ProductoRepository;
import desarrolloWeb.backend.util.UnidadMedida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/* Catálogo de productos y servicios que se facturan. No se borran: se desactivan para no romper facturas antiguas. */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAllByOrderByDescripcionAsc().stream().map(ProductoResponse::de).toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest datos) {
        String codigo = datos.codigo().trim().toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo);
        }
        Producto p = new Producto();
        copiar(datos, p, codigo);
        return ProductoResponse.de(productoRepository.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest datos) {
        Producto p = obtener(id);
        String codigo = datos.codigo().trim().toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo);
        }
        String unidad = UnidadMedida.validar(datos.unidad());
        // Con stock, cambiar la unidad cambiaría el significado de lo que hay en el almacén
        if (!unidad.equals(p.getUnidad()) && p.getStock().signum() != 0) {
            throw new ReglaNegocioException("No se puede cambiar la unidad de " + p.getDescripcion() + " porque tiene stock ("
                    + InventarioService.texto(p.getStock()) + " " + p.getUnidad() + "). Primero déjalo en cero con un ajuste en Inventario.");
        }
        copiar(datos, p, codigo);
        return ProductoResponse.de(p);
    }

    @Transactional
    public ProductoResponse cambiarEstado(Long id, boolean activo) {
        Producto p = obtener(id);
        p.setActivo(activo);
        return ProductoResponse.de(p);
    }

    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id));
    }

    private static void copiar(ProductoRequest datos, Producto p, String codigo) {
        p.setCodigo(codigo);
        p.setDescripcion(datos.descripcion().trim());
        p.setUnidad(UnidadMedida.validar(datos.unidad()));
        p.setValorUnitario(datos.valorUnitario().setScale(2, RoundingMode.HALF_UP));
        // Sin dato se conserva el mínimo actual; los servicios no lo usan
        if (!p.manejaStock()) {
            p.setStockMinimo(BigDecimal.ZERO);
        } else if (datos.stockMinimo() != null) {
            p.setStockMinimo(datos.stockMinimo().setScale(3, RoundingMode.HALF_UP));
        }
    }
}
