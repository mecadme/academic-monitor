# ADR-004: Authentication strategy

## Estado

Implementado en `feature/authentication-multiuser`.

## Decision

El MVP usa correo y contraseña con Argon2id. Spring Security valida JWT HS256 mediante Nimbus. La clave Base64 debe proceder del entorno y contener al menos 32 bytes; no se genera al arrancar. El access token dura 15 minutos y el refresh 7 días, configurables.

Ambos viajan en cookies HttpOnly. La cookie CSRF deliberadamente legible por JavaScript permite enviar `X-XSRF-TOKEN`. No se usa HttpSession ni almacenamiento web para los tokens. Los refresh son aleatorios, se persisten únicamente como SHA-256 y se consumen con bloqueo transaccional para permitir una sola rotación concurrente.

El principal centraliza userId, institutionId y roles. Los controllers derivan el ownership desde ese principal; los servicios de dominio conservan sus filtros institucionales y de docente.

## Consecuencias

El bootstrap queda limitado al perfil dev y al inicio del proceso. Conserva el usuario y membresía existentes y sólo añade un hash si no existe. Se elimina la API pública de bootstrap.

No hay registro público. El flujo de invitaciones y el rate limiting de login quedan para próximas features. No se añade infraestructura distribuida.
