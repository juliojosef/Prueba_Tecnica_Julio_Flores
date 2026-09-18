USE AccountsDB;
GO

CREATE OR ALTER PROCEDURE dbo.usp_ConsultarMovimientos
    @CuentaId UNIQUEIDENTIFIER,
    @Desde DATETIME2(6),
    @Hasta DATETIME2(6),
    @Pagina INT = 1,
    @TamanoPagina INT = 20
AS
BEGIN
    SET NOCOUNT ON;

    -- Validar los parametros.
    IF @CuentaId IS NULL
        THROW 50001, 'Debes indicar la cuenta.', 1;

    IF @Desde IS NULL OR @Hasta IS NULL OR @Desde >= @Hasta
        THROW 50002, 'El rango de fechas es invalido.', 1;

    IF @Pagina IS NULL OR @Pagina < 1
        THROW 50003, 'La pagina debe ser mayor o igual a 1.', 1;

    IF @TamanoPagina IS NULL
       OR @TamanoPagina < 1
       OR @TamanoPagina > 100
        THROW 50004, 'El tamano de pagina debe estar entre 1 y 100.', 1;

    IF NOT EXISTS
    (
        SELECT 1
        FROM dbo.cuenta
        WHERE id = @CuentaId
    )
        THROW 50005, 'La cuenta no existe.', 1;

    DECLARE @Desplazamiento BIGINT =
        (CONVERT(BIGINT, @Pagina) - 1) * @TamanoPagina;

    -- Calcular el saldo incluyendo el historial anterior a @Desde.
    ;WITH Historial AS
    (
        SELECT
            m.id,
            m.cuenta_id,
            m.fecha,
            m.tipo,
            m.monto,
            m.saldo_resultante,
            CAST
            (
                c.saldo_inicial
                + CAST
                (
                    SUM
                    (
                        CASE
                            WHEN m.tipo = 'CREDIT' THEN m.monto
                            ELSE -m.monto
                        END
                    ) OVER
                    (
                        ORDER BY m.fecha, m.id
                        ROWS BETWEEN UNBOUNDED PRECEDING
                             AND CURRENT ROW
                    )
                    AS DECIMAL(19, 2)
                )
                AS DECIMAL(19, 2)
            ) AS saldo_acumulado
        FROM dbo.movimiento AS m
        INNER JOIN dbo.cuenta AS c
            ON c.id = m.cuenta_id
        WHERE m.cuenta_id = @CuentaId
          AND m.fecha < @Hasta
    )
    SELECT
        id,
        cuenta_id,
        fecha,
        tipo,
        monto,
        saldo_resultante,
        saldo_acumulado
    FROM Historial
    WHERE fecha >= @Desde
    ORDER BY fecha, id
    OFFSET @Desplazamiento ROWS
    FETCH NEXT @TamanoPagina ROWS ONLY;
END;
GO