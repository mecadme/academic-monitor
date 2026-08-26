# ADR-001: Modular monolith

## Estado

Aceptado.

## Problema

Academic Monitor necesita crecer por dominios funcionales sin asumir desde el inicio la complejidad operacional de microservicios.

## Alternativas

- Monolito simple sin limites internos.
- Microservicios desde el inicio.
- Monolito modular con fronteras explicitas.

## Decision

Usar un monolito modular con package-by-feature y principios hexagonales pragmaticos.

## Razon

Permite avanzar rapido, mantener despliegue simple y conservar fronteras reemplazables para integraciones externas como Idukay y Ollama.

## Consecuencias

El backend mantendra dependencias dirigidas hacia dominio y casos de uso. ArchUnit se usara progresivamente para cuidar estos limites.
