# Academic Monitor

Academic Monitor es una plataforma web para seguimiento academico docente. El proyecto busca sincronizar informacion de plataformas educativas, detectar situaciones que requieren atencion mediante reglas deterministas y ayudar a preparar comunicaciones revisadas por docentes con apoyo de IA local.

Este repositorio esta en el primer hito tecnico. Todavia no implementa autenticacion, Idukay, alertas ni logica academica.

## Estado del proyecto

Bootstrap inicial:

- Backend Spring Boot 4.1.x con Java 21 y Maven.
- Frontend React 19 + TypeScript + Vite 8 + pnpm.
- PostgreSQL 18.
- Ollama.
- Docker Compose.
- Endpoint `GET /api/v1/health`.
- Pantalla inicial de estado.
- GitHub Actions para backend, frontend y compose.

## Arquitectura

La base sigue un monolito modular con principios hexagonales pragmaticos. El dominio futuro no debera depender de Idukay, Ollama, React, PostgreSQL, HTTP ni Docker.

Namespace Java raiz:

```text
io.academicmonitor
```

Estructura inicial:

```text
academic-monitor/
├── .github/workflows/ci.yml
├── backend/
├── frontend/
├── infra/docker/
├── docs/
│   ├── architecture/
│   ├── api/
│   └── adr/
├── compose.dev.yml
├── compose.yml
├── .env.example
├── .gitignore
├── CONTRIBUTING.md
├── SECURITY.md
└── README.md
```

## Inicio rapido

Copia la plantilla de variables locales:

```bash
cp .env.example .env
```

Levanta todo el sistema:

```bash
docker compose up --build
```

Abre la pantalla inicial:

```text
http://localhost:3000
```

Endpoint del backend:

```text
http://localhost:8080/api/v1/health
```

## Servicios

| Servicio | Puerto | Descripcion |
| --- | ---: | --- |
| frontend | 3000 | Interfaz React servida por Nginx |
| backend | 8080 | API Spring Boot |
| postgres | 5432 | Base de datos local |
| ollama | 11434 | Servicio local de IA |

## Desarrollo local

Modo recomendado durante desarrollo normal:

```text
HOST
├── Spring Boot
└── React

DOCKER
├── PostgreSQL
└── Ollama
```

Tambien existe modo completamente dockerizado:

```bash
docker compose -f compose.dev.yml up --build
```

Backend local:

```bash
cd backend
mvn spring-boot:run
```

Frontend local:

```bash
cd frontend
pnpm install
pnpm dev
```

## Validaciones

Backend:

```bash
cd backend
mvn test
mvn spotless:check
mvn spotbugs:check
```

Frontend:

```bash
cd frontend
pnpm install
pnpm lint
pnpm test
pnpm build
```

## Seguridad

El repositorio es publico. No se deben versionar secretos, tokens, cookies, respaldos, exports de Idukay, logs ni datos reales de estudiantes, docentes o instituciones.

Usa `.env` para configuracion local y conserva solo `.env.example` en git.

## Roadmap inmediato

1. Bootstrap repositorio.
2. Infraestructura de desarrollo.
3. Backend base.
4. Frontend base.
5. CI.
6. Modelo de identidad.
7. Autenticacion.
8. Multitenencia.
9. Modelo academico.
10. FakeAcademicPlatformAdapter.

## Licencia

Sin licencia inicialmente.
