-- B-02: legacy procurement records belong to a tenant; existing rows belong to the legacy tenant. Catalog items and
-- order/receipt/invoice lines are scoped through their parent records. Plain statements (no DO block) so the same
-- migration also runs on the H2 test database.

ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE suppliers SET tenant_id = '00000000-0000-4000-8000-000000000001' WHERE tenant_id IS NULL;
ALTER TABLE suppliers ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-4000-8000-000000000001';
ALTER TABLE suppliers ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant ON suppliers (tenant_id);

ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE purchase_orders SET tenant_id = '00000000-0000-4000-8000-000000000001' WHERE tenant_id IS NULL;
ALTER TABLE purchase_orders ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-4000-8000-000000000001';
ALTER TABLE purchase_orders ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_purchase_orders_tenant ON purchase_orders (tenant_id);

ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE goods_receipts SET tenant_id = '00000000-0000-4000-8000-000000000001' WHERE tenant_id IS NULL;
ALTER TABLE goods_receipts ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-4000-8000-000000000001';
ALTER TABLE goods_receipts ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_goods_receipts_tenant ON goods_receipts (tenant_id);

ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE supplier_invoices SET tenant_id = '00000000-0000-4000-8000-000000000001' WHERE tenant_id IS NULL;
ALTER TABLE supplier_invoices ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-4000-8000-000000000001';
ALTER TABLE supplier_invoices ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_supplier_invoices_tenant ON supplier_invoices (tenant_id);

ALTER TABLE procurement_audit ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE procurement_audit SET tenant_id = '00000000-0000-4000-8000-000000000001' WHERE tenant_id IS NULL;
ALTER TABLE procurement_audit ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-4000-8000-000000000001';
ALTER TABLE procurement_audit ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_procurement_audit_tenant ON procurement_audit (tenant_id);

-- Business numbers are unique per tenant.
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_supplier_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_suppliers_tenant_code ON suppliers (tenant_id, supplier_code);
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS purchase_orders_po_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_purchase_orders_tenant_number ON purchase_orders (tenant_id, po_number);
ALTER TABLE goods_receipts DROP CONSTRAINT IF EXISTS goods_receipts_receipt_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_goods_receipts_tenant_number ON goods_receipts (tenant_id, receipt_number);
