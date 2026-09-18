\# Verificación con SQL Server



La aplicación se ejecutó con Java 17 y el perfil sqlserver,

conectada a AccountsDB mediante el usuario accounts\_app.



Las credenciales se proporcionaron mediante variables de entorno.



\## Comprobaciones realizadas



\- Consulta de la cuenta creada por seed.sql:

&#x20; saldo inicial 100.00 y saldo actual 170.00.

\- Creación de una cuenta nueva desde la API con saldo 100.00.

\- Registro de un crédito de 50.00:

&#x20; respuesta HTTP 201 y saldo resultante 150.00.

\- Consulta desde SSMS:

&#x20; cuenta y movimiento guardados en SQL Server.

\- Repetición del crédito con la misma Idempotency-Key:

&#x20; respuesta HTTP 200 e Idempotency-Replayed: true.

\- El saldo permaneció en 150.00.

\- SQL confirmó un solo movimiento para esa cuenta y clave.



\## Pruebas automatizadas



La ejecución de Maven reportó 37 pruebas sin fallos ni errores.

Las pruebas de persistencia automatizadas utilizan H2.

Las comprobaciones con SQL Server descritas aquí fueron manuales.

