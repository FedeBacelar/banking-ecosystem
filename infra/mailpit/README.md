# Mailpit local

Mailpit captura los correos del ecosistema durante el desarrollo. No entrega
mensajes a Internet y no debe desplegarse como proveedor SMTP real.

## Iniciar

Desde la raíz del repositorio:

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

Esperar hasta que el contenedor esté saludable:

```powershell
docker compose -f infra/mailpit/docker-compose.yml ps
```

La bandeja local queda disponible en:

```txt
http://localhost:8025
```

El servidor SMTP escucha en:

```txt
localhost:1025
```

No requiere usuario, contraseña ni STARTTLS. `notification-service` usa esos
valores como defaults locales.

## Detener

```powershell
docker compose -f infra/mailpit/docker-compose.yml down
```

Los mensajes quedan en el volumen `mailpit_data`. Para eliminarlos junto con el
contenedor:

```powershell
docker compose -f infra/mailpit/docker-compose.yml down -v
```

## Puertos opcionales

Si los puertos locales estan ocupados, copiar `.env.example` como `.env`,
ajustar los valores y ejecutar:

```powershell
docker compose --env-file infra/mailpit/.env -f infra/mailpit/docker-compose.yml up -d
```

Los puertos se publican sólo en `127.0.0.1`. La interfaz no tiene autenticación
porque esta infraestructura es exclusivamente local.

Si cambia el puerto SMTP, establecer el mismo valor en
`NOTIFICATION_SMTP_PORT` al iniciar `notification-service`.

## SMTP real

Los entornos desplegados deben definir explícitamente host, puerto,
credenciales, autenticación, STARTTLS y remitente mediante variables de
entorno o un gestor de secretos. Nunca deben reutilizar Mailpit ni agregar
credenciales reales a los archivos del repositorio.
