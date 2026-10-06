# Autenticación multiusuario

Academic Monitor autentica con correo y contraseña. La conexión con Idukay es independiente: cada sesión Idukay permanece indexada por `(institutionId, teacherUserId)` y recibe esos valores del principal autenticado.

## Configuración local

En `.env` local, conserva `SPRING_PROFILES_ACTIVE=dev` y el correo del docente existente en `APP_BOOTSTRAP_USER_EMAIL`. No cambies el correo para habilitar autenticación: se reutilizan su User.id, membresía y datos sincronizados.

```dotenv
APP_AUTH_JWT_SECRET_BASE64=
APP_BOOTSTRAP_PASSWORD=
APP_AUTH_COOKIE_SECURE=false
APP_AUTH_ACCESS_TTL=15m
APP_AUTH_REFRESH_TTL=7d
APP_AUTH_COOKIE_SAME_SITE=Lax
```

Genera tú mismo una clave aleatoria de 32 bytes en PowerShell y coloca el resultado únicamente en tu configuración privada:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $jwtRandom.GetBytes($jwtBytes)
    [Convert]::ToBase64String($jwtBytes)
} finally {
    $jwtRandom.Dispose()
    [Array]::Clear($jwtBytes, 0, $jwtBytes.Length)
}
```

Configura una contraseña propia en `APP_BOOTSTRAP_PASSWORD`. No hay una contraseña predeterminada. Sólo se establece si el usuario aún no tiene hash. Una vez creado, puedes vaciar esa variable: no se reescribe el hash en arranques posteriores. El bootstrap sólo corre en `dev`, mediante ApplicationRunner; no existe endpoint público que cree identidad. Una membresía inactiva no se reemplaza automáticamente por una institución nueva.

No publiques `.env`, claves ni contraseñas. Cambiar la clave JWT invalida access tokens existentes. Usa una clave estable suministrada desde el entorno. En producción usa HTTPS y `APP_AUTH_COOKIE_SECURE=true`, perfil `prod` y `FRONTEND_ORIGINS` explícitos. No configures `*` como origen. No es necesario establecer dominio de cookie.

Docker Compose lee `.env`. Para Maven/IDE local exporta las variables al proceso e inicia con perfil dev; Spring Boot no importa `.env` automáticamente. El navegador debe usar el mismo hostname para frontend y backend en desarrollo, por ejemplo `localhost:3000` y `localhost:8080`.

```powershell
docker compose build backend
docker compose up -d --build backend frontend
docker compose ps
```

No se borran datos ni volúmenes. Flyway aplica V9 sobre V8 y Hibernate valida el esquema.

## Sesión y seguridad

- `am_access`: JWT HS256 de 15 minutos, HttpOnly, Path=/.
- `am_refresh`: 32 bytes aleatorios como mínimo, HttpOnly, Path=/api/v1/auth, 7 días.
- Ambas usan Secure configurable y SameSite=Lax por defecto.
- `XSRF-TOKEN`: cookie legible por React, enviada en `X-XSRF-TOKEN` para operaciones mutables. CSRF también protege login, refresh y logout.
- JWT contiene `sub`, `institutionId`, `systemRole`, `institutionRole`, `iat`, `exp`, `jti` e issuer; no contiene información académica o credenciales.
- `auth_refresh_tokens` guarda únicamente SHA-256 del refresh, familia, fechas y revocación/reemplazo. El bloqueo de fila serializa consumos concurrentes: sólo uno puede rotar un token.
- Cada refresh revalida usuario, institución y membresía activos. Un access emitido puede durar hasta su vencimiento.
- Logout revoca el refresh de esa sesión y borra ambas cookies. No revoca access JWT que ya hubieran sido copiados; éstos expiran con su TTL.
- Spring Security es stateless: no se usa HttpSession para autenticación.

El frontend inicializa CSRF y consulta `/auth/me`, comparte un refresh entre requests concurrentes y reintenta una sola vez. Si no hay sesión válida, presenta `/login`. El selector institucional aparece cuando hay más de una membresía activa. La institución enviada en login siempre se valida contra las membresías reales.

El email y rol institucional aparecen en el menú de usuario. No hay tokens en JSON, localStorage o sessionStorage. Los IDs recibidos en `/auth/me` pueden servir como claves de estado/preferencias, pero no se envían como ownership de las APIs académicas.

## Comprobación manual

1. Abre `http://localhost:3000/login`, inicia con el correo bootstrap y tu contraseña local.
2. Comprueba en las herramientas de desarrollo que `am_access` y `am_refresh` son HttpOnly y las respuestas JSON no incluyen tokens.
3. Revisa que una petición académica no envía institutionId/teacherUserId y que las operaciones mutables incluyen X-XSRF-TOKEN.
4. Elimina sólo am_access para comprobar refresh/restauración; cierra sesión y verifica el retorno a login y cookies borradas.
5. Sin X-XSRF-TOKEN, un POST/PATCH/DELETE devuelve 403. Sin sesión, una lectura protegida devuelve 401.

Los tests automatizados usan identidades y secretos exclusivos de prueba. Incluyen rotación, acceso manipulado/caducado, CSRF, aislamiento de docentes/instituciones y sesiones Idukay sin contactar con el servicio real.

## Pendientes deliberados

La creación por invitaciones ADMIN se implementará en `feature/user-invitations`; no hay registro público. El modelo permite añadir establecer/cambiar contraseña y revocar todas las sesiones. Rate limiting de login queda como hardening futuro; no se agrega Redis ni un limitador en memoria que pretenda cubrir despliegues distribuidos.

Referencias: [CSRF para SPA de Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) y [JWT con Spring Security/Nimbus](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
