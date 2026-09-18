\# Servicio de cuentas y movimientos



Microservicio desarrollado con Java 17 y Spring Boot para crear cuentas,

consultar saldos y registrar créditos y débitos con validación externa,

persistencia e idempotencia.



\## Tecnologías



\- Java 17 y Maven.

\- Spring Boot 3.x.

\- Spring Web, Bean Validation y Spring Data JPA.

\- SQL Server y H2.

\- Resilience4j: retry y circuit breaker.

\- WireMock para simular el servicio de validación.

\- Spring Boot Actuator y springdoc/OpenAPI.



\## Requisitos



\- JDK 17.

\- Maven 3.9.x.

\- Puertos 8081 y 8082 disponibles.

\- SQL Server únicamente si se utiliza el perfil `sqlserver`.



Los comandos siguientes están preparados para Windows PowerShell.

Se ejecutan desde la raíz del repositorio.



```powershell

java -version

mvn -version

```



\## Compilar y ejecutar pruebas



```powershell

mvn clean verify

```



Para ejecutar únicamente las pruebas:



```powershell

mvn test

```



Se verificaron 37 pruebas aprobadas, sin fallos, errores ni pruebas omitidas.

Las pruebas automatizadas de persistencia utilizan H2; la integración real

con SQL Server se verificó por separado de forma manual.



\## Ejecutar la validación externa



El stub se encuentra en `stub/mappings`.



Descargar WireMock si todavía no está disponible:



```powershell

New-Item -ItemType Directory -Force .\\tools | Out-Null



Invoke-WebRequest `

&#x20; -Uri "https://repo.maven.apache.org/maven2/org/wiremock/wiremock-standalone/3.13.2/wiremock-standalone-3.13.2.jar" `

&#x20; -OutFile ".\\tools\\wiremock-standalone-3.13.2.jar"

```



Ejecutar en una terminal independiente:



```powershell

java -jar .\\tools\\wiremock-standalone-3.13.2.jar `

&#x20; --port 8082 `

&#x20; --bind-address 127.0.0.1 `

&#x20; --root-dir .\\stub

```



Mantener esta terminal abierta mientras se realizan operaciones nuevas.

El archivo JAR es una herramienta descargable y no forma parte del código fuente.



\## Ejecutar con H2



En otra terminal:



```powershell

mvn spring-boot:run "-Dspring-boot.run.profiles=h2"

```



La aplicación escucha en `http://localhost:8081`.



H2 permite ejecutar la solución sin instalar SQL Server.

La base de datos es en memoria: sus datos se pierden al detener la aplicación.

Este perfil es para desarrollo y pruebas.



\## Ejecutar con SQL Server



\### 1. Preparar la base de datos



Desde SQL Server Management Studio, ejecutar en este orden:



1\. `sql/00-create-database.sql`

2\. `sql/schema.sql`

3\. `sql/procedure.sql`

4\. `sql/seed.sql`



La base de datos se llama `AccountsDB`.

Los scripts crean las tablas `dbo.cuenta` y `dbo.movimiento`,

el procedimiento `dbo.usp\_ConsultarMovimientos` y datos ficticios.



Los scripts de creación comprueban la existencia de sus objetos;

no deben utilizarse para actualizar automáticamente esquemas antiguos.



\### 2. Preparar el usuario de aplicación



Crear el login SQL `accounts\_app` y mapearlo como usuario de `AccountsDB`.

Elegir su contraseña localmente, sin incorporarla a los scripts ni al repositorio.



Ejecutar `sql/permissions.sql` con una identidad administrativa.

El usuario de aplicación recibe permisos específicos sobre las tablas

y el procedimiento; no necesita `sysadmin` ni `db\_owner`.



Para autenticación SQL, la instancia debe permitir autenticación mixta.

Para la URL del ejemplo, TCP/IP debe estar habilitado y escuchar en el puerto 1433.



Comprobar conectividad:



```powershell

Test-NetConnection -ComputerName 127.0.0.1 -Port 1433

```



\### 3. Configurar y arrancar



En la terminal donde se ejecutará la aplicación:



```powershell

$env:DB\_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=AccountsDB;encrypt=true;trustServerCertificate=true"

$env:DB\_USERNAME = "accounts\_app"



$sqlCredential = Get-Credential `

&#x20; -UserName "accounts\_app" `

&#x20; -Message "Introduce la contraseña de accounts\_app"



$env:DB\_PASSWORD = $sqlCredential.GetNetworkCredential().Password

$env:VALIDATION\_BASE\_URL = "http://localhost:8082"



mvn spring-boot:run "-Dspring-boot.run.profiles=sqlserver"

```



La configuración se encuentra en

`src/main/resources/application-sqlserver.properties`.



Hibernate valida el esquema existente; no crea ni modifica las tablas.

Las fechas se guardan y recuperan en UTC utilizando `DATETIME2(6)`.



`trustServerCertificate=true` se utiliza únicamente para la prueba local.

En producción se debe validar el certificado del servidor.



\## Configuración por variables de entorno



| Variable | Uso |

|---|---|

| `DB\_URL` | URL JDBC requerida para el perfil SQL Server |

| `DB\_USERNAME` | Usuario de la base de datos |

| `DB\_PASSWORD` | Contraseña inyectada al proceso |

| `VALIDATION\_BASE\_URL` | URL del tercero; por defecto `http://localhost:8082` |

| `VALIDATION\_TIMEOUT\_MS` | Timeout de la validación; por defecto 800 ms |



No se incluyen contraseñas ni tokens en el repositorio.

Cada ambiente debe utilizar credenciales propias y permisos mínimos.



\## API y documentación



\- Swagger UI: `http://localhost:8081/swagger-ui.html`

\- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

\- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`

\- Especificación exportada: `docs/openapi.yaml`

\- Health check: `http://localhost:8081/actuator/health`



| Método | Ruta | Resultado |

|---|---|---|

| POST | `/accounts` | Crea una cuenta |

| GET | `/accounts/{id}` | Consulta la cuenta y su saldo |

| POST | `/accounts/{id}/transactions` | Registra o recupera un movimiento idempotente |

| GET | `/actuator/health` | Consulta el estado de salud |



\## Ejemplos de uso



Ejecutar en una tercera terminal PowerShell.



\### Crear una cuenta



```powershell

$baseUrl = "http://localhost:8081"



$account = Invoke-RestMethod `

&#x20; -Uri "$baseUrl/accounts" `

&#x20; -Method Post `

&#x20; -ContentType "application/json" `

&#x20; -Body '{"initialBalance":100.00}'



$account

$accountUrl = "$baseUrl/accounts/$($account.id)"

$transactionsUrl = "$accountUrl/transactions"

```



Resultado esperado: HTTP 201 y una cuenta con saldo inicial y actual de 100.00.

El identificador se genera automáticamente.



\### Registrar un crédito



```powershell

$response = Invoke-WebRequest `

&#x20; -Uri $transactionsUrl `

&#x20; -Method Post `

&#x20; -ContentType "application/json" `

&#x20; -Headers @{"Idempotency-Key"="demo-credito-001"} `

&#x20; -Body '{"type":"CREDIT","amount":50.00}' `

&#x20; -UseBasicParsing



$response.StatusCode

$response.Content

```



Resultado esperado: HTTP 201, movimiento CREDIT por 50.00

y `resultingBalance` de 150.00.



\### Repetir el crédito



Ejecutar nuevamente el mismo comando, con la misma cuenta, clave y cuerpo.



```powershell

$response.StatusCode

$response.Headers\["Idempotency-Replayed"]

Invoke-RestMethod -Uri $accountUrl -Method Get

```



Resultado esperado:



\- HTTP 200.

\- Cabecera `Idempotency-Replayed: true`.

\- Mismo identificador y contenido del movimiento original.

\- Saldo de 150.00, sin un segundo crédito.



Reutilizar la clave con un tipo o importe distinto responde HTTP 409.



\### Registrar un débito



```powershell

Invoke-RestMethod `

&#x20; -Uri $transactionsUrl `

&#x20; -Method Post `

&#x20; -ContentType "application/json" `

&#x20; -Headers @{"Idempotency-Key"="demo-debito-001"} `

&#x20; -Body '{"type":"DEBIT","amount":30.00}'

```



Resultado esperado: HTTP 201 y saldo resultante de 120.00.



\### Consultar el saldo



```powershell

Invoke-RestMethod -Uri $accountUrl -Method Get

```



Resultado esperado después de las operaciones anteriores: saldo 120.00.



\### Probar saldo insuficiente



Con WireMock disponible:



```powershell

try {

&#x20;   Invoke-RestMethod `

&#x20;     -Uri $transactionsUrl `

&#x20;     -Method Post `

&#x20;     -ContentType "application/json" `

&#x20;     -Headers @{"Idempotency-Key"="demo-insuficiente-001"} `

&#x20;     -Body '{"type":"DEBIT","amount":999.00}'

}

catch {

&#x20;   \[int]$\_.Exception.Response.StatusCode

&#x20;   $\_.ErrorDetails.Message

}

```



Resultado esperado: HTTP 409; el saldo permanece en 120.00.



\### Probar entrada inválida



Con WireMock disponible:



```powershell

try {

&#x20;   Invoke-RestMethod `

&#x20;     -Uri $transactionsUrl `

&#x20;     -Method Post `

&#x20;     -ContentType "application/json" `

&#x20;     -Headers @{"Idempotency-Key"="demo-invalido-001"} `

&#x20;     -Body '{"type":"CREDIT","amount":0}'

}

catch {

&#x20;   \[int]$\_.Exception.Response.StatusCode

}

```



Resultado esperado: HTTP 400.



\### Probar cuenta inexistente



```powershell

$missingId = \[guid]::NewGuid().ToString()



try {

&#x20;   Invoke-RestMethod -Uri "$baseUrl/accounts/$missingId" -Method Get

}

catch {

&#x20;   \[int]$\_.Exception.Response.StatusCode

}

```



Resultado esperado: HTTP 404.



\## Resiliencia y tercero caído



Antes de confirmar una operación nueva se llama a `POST /validate`.

La integración está aislada en un adaptador, fuera del controller.



Se utilizan:



\- Timeout configurable.

\- Hasta dos intentos totales para fallas transitorias.

\- Espera de 150 ms entre intentos.

\- Circuit breaker con ventana de 10 llamadas, mínimo de 4 llamadas

&#x20; y umbral de fallas del 50 %.

\- Periodo abierto de 10 segundos y dos llamadas de prueba en estado half-open.

\- Fallback que rechaza de forma controlada con HTTP 503.



No se reintentan rechazos de negocio ni respuestas inválidas.

Una validación rechazada responde HTTP 409.



\### Verificar indisponibilidad



1\. Detener WireMock con `Ctrl + C`.

2\. Enviar un movimiento nuevo con una clave nueva:



```powershell

try {

&#x20;   Invoke-RestMethod `

&#x20;     -Uri $transactionsUrl `

&#x20;     -Method Post `

&#x20;     -ContentType "application/json" `

&#x20;     -Headers @{"Idempotency-Key"="demo-tercero-caido-001"} `

&#x20;     -Body '{"type":"DEBIT","amount":10.00}'

}

catch {

&#x20;   \[int]$\_.Exception.Response.StatusCode

&#x20;   $\_.ErrorDetails.Message

}



Invoke-RestMethod -Uri $accountUrl -Method Get

```



Resultado esperado: HTTP 503 y saldo sin cambios.

No se confirma una operación cuando no puede obtenerse la validación obligatoria.



Repetir solicitudes nuevas mientras el tercero permanece caído permite

alcanzar el umbral del circuit breaker. Las llamadas posteriores se rechazan

sin esperar una nueva conexión mientras el circuito está abierto.



Para comprobar recuperación, iniciar WireMock y esperar el periodo abierto

antes de reintentar.



Un movimiento ya confirmado puede recuperarse con la misma clave y cuerpo,

incluso si el tercero está caído: se devuelve el registro previo sin crear otro.



\## Estado de cuenta en SQL Server



Ejecutar `sql/verify.sql` o utilizar:



```sql

USE AccountsDB;

GO



EXEC dbo.usp\_ConsultarMovimientos

&#x20;   @CuentaId = '11111111-1111-1111-1111-111111111111',

&#x20;   @Desde = '2026-01-11T00:00:00',

&#x20;   @Hasta = '2026-01-13T00:00:00',

&#x20;   @Pagina = 1,

&#x20;   @TamanoPagina = 20;

```



Para los datos de prueba devuelve los movimientos del 11 y 12 de enero,

con saldos acumulados de 120.00 y 170.00.



El inicio del rango es inclusivo y el final exclusivo.

El cálculo considera el saldo inicial y los movimientos anteriores al rango.

La paginación utiliza orden estable por fecha e identificador.



\### Decisiones de modelado y optimización



\- Importes `DECIMAL(19,2)` y aritmética Java con `BigDecimal`.

\- Llaves primarias, llave foránea y restricciones sobre saldos, importes y tipos.

\- Restricción única por cuenta y clave de idempotencia.

\- Collation binaria en tipo y clave para respetar comparaciones sensibles a mayúsculas.

\- Índice por cuenta, fecha e identificador, con columnas incluidas.

\- Filtros de fecha sin funciones sobre la columna.

\- Paginación limitada a un máximo de 100 registros por página.



Con historiales extensos, mediría el plan real y las lecturas:

el saldo acumulado exige considerar historia previa y las páginas profundas

con OFFSET pueden resultar costosas. Evaluaría saldos de apertura

precalculados y paginación por cursor según la necesidad del negocio.



Las comprobaciones manuales se documentan en

`docs/sqlserver-verification.md`.



\## Decisiones de arquitectura



El ejercicio mantiene cuentas y movimientos en un mismo servicio para

actualizar saldo y registrar movimiento en una sola transacción local.



Se separan controllers, DTOs, servicios, repositorios y adaptador externo.

Los errores se traducen mediante un manejador centralizado.



Para operaciones nuevas, la validación externa se ejecuta antes de adquirir

el bloqueo de escritura en la BD. Después se bloquea la cuenta y se vuelve

a comprobar la clave de idempotencia dentro de la transacción.



Esto reduce el tiempo de retención del bloqueo y evita duplicar movimientos

ante solicitudes concurrentes. La restricción única protege también en la BD.



No se utiliza double para dinero ni se confirma un débito que deje saldo negativo.



\## Cobertura y límites de las pruebas



Las 37 pruebas cubren:



\- Reglas monetarias y saldo insuficiente.

\- Creación y consulta de cuentas.

\- Entradas inválidas y cuentas inexistentes.

\- Créditos y débitos.

\- Repetición idempotente y conflicto de payload.

\- Concurrencia sobre operaciones de débito.

\- Tercero indisponible, rechazo, timeout, retry y circuit breaker.

\- Recuperación de movimientos previos sin una nueva validación externa.



Los endpoints se prueban con Spring Boot y MockMvc.

El cliente externo se prueba con WireMock.



La ejecución automatizada utiliza H2 para ser reproducible sin SQL Server.

No sustituye las pruebas específicas de bloqueo y comportamiento bajo carga

en SQL Server. Estas serían una ampliación con Testcontainers y pruebas de carga.



\## Verificaciones realizadas



\- 37 pruebas automatizadas aprobadas.

\- Arranque de la aplicación con SQL Server y validación del esquema.

\- Lectura de la cuenta de prueba desde la API.

\- Persistencia de un crédito y su saldo resultante en SQL Server.

\- Repetición del crédito: HTTP 200, cabecera de replay y saldo sin cambios.

\- Consulta SQL que confirma un solo movimiento para la cuenta y clave.

\- Procedimiento con rango de fechas, saldo acumulado y paginación.

\- Rechazo controlado ante tercero caído, sin modificación del saldo.



\## Empaquetado Docker — pendiente



La construcción y ejecución con Docker todavía no se han validado.

En el equipo inicial Docker Desktop no pudo iniciar su motor.



El empaquetado pendiente requiere un Dockerfile multi-stage,

Java 17 para ejecución y configuración por variables de entorno.

Una vez incorporados los archivos, deben verificarse `docker build`

y el arranque del contenedor antes de considerar este punto completo.



\## Alcance y mejoras futuras



La solución se enfoca en los criterios funcionales del ejercicio.

Antes de un uso productivo incorporaría autenticación y autorización,

gestión centralizada de secretos, certificados válidos, pruebas con SQL Server

automatizadas y pruebas de carga.



El bonus de observabilidad completa queda pendiente:

logs estructurados, correlation-id propagado, métricas y runbook.



Las respuestas de teoría y revisión del fragmento Java se encuentran

en `RESPUESTAS.md`.

