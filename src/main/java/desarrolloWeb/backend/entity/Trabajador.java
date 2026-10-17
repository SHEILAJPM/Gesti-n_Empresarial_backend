package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trabajadores")
public class Trabajador {

    public static final String ACTIVO = "ACTIVO";
    public static final String CESADO = "CESADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombres;

    @Column(unique = true)
    private String documentoIdentidad;

    private String cargo;

    private String area;

    // ACTIVO o CESADO; al cesar se conserva el historial (asistencia, documentos, pagos)
    private String estado;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(precision = 12, scale = 2)
    private BigDecimal sueldo;

    // ONP o AFP
    @Column(name = "sistema_pension", length = 3)
    private String sistemaPension;

    @Column(length = 30)
    private String afp;

    @Column(length = 60)
    private String banco;

    @Column(name = "numero_cuenta", length = 30)
    private String numeroCuenta;

    @Column(name = "fecha_cese")
    private LocalDate fechaCese;

    @Column(name = "motivo_cese", length = 250)
    private String motivoCese;

    public Trabajador() {
    }

    public Trabajador(Long id, String nombres, String documentoIdentidad,
                      String cargo, String area, String estado) {
        this.id = id;
        this.nombres = nombres;
        this.documentoIdentidad = documentoIdentidad;
        this.cargo = cargo;
        this.area = area;
        this.estado = estado;
    }

    public boolean estaActivo() {
        return ACTIVO.equals(estado);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public BigDecimal getSueldo() {
        return sueldo;
    }

    public void setSueldo(BigDecimal sueldo) {
        this.sueldo = sueldo;
    }

    public String getSistemaPension() {
        return sistemaPension;
    }

    public void setSistemaPension(String sistemaPension) {
        this.sistemaPension = sistemaPension;
    }

    public String getAfp() {
        return afp;
    }

    public void setAfp(String afp) {
        this.afp = afp;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public LocalDate getFechaCese() {
        return fechaCese;
    }

    public void setFechaCese(LocalDate fechaCese) {
        this.fechaCese = fechaCese;
    }

    public String getMotivoCese() {
        return motivoCese;
    }

    public void setMotivoCese(String motivoCese) {
        this.motivoCese = motivoCese;
    }
}
