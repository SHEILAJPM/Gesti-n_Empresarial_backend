-- Retención de renta de 5ta categoría en la planilla. Las planillas ya generadas quedan con 0.
ALTER TABLE planilla_detalles ADD COLUMN renta_quinta NUMERIC(12, 2) DEFAULT 0 NOT NULL;
