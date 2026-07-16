# Observabilidad local de Nerva

Este stack permite inspeccionar métricas, logs y trazas del onboarding durante
el desarrollo. Usa una única imagen LGTM, pero conserva cada señal en su
backend correspondiente: Prometheus, Loki y Tempo. Grafana es la interfaz de
consulta y OpenTelemetry Collector recibe la telemetría de los servicios.

Antes de persistir trazas o logs, el Collector elimina atributos semánticos
conocidos de URL, red, identidad, credenciales y excepciones. Conserva el tipo
de error, el contexto de la traza y los atributos operativos seguros. Esta
defensa complementa la regla principal: los servicios nunca deben escribir
datos personales, tokens ni enlaces sensibles en el cuerpo de un log.

No es una configuración para producción. Sólo publica Grafana y el receptor
OTLP en `127.0.0.1`; sus credenciales son exclusivamente locales.

La imagen trae Pyroscope embebido y ejecuta su proceso interno, pero Nerva no
configura un pipeline, datasource ni puerto de profiling. Por eso no lo usa y
queda fuera del alcance de esta etapa.

## Requisitos

- Docker Engine con Docker Compose v2.
- Hasta 4 GiB de memoria y 2 CPU disponibles para el contenedor.
- Los servicios Nerva iniciados en sus puertos habituales con el perfil
  `observability`.

`host.docker.internal` permite que Prometheus consulte procesos ejecutados en
Windows. La entrada `host-gateway` del Compose ofrece el mismo nombre en Linux.

## Iniciar

Desde la raíz del repositorio:

```powershell
docker compose -f infra/observability/docker-compose.yml up -d
docker compose -f infra/observability/docker-compose.yml ps
```

El primer arranque puede tardar alrededor de un minuto. Esperá hasta que
`banking-observability` figure como `healthy`.

Grafana queda disponible en <http://localhost:3000>:

```text
Usuario: admin
Contraseña: nerva-local
```

Son valores de desarrollo. Para cambiarlos, copiá `.env.example` como `.env`,
modificá las variables y usá:

```powershell
docker compose --env-file infra/observability/.env -f infra/observability/docker-compose.yml up -d
```

Grafana aplica las credenciales al crear su base por primera vez. Si el volumen
ya existe, cambiar el archivo `.env` no reemplaza la contraseña almacenada.

## Conectar los servicios

Los servicios deben iniciar con el perfil `observability` y exportar OTLP HTTP
a este endpoint:

```text
http://localhost:4318
```

En cada terminal de API Gateway, BFF, Onboarding y Notification, iniciá el
proceso con el perfil activo:

```powershell
$env:SPRING_PROFILES_ACTIVE = "observability"
$env:OTEL_EXPORTER_OTLP_ENDPOINT = "http://localhost:4318"
.\mvnw.cmd spring-boot:run
```

Las variables afectan sólo a ese proceso y su terminal. Config Server, Eureka
y el resto de los servicios continúan iniciándose con su perfil habitual.

Prometheus consulta cada 15 segundos:

| Servicio | Endpoint consultado desde el contenedor |
|---|---|
| API Gateway | `host.docker.internal:8085/actuator/prometheus` |
| Home Banking BFF | `host.docker.internal:8086/web/actuator/prometheus` |
| Onboarding | `host.docker.internal:8087/actuator/prometheus` |
| Notification | `host.docker.internal:8083/actuator/prometheus` |

Estos endpoints operativos están pensados sólo para procesos locales o una red
privada. El Gateway impide acceder a ellos a través de `/web/actuator/**`; no
deben exponerse directamente a Internet.

El dashboard provisionado aparece en **Dashboards → Nerva → Nerva ·
Onboarding**. Sus dos secciones son **Estado del ecosistema** y **Recorrido del
cliente**. Al principio algunos paneles estarán vacíos hasta que los servicios
generen tráfico.

## Diagnóstico

Ver el estado y los logs del stack:

```powershell
docker compose -f infra/observability/docker-compose.yml ps
docker compose -f infra/observability/docker-compose.yml logs -f observability
```

Consultar los targets de Prometheus sin exponer su puerto al host:

```powershell
docker exec banking-observability curl -s http://127.0.0.1:9090/api/v1/targets
```

Si un target figura `down`, confirmá que el servicio esté ejecutándose, que
exponga `/actuator/prometheus` con el perfil `observability` y que use el puerto
indicado en la tabla.

## Retención y apagado

- Prometheus conserva hasta 7 días o 1 GB, lo que ocurra primero.
- Loki y Tempo conservan 3 días.
- Todas las señales se guardan en el volumen `observability_data`.

Detener el stack sin perder datos:

```powershell
docker compose -f infra/observability/docker-compose.yml down
```

Eliminar únicamente la telemetría local:

```powershell
docker compose -f infra/observability/docker-compose.yml down -v
```

Este último comando no elimina los volúmenes de MySQL, Keycloak, MinIO o
Mailpit, porque pertenecen a otros proyectos de Compose.
