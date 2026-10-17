-- Datos de ejemplo: solo se cargan con el perfil "local" (ver application-local.properties).
-- Agosto 2026: compra 100.00 (IGV 18.00) y venta emitida 300.00 (IGV 54.00).
-- La factura F001-1 de agosto está ANULADA: no cuenta en el resumen.
-- Resultado esperado de /api/tributario/resumen-mensual?anio=2026&mes=8:
--   crédito 18.00 · débito 54.00 · IGV por pagar 36.00
-- El IGV y los usuarios base los crean los inicializadores al arrancar.

INSERT INTO proveedores (ruc, razon_social, direccion) VALUES
  ('20123456789', 'Distribuidora ACME S.A.C.', 'Av. Arequipa 1234, Lima'),
  ('20987654321', 'Suministros del Sur E.I.R.L.', NULL);

INSERT INTO clientes (documento, nombre, tipo_documento) VALUES
  ('10203040', 'Carlos Mendoza', 'DNI'),
  ('45678912', 'Lucía Paredes', 'DNI'),
  ('20555666777', 'Comercial Lima S.A.C.', 'RUC');

INSERT INTO compras (proveedor_id, numero_comprobante, fecha, monto_sin_igv, igv, monto_total, tasa_aplicada) VALUES
  ((SELECT id FROM proveedores WHERE ruc = '20123456789'), 'F001-101', DATE '2026-08-05', 100.00, 18.00, 118.00, 0.1800),
  ((SELECT id FROM proveedores WHERE ruc = '20987654321'), 'F002-201', DATE '2026-07-20', 250.00, 45.00, 295.00, 0.1800),
  ((SELECT id FROM proveedores WHERE ruc = '20123456789'), 'F001-102', DATE '2026-09-03', 400.00, 72.00, 472.00, 0.1800);

INSERT INTO ventas (cliente_id, tipo_comprobante, numero_comprobante, fecha, monto_sin_igv, igv, monto_total, tasa_aplicada, estado) VALUES
  ((SELECT id FROM clientes WHERE documento = '10203040'),    'BOLETA',  'B001-1', DATE '2026-08-10', 300.00, 54.00, 354.00, 0.1800, 'EMITIDA'),
  ((SELECT id FROM clientes WHERE documento = '20555666777'), 'FACTURA', 'F001-1', DATE '2026-08-12', 200.00, 36.00, 236.00, 0.1800, 'ANULADA'),
  ((SELECT id FROM clientes WHERE documento = '45678912'),    'BOLETA',  'B001-2', DATE '2026-07-15', 150.00, 27.00, 177.00, 0.1800, 'EMITIDA'),
  ((SELECT id FROM clientes WHERE documento = '20555666777'), 'FACTURA', 'F001-2', DATE '2026-09-05', 500.00, 90.00, 590.00, 0.1800, 'EMITIDA');
