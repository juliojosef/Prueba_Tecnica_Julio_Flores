USE AccountsDB;
GO

-- Consultar y crear cuentas; actualizar saldo y version.
GRANT SELECT, INSERT, UPDATE
ON OBJECT::dbo.cuenta
TO accounts_app;
GO

-- Consultar y registrar movimientos.
GRANT SELECT, INSERT
ON OBJECT::dbo.movimiento
TO accounts_app;
GO

-- Ejecutar la consulta del historial.
GRANT EXECUTE
ON OBJECT::dbo.usp_ConsultarMovimientos
TO accounts_app;
GO