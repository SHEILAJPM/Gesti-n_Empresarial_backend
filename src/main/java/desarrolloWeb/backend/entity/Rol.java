package desarrolloWeb.backend.entity;

public enum Rol {
    ADMIN,
    RRHH,
    CONTABILIDAD,
    ADMINISTRACION,
    // Roles del personal: siempre ligados a una ficha de trabajador; marcan asistencia desde su portal
    TRABAJADOR,
    VENDEDOR,
    ALMACEN;

    /* Estos roles no pueden existir sin un trabajador asociado */
    public boolean esPersonal() {
        return this == TRABAJADOR || this == VENDEDOR || this == ALMACEN;
    }
}
