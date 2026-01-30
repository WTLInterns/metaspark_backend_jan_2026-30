CREATE TABLE order_checkbox_base_selection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pdf_type VARCHAR(10) NOT NULL,
  scope VARCHAR(50) NOT NULL,
  row_key VARCHAR(255) NOT NULL,
  created_by_user_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_base_order_pdf_scope_row (order_id, pdf_type, scope, row_key),
  INDEX idx_base_order_pdf_scope (order_id, pdf_type, scope)
);

CREATE TABLE order_checkbox_assignment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pdf_type VARCHAR(10) NOT NULL,
  scope VARCHAR(50) NOT NULL,
  row_key VARCHAR(255) NOT NULL,
  assigned_to_user_id BIGINT NOT NULL,
  assigned_by_user_id BIGINT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_assign_order_pdf_scope_row (order_id, pdf_type, scope, row_key),
  INDEX idx_assign_assignee (assigned_to_user_id),
  INDEX idx_assign_order_pdf_scope (order_id, pdf_type, scope)
);
