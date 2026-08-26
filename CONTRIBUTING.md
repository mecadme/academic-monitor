# Contributing

Gracias por contribuir a Academic Monitor.

## Flujo de trabajo

1. Crea una rama corta desde `main`.
2. Mantén cada cambio enfocado.
3. Ejecuta las validaciones locales antes de abrir un pull request.
4. Usa Conventional Commits.
5. No subas secretos ni datos reales.

## Convenciones de commits

Ejemplos:

```text
chore: bootstrap academic monitor platform
feat(identity): add institution memberships
fix(sync): prevent duplicate grade imports
test(auth): add refresh token integration tests
```

## Alcance actual

El bootstrap inicial solo cubre infraestructura base. Autenticacion, Idukay, alertas y logica academica se implementaran en hitos posteriores.
