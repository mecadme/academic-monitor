# Academic Monitor — Especificación Técnica v1.0

**Estado:** Aprobada para inicio de desarrollo  
**Arquitectura:** Monolito modular con principios hexagonales  
**Repositorio:** `academic-monitor`  
**Visibilidad:** Público  
**Licencia:** Sin licencia inicialmente  
**Producto visible:** Academic Monitor — nombre provisional

---

## 1. Visión del producto

Academic Monitor será una plataforma web para seguimiento académico docente que permitirá sincronizar información procedente de plataformas educativas, detectar automáticamente situaciones que requieran atención mediante reglas deterministas y facilitar la comunicación con representantes mediante mensajes personalizados generados con inteligencia artificial local.

El producto no estará diseñado exclusivamente para docentes tutores. Todo docente podrá monitorear sus propias asignaturas y cursos. Las capacidades de tutoría constituirán un módulo opcional disponible únicamente cuando exista una asignación de tutoría.

La primera plataforma integrada será Idukay; sin embargo, el núcleo de Academic Monitor no dependerá directamente de clases, identificadores o estructuras específicas de Idukay.

---

## 2. Objetivos principales

Academic Monitor permitirá:

- descubrir automáticamente los cursos disponibles para un docente;
- seleccionar qué cursos desea monitorear;
- sincronizar estudiantes, actividades y calificaciones;
- conservar el estado actual y el historial de modificaciones de las calificaciones;
- evaluar cada nueva calificación mediante políticas configurables;
- generar alertas sin duplicados;
- diferenciar categorías y niveles de alerta;
- resolver automáticamente una alerta cuando la calificación deje de cumplir su condición, conservando el antecedente;
- mostrar la evolución académica individual del estudiante;
- agrupar visualmente alertas de una misma actividad;
- generar mensajes personalizados mediante un LLM local;
- permitir que el docente modifique y apruebe el mensaje;
- registrar comunicaciones y cambios mediante auditoría;
- enviar posteriormente las comunicaciones mediante Idukay cuando la API lo permita;
- habilitar seguimiento adicional para docentes que también sean tutores.

---

## 3. Principios arquitectónicos

Se utilizará un **monolito modular**, evitando microservicios durante las primeras etapas.

Cada área funcional tendrá límites claros y las dependencias deberán dirigirse hacia el dominio y los casos de uso, no hacia tecnologías concretas.

No se utilizará una arquitectura hexagonal excesivamente ceremonial. Se crearán puertos e interfaces cuando exista una razón real de desacoplamiento, especialmente en:

**Plataformas académicas externas →** Idukay.  
**Inteligencia artificial →** Ollama.  
**Entrega de comunicaciones →** correo interno de Idukay.  
**Persistencia →** PostgreSQL cuando el dominio requiera aislamiento.

El dominio no deberá conocer conceptos como:

```text
Idukay
Ollama
React
PostgreSQL
HTTP
Docker
```

De esta forma podrá reemplazarse una integración sin modificar las reglas académicas.

---

## 4. Stack tecnológico

### Backend

| Tecnología | Decisión |
|---|---|
| Lenguaje | Java 21 LTS |
| Framework | Spring Boot 4.1.x |
| Build | Maven |
| Seguridad | Spring Security |
| Persistencia | Spring Data JPA |
| Validación | Jakarta Bean Validation |
| Migraciones | Flyway |
| Documentación API | OpenAPI / Swagger |
| Health | Spring Boot Actuator |
| Testing | JUnit 5 + Testcontainers + ArchUnit |
| Formato | Spotless |
| Análisis | SpotBugs |

Spring Boot 4.1.1 requiere Java 17 como mínimo y soporta actualmente hasta Java 26, por lo que Java 21 constituye una elección compatible y conservadora para el proyecto. citeturn973432search0

### Frontend

| Tecnología | Decisión |
|---|---|
| Framework | React 19 |
| Lenguaje | TypeScript |
| Build | Vite 8 |
| Paquetes | pnpm |
| UI | shadcn/ui + Tailwind CSS |
| Routing | React Router |
| Estado servidor | TanStack Query |
| Estado local | React |
| Estado global adicional | Zustand solo si aparece una necesidad |
| Formularios | React Hook Form |
| Validación | Zod |
| Tests unitarios | Vitest + Testing Library |
| E2E | Playwright |
| Calidad | ESLint + Prettier |

React mantiene actualmente 19.2 como versión vigente de su documentación y Vite 8.1 fue publicado en junio de 2026. citeturn973432search1turn973432search6

### Datos e infraestructura

| Componente | Tecnología |
|---|---|
| Base de datos | PostgreSQL 18 |
| IA | Ollama |
| Contenedores | Docker Compose |
| CI | GitHub Actions |
| Código | GitHub público |

PostgreSQL 18 es actualmente la versión estable principal y la rama 18.6 fue publicada el 13 de agosto de 2026. PostgreSQL 18 incorpora además soporte nativo para `uuidv7()`. citeturn973432search2turn973432search12turn973432search13

---

## 5. Namespace Java

El namespace raíz será:

```text
io.academicmonitor
```

Ejemplo:

```java
package io.academicmonitor.monitoring.domain;
```

El nombre comercial podrá cambiar posteriormente sin necesidad de cambiar inmediatamente el namespace interno.

---

## 6. Organización backend

La organización será **package-by-feature**, evitando una estructura global basada únicamente en `controller/service/repository`.

Estructura conceptual:

```text
io.academicmonitor

├── shared
│   ├── security
│   ├── audit
│   ├── error
│   ├── time
│   └── observability
│
├── identity
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── institution
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── academic
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── monitoring
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── tutoring
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── communication
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── ai
│   ├── application
│   └── infrastructure
│
└── integration
    └── idukay
```

ArchUnit verificará que los límites arquitectónicos establecidos no sean violados accidentalmente.

---

## 7. Multitenencia

Academic Monitor quedará preparado para múltiples instituciones desde su diseño inicial.

Se utilizará:

```text
1 PostgreSQL
1 esquema compartido
institution_id en entidades institucionales
```

No se utilizará una base de datos por institución ni un esquema por institución durante el MVP.

Los filtros de tenant se aplicarán desde la aplicación y se reforzarán mediante restricciones e índices en PostgreSQL.

El diseño permitirá incorporar posteriormente **PostgreSQL Row Level Security** sin modificar el modelo funcional.

El `institution_id` se obtendrá del contexto autenticado. Un cliente React nunca podrá seleccionar libremente otro `institution_id` mediante una petición HTTP para intentar acceder a otra institución.

---

## 8. Usuarios y roles

El usuario será una identidad global.

La relación con las instituciones se manejará mediante membresías, permitiendo que el modelo pueda soportar en el futuro que una misma persona pertenezca a más de una institución.

Roles iniciales:

```text
SUPER_ADMIN
ADMIN
TEACHER
```

### SUPER_ADMIN

Administra la plataforma completa y será el único rol autorizado inicialmente para modificar políticas académicas globales, categorías y umbrales de alerta.

### ADMIN

Administra los usuarios y configuraciones permitidas de su institución.

### TEACHER

Puede conectar su plataforma académica, seleccionar cursos, sincronizar información, revisar estudiantes, calificaciones y alertas, generar mensajes y gestionar comunicaciones.

Ser tutor no constituye un rol.

Una asignación adicional:

```text
TutoringAssignment
```

determinará si el docente dispone de funciones de tutoría.

---

## 9. Autenticación de Academic Monitor

El MVP utilizará:

```text
correo electrónico
+
contraseña
```

Las cuentas serán creadas mediante invitación de un administrador.

No existirá inicialmente registro público.

Las contraseñas se almacenarán mediante **Argon2id**, nunca mediante cifrado reversible ni hashes rápidos. OWASP recomienda actualmente Argon2id como primera opción para nuevas aplicaciones de almacenamiento de contraseñas. citeturn922860search1

Se utilizarán:

```text
Access Token JWT
+
Refresh Token
```

El access token tendrá vida corta y el refresh token una duración mayor; ambos tiempos serán configurables.

Valores iniciales de desarrollo:

```text
Access token: 15 minutos
Refresh token: 7 días
```

La autenticación viajará mediante cookies:

```text
HttpOnly
Secure en producción
SameSite
```

No se almacenarán tokens de autenticación en `localStorage`.

Los refresh tokens serán rotatorios y revocables. Su representación persistente no se almacenará como texto reutilizable.

Al utilizar autenticación basada en cookies, Spring Security mantendrá protección CSRF para las operaciones que modifiquen estado.

---

## 10. Integración con Idukay

Idukay será tratado como una plataforma externa.

Puerto conceptual:

```java
public interface AcademicPlatformPort {

    List<ExternalCourse> findTeacherCourses();

    List<ExternalStudent> findStudents(String courseId);

    List<ExternalActivity> findActivities(String courseId);

    List<ExternalGrade> findGrades(String courseId);
}
```

Implementación:

```text
IdukayAcademicPlatformAdapter
```

Los identificadores externos de Idukay **no serán primary keys internas**.

Cada entidad dispondrá de:

```text
id            → UUID v7 propio
external_id   → identificador proporcionado por Idukay
platform      → IDUKAY
```

Esto permitirá reemplazar o añadir otra plataforma posteriormente.

### Credenciales

Debido a que todavía debe confirmarse si Idukay entrega tokens por docente o por institución, `PlatformConnection` soportará ambos alcances:

```text
USER
INSTITUTION
```

De esta forma ninguna de las dos respuestas exigirá modificar el modelo.

Los tokens de Idukay se almacenarán cifrados en PostgreSQL.

El material criptográfico utilizado para descifrarlos permanecerá fuera de PostgreSQL y del repositorio.

El cifrado deberá proporcionar confidencialidad e integridad, utilizando un mecanismo autenticado como AES-GCM.

Los administradores podrán:

```text
reemplazar credencial
revocar conexión
probar conexión
```

pero **no visualizar nuevamente el token original**.

OWASP recomienda mantener las claves de cifrado separadas de los datos que protegen y minimizar la información sensible persistida. citeturn922860search6

---

## 11. Sincronización

El MVP ejecutará sincronizaciones manuales desde la interfaz.

Ejemplo:

```text
[ Sincronizar ]
```

Internamente la operación no permanecerá asociada a una petición HTTP larga.

Se creará:

```text
SyncJob

PENDING
  ↓
RUNNING
  ↓
SUCCESS
   o
FAILED
```

El job almacenará:

```text
usuario solicitante
institución
plataforma
inicio
finalización
estado
cursos procesados
estudiantes procesados
actividades procesadas
calificaciones procesadas
resumen de errores
```

Las sincronizaciones automáticas se incorporarán posteriormente reutilizando exactamente los mismos casos de uso.

No se utilizará Kafka ni RabbitMQ en el MVP.

---

## 12. Modelo académico

Entidades iniciales:

```text
Institution
User
InstitutionMembership
PlatformConnection

Teacher
Course
Student
CourseEnrollment
Activity
Grade
GradeHistory

MonitoringSelection
AlertCategory
AlertPolicy
Alert
AlertEvent

TutoringAssignment

MessageDraft
Communication
PromptVersion

SyncJob
AuditEvent
```

---

## 13. UUID

Las entidades utilizarán **UUID v7**.

Ventajas buscadas:

```text
identificador global
sin dependencia de secuencias externas
orden temporal aproximado
mejor comportamiento de índices que UUID totalmente aleatorios
```

PostgreSQL 18 dispone actualmente de `uuidv7()` nativo. citeturn973432search13

El lugar exacto de generación —aplicación o PostgreSQL— se decidirá durante la implementación del modelo persistente, pero el contrato será UUID v7.

---

## 14. Historial de calificaciones

No se conservará únicamente la última calificación.

Ejemplo:

```text
6.4
 ↓
8.0
 ↓
5.5
```

`Grade` contendrá el estado actual.

`GradeHistory` registrará cada cambio relevante.

No se generará un snapshot completo de todos los datos en cada sincronización.

---

## 15. Motor de monitoreo

La decisión de generar una alerta será completamente determinista.

El LLM **no determinará si un estudiante se encuentra o no en riesgo**.

Flujo:

```text
Grade
  ↓
Monitoring Engine
  ↓
AlertPolicy
  ↓
¿coincide?
 /       \
NO       SÍ
          ↓
        Alert
```

La primera clase de regla será:

```text
ACTIVITY_GRADE
```

Se preparará el modelo para operadores:

```text
LT
LTE
EQ
GT
GTE
BETWEEN
```

Esto permitirá, por ejemplo:

```text
nota = 7       → AVISO
nota < 7       → ALERTA
```

sin introducir estas condiciones directamente en código.

Las categorías y políticas serán configuradas exclusivamente por `SUPER_ADMIN`.

---

## 16. Prevención de duplicados

Una misma condición persistente no generará una alerta nueva después de cada sincronización.

La identidad funcional incluirá aproximadamente:

```text
platform
institution
studentExternalId
courseExternalId
activityExternalId
policyId
```

Ejemplo:

```text
Primera sincronización
6.4 → crea alerta

Segunda sincronización
6.4 → no duplica

Cambio
6.4 → 8.0
→ alerta RESOLVED

Cambio posterior
8.0 → 5.5
→ nuevo evento / nueva incidencia
```

El historial permanecerá disponible.

---

## 17. Visualización agrupada

Si diez estudiantes obtienen una calificación que activa la misma política en una actividad:

```text
Prueba de movimiento
10 estudiantes con alerta
```

React podrá presentarlos agrupados.

Sin embargo, internamente existirán diez alertas independientes.

Esto permite:

```text
resolver individualmente
comunicar individualmente
auditar individualmente
consultar historial individual
```

---

## 18. Perfil del estudiante

Academic Monitor tendrá una vista individual que mostrará, como mínimo:

```text
información académica básica
cursos
actividades
calificaciones
evolución temporal
alertas abiertas
alertas resueltas
comunicaciones
```

La interfaz deberá permitir comprender rápidamente la evolución académica sin exponer información innecesaria.

---

## 19. Tutoría

Tutoría será un módulo opcional.

```text
Teacher
   │
   ├── Course assignments
   │
   └── TutoringAssignment [0..n]
```

Cuando exista una asignación de tutoría, Academic Monitor podrá analizar las calificaciones disponibles de los tutoriados en otras asignaturas, siempre dentro de los permisos proporcionados por la plataforma académica.

Un docente sin tutoría no visualizará controles innecesarios de dicho módulo.

---

## 20. Inteligencia artificial

El primer proveedor será Ollama.

Puerto:

```java
public interface MessageGeneratorPort {

    GeneratedMessage generate(
        MessageGenerationContext context
    );
}
```

Adaptador inicial:

```text
OllamaMessageGeneratorAdapter
```

Existirá además un generador mediante plantilla determinista como fallback.

```text
Ollama disponible
       ↓
Mensaje IA

Ollama no disponible
       ↓
Plantilla estándar
```

La aplicación continuará funcionando aunque el LLM esté detenido.

`OLLAMA_BASE_URL` será configurable para permitir:

```text
Ollama en mismo equipo
Ollama en servidor separado
Ollama en VPS
```

sin modificar código.

---

## 21. Privacidad en el LLM

Solo se enviará al modelo la información académica estrictamente necesaria.

Ejemplo:

```text
Asignatura
Actividad
Calificación
Umbral
Fecha
Historial académico mínimo pertinente
Preferencias del docente
```

No se incorporarán automáticamente:

```text
cédula
dirección
teléfono
datos familiares
información no relacionada
```

La identidad del estudiante se añadirá localmente cuando sea necesaria para el mensaje final.

---

## 22. Prompts

Los prompts del sistema serán versionados.

Ejemplo:

```text
academic-notification-v1
academic-notification-v2
```

Cada mensaje generado conservará:

```text
prompt_version
modelo
fecha
parámetros esenciales
```

Además podrá incorporar preferencias del docente respecto a:

```text
tono
extensión
forma de saludo
recomendaciones
cierre
```

El docente no modificará libremente el system prompt del producto.

---

## 23. Ciclo de comunicación

Flujo:

```text
Alert
  ↓
[Generar mensaje]
  ↓
LLM
  ↓
Borrador
  ↓
Docente revisa
  ↓
Docente modifica
  ↓
Docente aprueba
  ↓
Idukay
  ↓
Communication SENT
```

El LLM nunca enviará directamente una comunicación.

Si la API de Idukay no permite enviar correo interno, se implementará inicialmente un mecanismo de fallback sin modificar `CommunicationService`.

---

## 24. Retención de mensajes LLM

Se conservarán por separado:

```text
respuesta original del modelo
texto finalmente aprobado
texto enviado
```

La respuesta original estará sometida a una política de retención configurable.

No se fijará todavía un número definitivo de días porque esta decisión deberá alinearse con la política institucional de tratamiento de información.

---

## 25. Auditoría

Existirá una tabla inmutable:

```text
audit_event
```

Eventos iniciales:

```text
USER_LOGIN
PLATFORM_CONNECTED
PLATFORM_DISCONNECTED
SYNC_STARTED
SYNC_COMPLETED
SYNC_FAILED
GRADE_CREATED
GRADE_CHANGED
ALERT_CREATED
ALERT_RESOLVED
MESSAGE_GENERATED
MESSAGE_EDITED
MESSAGE_APPROVED
MESSAGE_SENT
POLICY_CREATED
POLICY_CHANGED
```

Las auditorías y comunicaciones no serán eliminadas físicamente mediante operaciones habituales.

---

## 26. Soft delete

No se aplicará indiscriminadamente `deleted_at` a todas las tablas.

Se utilizará eliminación lógica donde aporte valor.

Auditorías y comunicaciones conservarán su historial.

Datos temporales o técnicos podrán eliminarse físicamente cuando corresponda.

---

## 27. Fechas y zonas horarias

La persistencia temporal utilizará UTC.

Cada institución tendrá:

```text
timezone
```

Valor inicial:

```text
America/Guayaquil
```

React presentará las fechas transformadas a la zona correspondiente.

Nunca se utilizará la zona horaria del servidor como fuente implícita de lógica de negocio.

---

## 28. API

La API será REST y estará versionada desde el primer día:

```text
/api/v1
```

Ejemplos:

```text
/api/v1/auth
/api/v1/me
/api/v1/institutions
/api/v1/users
/api/v1/courses
/api/v1/students
/api/v1/grades
/api/v1/alerts
/api/v1/communications
/api/v1/integrations
/api/v1/sync-jobs
/api/v1/admin/policies
```

OpenAPI/Swagger se generará desde Spring.

---

## 29. Errores HTTP

La API utilizará **RFC 9457 — Problem Details for HTTP APIs**.

Esto proporcionará un formato uniforme de errores para React en lugar de inventar estructuras diferentes por endpoint. RFC 9457 define precisamente un modelo interoperable para proporcionar detalles de errores legibles por clientes HTTP. citeturn973432search7

Ejemplo conceptual:

```json
{
  "type": "/problems/invalid-credentials",
  "title": "Credenciales inválidas",
  "status": 401,
  "detail": "El correo o la contraseña no son correctos."
}
```

Las validaciones podrán extender esta estructura con información de campos.

---

## 30. Flyway

Flyway será el único mecanismo autorizado para evolucionar el esquema de producción.

Ejemplo:

```text
V1__create_identity_tables.sql
V2__create_institution_tables.sql
V3__create_academic_tables.sql
V4__create_monitoring_tables.sql
```

Las migraciones versionadas se ejecutan una sola vez y Flyway mantiene su historial y checksum, permitiendo reproducir de forma consistente la estructura entre desarrollo, pruebas y producción. citeturn922860search7turn922860search8

Hibernate no utilizará:

```text
ddl-auto=update
```

en producción.

---

## 31. Seguridad de logs

Nunca se registrarán:

```text
contraseñas
access tokens
refresh tokens
token Idukay
claves criptográficas
cookies de autenticación
```

También se minimizarán:

```text
nombres completos
calificaciones
identificadores personales
contenido de comunicaciones
```

cuando no sean esenciales para diagnóstico.

---

## 32. Request ID

Cada petición dispondrá de un identificador de correlación.

Ejemplo:

```text
X-Request-Id: 93c4...
```

El mismo identificador se incorporará al contexto MDC del backend.

Esto permitirá seguir:

```text
React
→ API
→ SyncJob
→ IdukayAdapter
→ PostgreSQL
→ MonitoringEngine
```

sin registrar información sensible.

Posteriormente podrá evolucionarse hacia OpenTelemetry/tracing distribuido.

---

## 33. Observabilidad

Desde el MVP existirán:

```text
logs estructurados
request ID
Spring Boot Actuator
health checks
```

No se instalarán inicialmente:

```text
Prometheus
Grafana
ELK
```

Estos componentes se evaluarán cuando exista un VPS de producción.

---

## 34. Testing backend

Se utilizarán tres niveles.

```text
JUnit 5
        → lógica aislada

Testcontainers
        → PostgreSQL real en integración

ArchUnit
        → reglas de arquitectura
```

No se utilizará H2 para simular PostgreSQL en las pruebas críticas de persistencia.

---

## 35. Testing frontend

```text
Vitest
+
React Testing Library
+
Playwright E2E
```

Los E2E se incorporarán progresivamente a los flujos críticos:

```text
login
selección de cursos
sincronización
revisión de alerta
generación de mensaje
```

---

## 36. Calidad backend

Inicialmente:

```text
Spotless    → formato
SpotBugs    → análisis estático
```

Checkstyle no será incorporado inicialmente.

Se añadirá únicamente si aparecen reglas de estilo o arquitectura que Spotless y ArchUnit no cubran adecuadamente.

---

## 37. Calidad frontend

```text
ESLint
+
Prettier
```

ESLint se ocupará del análisis del código y Prettier de mantener formato consistente.

---

## 38. Git

Estrategia:

```text
main
  ↑
feature/...
fix/...
refactor/...
```

Se utilizará desarrollo trunk-based con ramas cortas.

No utilizaremos GitFlow.

---

## 39. Conventional Commits

Formato:

```text
feat:
fix:
refactor:
test:
docs:
chore:
ci:
```

Preferentemente con scope:

```text
feat(monitoring): add grade alert policies

fix(sync): prevent duplicate grade imports

feat(idukay): discover teacher courses

test(auth): add refresh token integration tests
```

---

## 40. Pull Requests

Incluso durante el desarrollo individual se utilizarán Pull Requests para cambios relevantes.

Esto permitirá:

```text
ejecutar CI
mantener historial de decisiones
revisar cambios
documentar funcionalidades
practicar flujo profesional
```

---

## 41. GitHub Actions

El repositorio será público.

GitHub establece actualmente que el uso de runners estándar alojados por GitHub es gratuito e ilimitado para repositorios públicos. citeturn922860search0

Desde el inicio, CI verificará:

```text
BACKEND
compile
tests
integration tests
Spotless
SpotBugs

FRONTEND
install
lint
tests
build
```

Más adelante se añadirá construcción y publicación de imágenes Docker.

---

## 42. Desarrollo local

Durante el desarrollo normal:

```text
HOST
├── Spring Boot
└── React

DOCKER
├── PostgreSQL
└── Ollama
```

Esto permitirá recarga rápida de código sin reconstruir constantemente contenedores.

Se documentará también una ejecución completamente dockerizada cuando sea necesaria para pruebas de integración u onboarding.

---

## 43. Producción

En el VPS los componentes estarán separados:

```text
Reverse Proxy
     │
     ├── Frontend
     ├── Backend
     ├── PostgreSQL
     └── Ollama
```

El reverse proxy —Nginx, Caddy o Traefik— se decidirá durante la fase de despliegue.

No se utilizará Kubernetes en el MVP.

---

## 44. Configuración

Spring utilizará perfiles:

```text
dev
test
prod
```

Los secretos se suministrarán mediante variables de entorno o mecanismos externos.

Nunca estarán dentro de:

```text
application.yml
application-prod.yml
GitHub
Dockerfile
README
```

---

## 45. Datos de desarrollo

Existirán dos mecanismos complementarios:

```text
FakeAcademicPlatformAdapter
+
fixtures de desarrollo
```

El fake permitirá desarrollar el sistema completo sin depender de Idukay.

Los fixtures permitirán reproducir escenarios académicos concretos.

Todos los datos serán completamente ficticios.

Nunca se incorporarán datos reales de estudiantes al repositorio público.

---

## 46. Estructura definitiva inicial del repositorio

```text
academic-monitor/
│
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── backend/
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       └── db/
│   │   │           └── migration/
│   │   └── test/
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── infra/
│   └── docker/
│
├── docs/
│   ├── architecture/
│   ├── api/
│   └── adr/
│
├── compose.dev.yml
├── .editorconfig
├── .env.example
├── .gitignore
├── CONTRIBUTING.md
├── SECURITY.md
└── README.md
```

No se creará `LICENSE` inicialmente.

---

## 47. Architecture Decision Records

Las decisiones importantes tendrán ADR.

Inicialmente:

```text
ADR-001 modular-monolith.md

ADR-002 shared-schema-multitenancy.md

ADR-003 academic-platform-port.md

ADR-004 authentication-strategy.md

ADR-005 encrypted-platform-credentials.md

ADR-006 local-llm-first.md

ADR-007 deterministic-alert-engine.md
```

Un ADR explica:

```text
problema
alternativas
decisión
razón
consecuencias
```

---

## 48. README

El README no será una página genérica.

Contendrá:

```text
qué es Academic Monitor
problema que resuelve
estado del proyecto
arquitectura
stack
requisitos
inicio rápido
estructura
seguridad
tests
roadmap
contribución
```

Como repositorio público y proyecto de portafolio, deberá permitir que otro desarrollador comprenda el producto sin leer el código completo.

---

## 49. Seguridad del repositorio público

Será obligatorio excluir:

```text
.env
*.pem
*.key
tokens
cookies
exports Idukay
bases de datos locales
logs
datos académicos reales
```

`SECURITY.md` explicará cómo reportar vulnerabilidades.

`.env.example` contendrá únicamente nombres y valores ficticios.

---

## 50. Alcance del MVP

### Incluido

Sincronización manual, instituciones, usuarios, roles, cursos, estudiantes, actividades, calificaciones, historial, políticas configurables, alertas, página del estudiante, auditoría, Ollama, generación y edición de mensajes, integración Idukay mediante adaptador y soporte opcional de tutoría.

### No incluido inicialmente

Aplicación móvil nativa, pagos, suscripciones, GraphQL, Kubernetes, microservicios, Kafka/RabbitMQ, analítica predictiva mediante IA, generación autónoma de alertas mediante LLM, envío automático sin revisión docente, Moodle, Canvas, aplicación multiidioma completa y monitoreo automático periódico.

La arquitectura no deberá impedir que estos elementos puedan añadirse posteriormente.

---

## 51. Incertidumbres externas conocidas

La respuesta pendiente de Idukay deberá aclarar:

```text
scope del token
token por usuario o institución
endpoint de calificaciones
endpoint de actividades
endpoint de representantes
endpoint de correo interno
expiración/rotación
límites de API
permisos autorizados
```

Ninguna de estas incertidumbres modifica la arquitectura base.

Solo determinará la implementación concreta de:

```text
IdukayAcademicPlatformAdapter
```

y:

```text
IdukayCommunicationAdapter
```

---

## 52. Orden de implementación

La construcción se realizará en este orden:

```text
01. Bootstrap repositorio
02. Infraestructura de desarrollo
03. Backend base
04. Frontend base
05. CI
06. Modelo de identidad
07. Autenticación
08. Multitenencia
09. Modelo académico
10. FakeAcademicPlatformAdapter
11. Sincronización
12. Motor de políticas
13. Alertas
14. Perfil del estudiante
15. Ollama
16. Comunicaciones
17. Auditoría completa
18. IdukayAdapter
19. Tutoría
20. Preparación VPS
```

No se empezará por Idukay.

---

## 53. Primer hito técnico

La primera versión ejecutable deberá conseguir:

```text
PostgreSQL          ✅
Ollama              ✅
Spring Boot         ✅
React               ✅
GitHub Actions      ✅
```

Con:

```text
GET /api/v1/health
```

y una pantalla inicial capaz de confirmar conectividad del frontend con el backend.

El primer hito no incorporará todavía lógica académica.

---

## 54. Criterio de terminado de una funcionalidad

Una funcionalidad se considerará terminada cuando:

```text
implementación completada
tests correspondientes pasan
CI pasa
sin secretos ni PII
formato correcto
documentación actualizada cuando corresponda
migración Flyway cuando exista cambio de BD
API documentada
errores normalizados
auditoría cuando aplique
```

---

## 55. Decisión final

Academic Monitor se construirá como un producto profesional y potencialmente comercializable, pero se evitará deliberadamente diseñar desde el inicio infraestructura correspondiente a una escala que todavía no existe.

La prioridad será:

**mantenibilidad + seguridad + pruebas + desacoplamiento + simplicidad operacional.**

La arquitectura deberá permitir crecer, pero cada abstracción deberá justificar su existencia mediante una necesidad actual o una frontera externa claramente identificada.

**Esta especificación constituye la línea base arquitectónica v1.0 para iniciar el repositorio `academic-monitor`.**