# API

La API REST queda versionada desde el inicio bajo `/api/v1`.

Salud:

```http
GET /api/v1/health
```

La API académica requiere autenticación y deriva institución/docente de las cookies. Los parámetros `institutionId` y `teacherUserId` ya no autorizan acceso. `courseId`, `academicPeriodId`, IDs de alertas y comunicaciones continúan validados dentro de ese contexto.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/v1/auth/csrf` | Inicializar la cookie CSRF |
| POST | `/api/v1/auth/login` | Correo, contraseña, institutionId opcional validado |
| POST | `/api/v1/auth/refresh` | Rotar el refresh de la cookie |
| POST | `/api/v1/auth/logout` | Revocar la sesión y limpiar cookies |
| GET | `/api/v1/auth/me` | Usuario e institución actuales |

Los POST, PATCH, PUT y DELETE necesitan el header `X-XSRF-TOKEN`, incluidos login, refresh y logout. Login/me devuelven datos seguros, nunca tokens. La selección institucional usa `INSTITUTION_SELECTION_REQUIRED` e `institutions` con institutionId, institutionName e institutionRole.

`POST /api/v1/context/bootstrap` fue eliminado. Swagger/OpenAPI están deshabilitados fuera de dev; en dev conservan protección de acceso. Los endpoints Idukay `test-*` también requieren perfil dev, bandera `app.idukay.test-login-enabled` y docente autenticado. Consulta la [guía de autenticación](../authentication.md).
