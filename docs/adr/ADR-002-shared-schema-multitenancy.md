# ADR-002: Shared schema multitenancy

## Estado

Aceptado.

## Decision

Usar una sola base PostgreSQL con esquema compartido e `institution_id` en entidades institucionales futuras.

## Consecuencias

El MVP evita complejidad de multiples bases o esquemas por institucion y deja abierta la posibilidad de incorporar Row Level Security mas adelante.
