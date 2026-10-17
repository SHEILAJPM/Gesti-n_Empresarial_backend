# Gestión Empresarial · Backend

API REST de un sistema de gestión para empresas peruanas: personal y asistencia, planillas, ventas con facturación electrónica, compras, pagos y tributos (IGV).

Frontend (Angular): [Gesti-n_Empresarial_Frontend](https://github.com/SHEILAJPM/Gesti-n_Empresarial_Frontend)

## Tecnologías

- Java 21 y Spring Boot 4 (Web MVC, Data JPA, Security, Validation)
- PostgreSQL en [Neon](https://neon.tech), con el esquema versionado por Flyway
- H2 para trabajar sin internet y para los tests
- Facturación electrónica con [APISUNAT](https://apisunat.com) o un simulador local de SUNAT
- Consulta de DNI (RENIEC) y RUC (SUNAT) con [Decolecta](https://decolecta.com)

## Módulos

| Módulo | Qué hace |
|---|---|
| Personal | Ficha del trabajador, cese y reactivación, asistencia y documentos con vencimiento |
| Portal del trabajador | Cada trabajador marca su entrada y salida por GPS dentro del radio de la sede y ve sus boletas |
| Planillas | Cálculo mensual de ONP, AFP y EsSalud. RRHH la genera y Contabilidad la aprueba, lo que crea los pagos |
| Ventas y clientes | Facturas (RUC) y boletas (DNI) con IGV, anulación y envío del comprobante electrónico |
| Compras y proveedores | Facturas de compra para el crédito fiscal |
| Tributario | IGV configurable, resumen mensual (débito, crédito e IGV por pagar) y simulador |
| Pagos | Pagos programados a trabajadores y proveedores |
| Panel | Métricas generales del negocio |
| Consultas | Autocompleta el nombre por DNI y la razón social y dirección por RUC |

## Requisitos

- JDK 21
- Maven no hace falta: el proyecto trae el wrapper (`mvnw` / `mvnw.cmd`)

## Puesta en marcha

### Opción 1: con la base de datos de Neon

1. Copia `.env.example` como `.env` y completa `DATABASE_URL` y `DATABASE_URL_UNPOOLED` con los datos de tu proyecto en Neon. Si tienes la CLI de Neon, puedes generarlo con `neon env pull --file .env -s postgres`.
2. Arranca la aplicación:

   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```

Flyway crea o actualiza las tablas al arrancar.

### Opción 2: sin internet (perfil `local`)

Usa una base H2 guardada en `data/` con el mismo esquema y datos de ejemplo (clientes, proveedores, compras y ventas):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

La consola de H2 queda en <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:file:./data/desarrollo_web_local`, usuario `sa`, sin clave).

La API queda en <http://localhost:8080>.

## Variables de entorno (`.env`)

`.env` nunca se sube al repositorio. Solo la base de datos es obligatoria.

| Variable | Para qué sirve |
|---|---|
| `DATABASE_URL` | Conexión de la aplicación (pooled) |
| `DATABASE_URL_UNPOOLED` | Conexión directa para las migraciones de Flyway |
| `APISUNAT_PERSONA_ID`, `APISUNAT_PERSONA_TOKEN` | Credenciales de APISUNAT. Sin ellas se usa el simulador local de SUNAT |
| `EMISOR_RUC`, `EMISOR_RAZON_SOCIAL` | Empresa que emite los comprobantes |
| `DECOLECTA_TOKEN` | Token para consultar DNI y RUC. Se crea gratis en <https://decolecta.com/profile>. Sin él, el resto del sistema funciona y la consulta responde 503 |

## Autenticación y roles

La API usa HTTP Basic: cada petición envía `Authorization: Basic <usuario:clave en Base64>`. `GET /api/auth/me` devuelve el usuario, su rol y, si lo tiene, el trabajador vinculado.

Al arrancar por primera vez se crean estos usuarios (solo si no existen):

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123` | ADMIN |
| `rrhh1` | `Rrhh123` | RRHH |
| `contabilidad1` | `Conta123` | CONTABILIDAD |
| `administracion1` | `AdminArea123` | ADMINISTRACION |

> Son contraseñas de desarrollo. Cámbialas antes de usar el sistema con datos reales.

Los roles `TRABAJADOR`, `VENDEDOR` y `ALMACEN` son del personal: cada cuenta va vinculada a la ficha de un trabajador y solo usa el portal (`/api/mi/**`).

| Recurso | Consultar | Modificar |
|---|---|---|
| Personal, asistencia y documentos | ADMIN, RRHH (la lista de personal también CONTABILIDAD) | ADMIN, RRHH |
| Clientes y ventas | ADMIN, CONTABILIDAD, ADMINISTRACION | ADMIN, ADMINISTRACION (anular venta: ADMIN, CONTABILIDAD) |
| Proveedores y compras | ADMIN, ADMINISTRACION, CONTABILIDAD | ADMIN, ADMINISTRACION, CONTABILIDAD |
| Planillas | ADMIN, RRHH, CONTABILIDAD | Generar: ADMIN, RRHH · Aprobar: ADMIN, CONTABILIDAD |
| Pagos | ADMIN, CONTABILIDAD | ADMIN, CONTABILIDAD |
| Tributario | ADMIN, CONTABILIDAD | Cambiar la tasa del IGV: ADMIN |
| Sede (ubicación para marcar) | ADMIN, RRHH | ADMIN |
| Consulta de DNI/RUC | ADMIN, RRHH, CONTABILIDAD, ADMINISTRACION | — |
| Portal del trabajador | Cualquier usuario autenticado con ficha vinculada | — |
| Usuarios, panel y lo demás | ADMIN | ADMIN |

## Endpoints

Todos devuelven JSON. Los errores tienen siempre la misma forma: `{ timestamp, status, error, mensaje, path, errores }`.

| Recurso | Endpoints |
|---|---|
| Sesión | `GET /api/auth/me` |
| Personal | `GET/POST /api/personal`, `GET/PUT/DELETE /api/personal/{id}`, `GET /api/personal/area/{area}`, `PATCH /api/personal/{id}/cesar`, `PATCH /api/personal/{id}/reactivar` |
| Asistencia | `GET /api/asistencia`, `POST /api/asistencia/marcar`, `GET /api/asistencia/trabajador/{id}`, `GET /api/asistencia/intentos` |
| Documentos | `GET/POST /api/documentos`, `GET /api/documentos/trabajador/{id}`, `GET /api/documentos/proximos-a-vencer`, `GET /api/documentos/vencidos` |
| Portal | `GET /api/mi/perfil`, `GET/POST /api/mi/asistencia`, `GET /api/mi/boletas` |
| Sede | `GET/PUT /api/sede` |
| Planillas | `GET /api/planillas`, `GET/DELETE /api/planillas/{id}`, `POST /api/planillas/generar`, `PATCH /api/planillas/{id}/aprobar` |
| Pagos | `GET/POST /api/pagos`, `GET /api/pagos/pendientes`, `PUT /api/pagos/{id}/pagar`, `GET /api/pagos/trabajador/{id}`, `GET /api/pagos/proveedor/{id}` |
| Clientes | `GET/POST /api/clientes`, `GET/PUT/DELETE /api/clientes/{id}`, `GET /api/clientes/buscar`, `GET /api/clientes/documento/{documento}` |
| Ventas | `GET/POST /api/ventas`, `GET /api/ventas/{id}`, `PATCH /api/ventas/{id}/anular`, `GET /api/ventas/cliente/{id}`, `GET /api/ventas/periodo`, `GET /api/ventas/igv-periodo`, `GET /api/ventas/siguiente-numero`, `GET /api/ventas/reportes/por-comprobante`, `GET /api/ventas/reportes/ticket-promedio` |
| Facturación electrónica | `GET /api/ventas/{id}/sunat`, `POST /api/ventas/{id}/sunat/reenviar`, `GET /api/facturacion/estado` |
| Proveedores | `GET/POST /api/proveedores`, `GET/PUT/DELETE /api/proveedores/{id}` |
| Compras | `GET/POST /api/compras`, `GET/PUT/DELETE /api/compras/{id}`, `GET /api/compras/proveedor/{id}`, `GET /api/compras/periodo`, `GET /api/compras/igv-periodo` |
| Tributario | `GET /api/tributario/resumen`, `GET /api/tributario/resumen-mensual`, `GET /api/tributario/calcular`, `GET /api/tributario/configuracion`, `PUT /api/tributario/configuracion/{codigo}` |
| Consultas | `GET /api/consultas/documento/{dni o ruc}` |
| Usuarios | `GET/POST /api/usuarios`, `PATCH /api/usuarios/{id}/estado`, `PATCH /api/usuarios/{id}/trabajador` |
| Panel | `GET /api/dashboard` |

Ejemplo:

```bash
curl -u admin:Admin123 http://localhost:8080/api/consultas/documento/20601030013
```

En la raíz hay archivos JSON de ejemplo (`trabajador.json`, `asistencia.json`, `documento.json`) para probar las peticiones `POST`.

## Tests

```bash
./mvnw test
```

Los tests usan H2 en memoria: no necesitan Neon, ni internet, ni llamar a APISUNAT o Decolecta.

## Estructura

```
src/main/java/desarrolloWeb/backend/
├── config/       Seguridad, inicializadores, facturación y conexión a Neon
├── controller/   Endpoints REST
├── dto/          Datos que entran y salen de la API
├── entity/       Entidades JPA
├── exception/    Errores del negocio y manejador global
├── repository/   Repositorios de Spring Data
├── security/     Respuestas JSON para 401 y 403
├── service/      Lógica del negocio
└── util/         Cálculos de planilla e IGV, validación de documentos, XML UBL
src/main/resources/db/
├── migration/      Esquema (Flyway V1 a V5)
└── datos-ejemplo/  Datos del perfil local
```
