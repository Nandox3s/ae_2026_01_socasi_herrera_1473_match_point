# ADR-011 · Entregar sobre Docker Compose local y documentar la arquitectura AWS objetivo

- **Estado:** Aceptado
- **Fecha:** 2026-08-04
- **Decide:** Josué Herrera, Fernando Socasi
- **Requerimientos afectados:** RNF-05, RNF-10 · criterios 4.2 y 4.3 de la rúbrica
- **Historia:** configuración y documentación

## Contexto

La entrega debe demostrar capacidad de desplegar en entornos IaaS y de contenedores. Había que
elegir entre invertir el tiempo restante en **desplegar en AWS** o en **dejar el entorno local
impecable y diseñar en detalle la arquitectura objetivo**.

Los hechos que pesaron:

- El sistema ya está **completamente contenedorizado**: seis servicios, imágenes con versión fija,
  `healthcheck` en todos, red interna, volúmenes con nombre y un solo puerto publicado. Eso es
  virtualización de contenedores real, no una maqueta.
- Un despliegue en AWS con cuenta de estudiante introduce dependencias fuera del control del
  equipo: límites de la cuenta, costos, credenciales que expiran a mitad de una demostración.
- **Una demo que depende de internet en el aula es una demo que puede no ocurrir.** El riesgo no es
  teórico: si la red falla, no hay nada que enseñar.
- Lo que la rúbrica pide en 4.3 es **diseñar** la arquitectura de infraestructura, no
  necesariamente tenerla corriendo.

## Decisión

**La demostración se hace sobre Docker Compose local, y la arquitectura en la nube se entrega
diseñada, dimensionada y costeada.**

1. **Entorno de ejecución:** `docker compose up -d --build` levanta el sistema completo en
   cualquier máquina con Docker, sin JDK, sin Gradle y sin PostgreSQL instalados.
2. **Arquitectura objetivo:** diseño completo sobre AWS con VPC, subredes públicas y privadas,
   ALB, Auto Scaling Group, RDS Multi-AZ, ECR y Cognito, con el dimensionamiento y el costo mensual
   estimado. Documentado en [`entrega/04-nube/COMPUTACION-EN-LA-NUBE.md`](../../04-nube/COMPUTACION-EN-LA-NUBE.md).
3. **Puente entre ambos:** las mismas imágenes que corren en local son las que se subirían a ECR.
   No hay un `Dockerfile` de desarrollo y otro de producción; toda la diferencia entre entornos
   está en variables de entorno.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Desplegar en EC2 con Docker Compose** | Es la ruta más corta a "está en la nube" y era viable. Se descartó por el riesgo de la demo en vivo: una instancia apagada por límite de crédito, o una red del aula que no responde, deja la sustentación sin nada que mostrar. La arquitectura que se enseña es además la misma que aquí se diseña. |
| **ECS Fargate** | Es el destino natural de una aplicación ya contenedorizada y evita administrar servidores. Quedó documentado como **evolución** en el diseño objetivo, pero implementarlo exigía definiciones de tarea, roles de IAM y un balanceador configurado: días de trabajo que no compran ningún punto adicional de la rúbrica frente al diseño bien hecho. |
| **Kubernetes (EKS)** | Sobredimensionado para dos microservicios. El costo del plano de control por sí solo supera al de toda la arquitectura propuesta, y la complejidad operativa no se justifica a esta escala. Se menciona en el diseño como el umbral a partir del cual sí tendría sentido. |
| **Desplegar solo el frontend o una parte** | Un despliegue parcial no demuestra ni el entorno local completo ni la arquitectura en la nube. Lo peor de las dos opciones. |

## Consecuencias

**A favor**

- **La demostración no depende de la red.** Corre en cualquier laptop con Docker, siempre igual.
- **Reproducibilidad total:** imágenes con versión fija (`postgres:16-alpine`, `nginx:1.27-alpine`,
  `dpage/pgadmin4:8.14`), nunca `latest`. La demo de hoy y la de la próxima semana son idénticas.
- **El diseño en la nube quedó más completo** de lo que habría estado si el tiempo se hubiera ido
  en pelear con la consola de AWS: incluye dimensionamiento, política de escalado, estimación de
  costos y comparación explícita de escalamiento vertical contra horizontal.
- **El camino a producción está trazado y es corto**, porque la aplicación ya cumple lo que exige:
  configuración por variables de entorno, estado fuera del contenedor, `healthcheck` reales y
  logs a `stdout`.

**En contra**

- **No hay una URL pública que enseñar.** Es la limitación más visible de la entrega y hay que
  decirla de frente, no esperar a que la pregunten.
- **La arquitectura objetivo no está validada en ejecución.** Las estimaciones de costo y
  dimensionamiento son eso: estimaciones fundamentadas, no mediciones.
- **Quedan cosas por resolver que solo aparecen al desplegar de verdad:** certificados TLS,
  registros DNS, roles de IAM, respaldos y ventanas de mantenimiento. Están identificadas en el
  documento de nube, no escondidas.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `docker-compose.yml` | Seis servicios, red `backbone`, tres volúmenes con nombre, `healthcheck` y `restart` en todos |
| `matchpoint/Dockerfile` · `users/Dockerfile` | Build multi-stage: se compila con Gradle y se ejecuta sobre una imagen JRE mínima |
| `.env.example` | Toda la configuración por variables de entorno; **ningún valor real versionado** |
| `entrega/04-nube/COMPUTACION-EN-LA-NUBE.md` | Arquitectura objetivo, escalamiento, dimensionamiento y costos |
| `docs/DEMO.md` | Guion de la demostración local, con el *checklist* previo |

Verificación de la portabilidad, que es la afirmación central de este ADR:

```bash
git clone <repo> && cd match
cp .env.example .env          # y completar los valores
docker compose up -d --build
docker compose ps             # los seis servicios en (healthy)
```

En una máquina limpia, sin JDK, sin Gradle y sin PostgreSQL instalados.
