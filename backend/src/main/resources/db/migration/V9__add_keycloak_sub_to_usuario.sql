ALTER TABLE proyecta_db.usuario
    ADD COLUMN IF NOT EXISTS keycloak_sub VARCHAR(120) UNIQUE;

COMMENT ON COLUMN proyecta_db.usuario.keycloak_sub IS 'Identificador estable del usuario en Keycloak (claim sub)';
