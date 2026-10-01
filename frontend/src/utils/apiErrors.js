/**
 * Extrae el mensaje legible de un error de la API sin exponer objetos internos.
 *
 * Prioriza el campo `detail` del backend, luego `message` y por ultimo el
 * fallback; nunca devuelve el objeto de error completo (CWE-209: no filtrar
 * detalles internos ni la peticion original con su header Authorization).
 */
export function extractApiDetail(err, fallback = 'Ocurrio un error inesperado.') {
  const apiMessage = err?.response?.data?.detail ?? err?.response?.data?.message;
  if (typeof apiMessage === 'string' && apiMessage.trim()) {
    return apiMessage;
  }
  if (typeof err?.message === 'string' && err.message.trim() && !err?.response) {
    return err.message;
  }
  return fallback;
}

export default extractApiDetail;
