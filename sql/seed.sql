USE AccountsDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @CuentaId UNIQUEIDENTIFIER =
    '11111111-1111-1111-1111-111111111111';

BEGIN TRY
    BEGIN TRANSACTION;

    -- Cargar el ejemplo solamente si la cuenta no existe.
    IF NOT EXISTS
    (
        SELECT 1
        FROM dbo.cuenta WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CuentaId
    )
    BEGIN
        INSERT INTO dbo.cuenta
            (id, saldo_inicial, saldo, version)
        VALUES
            (@CuentaId, 100.00, 170.00, 3);

        INSERT INTO dbo.movimiento
        (
            cuenta_id,
            tipo,
            monto,
            saldo_resultante,
            clave_idempotencia,
            fecha
        )
        VALUES
        (
            @CuentaId,
            'CREDIT',
            50.00,
            150.00,
            'seed-credito-001',
            '2026-01-10T10:00:00'
        ),
        (
            @CuentaId,
            'DEBIT',
            30.00,
            120.00,
            'seed-debito-001',
            '2026-01-11T10:00:00'
        ),
        (
            @CuentaId,
            'CREDIT',
            50.00,
            170.00,
            'seed-credito-002',
            '2026-01-12T10:00:00'
        );

        PRINT 'Cuenta y movimientos de ejemplo creados.';
    END
    ELSE
    BEGIN
        PRINT 'La cuenta de ejemplo ya existe. No se modificaron sus datos.';
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;

-- Consultar la cuenta de ejemplo.
SELECT id, saldo_inicial, saldo, version
FROM dbo.cuenta
WHERE id = @CuentaId;

-- Consultar sus movimientos en orden cronologico.
SELECT id, tipo, monto, saldo_resultante,
       clave_idempotencia, fecha
FROM dbo.movimiento
WHERE cuenta_id = @CuentaId
ORDER BY fecha, id;