# Security Policy

## Reportar vulnerabilidades

Si encuentras una vulnerabilidad, reportala de forma privada al mantenedor del repositorio. No abras issues publicos con detalles explotables.

## Repositorio publico

No se deben versionar:

- Passwords reales.
- Tokens de API.
- Cookies.
- Claves criptograficas.
- Archivos `.pem` o `.key`.
- Exports de Idukay.
- Dumps de base de datos.
- Logs.
- Datos reales de estudiantes, docentes o instituciones.

Usa `.env` localmente y conserva `.env.example` como plantilla publica con valores ficticios.
