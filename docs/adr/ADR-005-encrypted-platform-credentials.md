# ADR-005: Encrypted platform credentials

## Estado

Aceptado para implementacion posterior.

## Decision

Las credenciales externas se almacenaran cifradas y no podran visualizarse nuevamente desde la aplicacion.

## Consecuencias

No se versionan secretos ni datos reales. La clave de cifrado permanecera fuera de PostgreSQL y fuera del repositorio.
