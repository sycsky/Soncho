-- Alter purchase_order_items quantity fields to support decimals
ALTER TABLE `purchase_order_items`
  MODIFY COLUMN `quantity_requested` DECIMAL(12,3) NULL,
  MODIFY COLUMN `quantity_shipped` DECIMAL(12,3) NULL,
  MODIFY COLUMN `quantity_received` DECIMAL(12,3) NULL;

