-- Comprobante electrónico de cada venta (facturación electrónica: simulador local o APISUNAT).

ALTER TABLE ventas ADD COLUMN sunat_estado VARCHAR(12) DEFAULT 'NO_ENVIADO' NOT NULL;
ALTER TABLE ventas ADD COLUMN sunat_proveedor VARCHAR(12);
ALTER TABLE ventas ADD COLUMN sunat_document_id VARCHAR(60);
ALTER TABLE ventas ADD COLUMN sunat_file_name VARCHAR(40);
ALTER TABLE ventas ADD COLUMN sunat_mensaje VARCHAR(500);
ALTER TABLE ventas ADD COLUMN sunat_xml_url VARCHAR(300);
ALTER TABLE ventas ADD COLUMN sunat_cdr_url VARCHAR(300);
ALTER TABLE ventas ADD COLUMN sunat_fecha_envio TIMESTAMP;

ALTER TABLE ventas ADD CONSTRAINT ck_ventas_sunat_estado
    CHECK (sunat_estado IN ('NO_ENVIADO', 'PENDIENTE', 'ACEPTADO', 'RECHAZADO', 'EXCEPCION', 'ERROR'));

CREATE INDEX idx_ventas_sunat_estado ON ventas (sunat_estado);
