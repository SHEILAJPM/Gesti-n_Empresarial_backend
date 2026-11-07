-- Momento del último cambio de contraseña (UTC): los tokens emitidos antes dejan de servir
ALTER TABLE usuarios ADD COLUMN clave_cambiada_en TIMESTAMP;

-- Horario de la sede: con él se calculan tardanzas y faltas en el resumen mensual de asistencia
ALTER TABLE sedes ADD COLUMN hora_entrada TIME DEFAULT '08:00:00' NOT NULL;
ALTER TABLE sedes ADD COLUMN hora_salida TIME DEFAULT '17:00:00' NOT NULL;
ALTER TABLE sedes ADD COLUMN tolerancia_minutos INTEGER DEFAULT 10 NOT NULL;
ALTER TABLE sedes ADD CONSTRAINT ck_sedes_tolerancia CHECK (tolerancia_minutos BETWEEN 0 AND 120);
