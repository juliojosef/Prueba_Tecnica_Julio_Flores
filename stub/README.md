\# Simulador de validacion externa



WireMock simula el servicio externo requerido por la prueba tecnica.



\## Preparacion



Desde la raiz del repositorio, ejecutar en PowerShell:



```powershell

New-Item -ItemType Directory -Force -Path .\\tools | Out-Null



Invoke-WebRequest `

&#x20;   -Uri "https://repo.maven.apache.org/maven2/org/wiremock/wiremock-standalone/3.13.2/wiremock-standalone-3.13.2.jar" `

&#x20;   -OutFile .\\tools\\wiremock-standalone-3.13.2.jar `

&#x20;   -UseBasicParsing

```



\## Ejecucion



Con Java 17 disponible, ejecutar desde la raiz del repositorio:



```powershell

java -jar .\\tools\\wiremock-standalone-3.13.2.jar --port 8082 --bind-address 127.0.0.1 --root-dir .\\stub

```



\## Endpoint



POST http://localhost:8082/validate



Solicitud de ejemplo:



```json

{

&#x20; "accountId": "eb305365-4754-4e41-ade1-2664cb10f7a3",

&#x20; "type": "CREDIT",

&#x20; "amount": 50.00

}

```



Respuesta configurada:



```json

{

&#x20; "approved": true,

&#x20; "reason": "APPROVED"

}

```



La regla inicial aprueba cualquier POST a /validate.

El simulador no consulta la base de datos de cuentas.



Para detenerlo, presionar Ctrl + C en su terminal.

