-- Se Amplía la respuestas_furag.respuesta para soportar el valor 'NO_APLICA' de 9 caracteres
-- del enum RespuestaFurag. La columna original era anteriormente character varying(5), lo que
-- provocaba DataIntegrityViolationException al guardar respuestas NO_APLICA lo cual es incorrecto.
ALTER TABLE proyecta_db.respuestas_furag
    ALTER COLUMN respuesta TYPE character varying(20);
