# ADR-006: Local LLM first

## Estado

Aceptado.

## Decision

El primer proveedor de IA sera Ollama, con configuracion mediante `OLLAMA_BASE_URL`.

## Consecuencias

El bootstrap levanta Ollama, pero no descarga modelos ni implementa generacion de mensajes todavia.
