-- Ficha completa del trabajador y baja (cese) en lugar de eliminar su historial.

ALTER TABLE trabajadores ADD COLUMN fecha_nacimiento DATE;
ALTER TABLE trabajadores ADD COLUMN telefono VARCHAR(20);
ALTER TABLE trabajadores ADD COLUMN correo VARCHAR(120);
ALTER TABLE trabajadores ADD COLUMN direccion VARCHAR(200);
ALTER TABLE trabajadores ADD COLUMN fecha_ingreso DATE;
ALTER TABLE trabajadores ADD COLUMN sueldo NUMERIC(12, 2);
ALTER TABLE trabajadores ADD COLUMN sistema_pension VARCHAR(3);
ALTER TABLE trabajadores ADD COLUMN afp VARCHAR(30);
ALTER TABLE trabajadores ADD COLUMN banco VARCHAR(60);
ALTER TABLE trabajadores ADD COLUMN numero_cuenta VARCHAR(30);
ALTER TABLE trabajadores ADD COLUMN fecha_cese DATE;
ALTER TABLE trabajadores ADD COLUMN motivo_cese VARCHAR(250);

-- Los registros previos quedan activos; antes se usaba también INACTIVO
UPDATE trabajadores SET estado = 'ACTIVO' WHERE estado IS NULL OR estado NOT IN ('ACTIVO', 'CESADO');

ALTER TABLE trabajadores ADD CONSTRAINT ck_trabajadores_estado CHECK (estado IN ('ACTIVO', 'CESADO'));
ALTER TABLE trabajadores ADD CONSTRAINT ck_trabajadores_pension CHECK (sistema_pension IS NULL OR sistema_pension IN ('ONP', 'AFP'));
ALTER TABLE trabajadores ADD CONSTRAINT uk_trabajadores_documento UNIQUE (documento_identidad);
