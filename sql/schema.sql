USE AccountsDB;
GO

IF OBJECT_ID(N'dbo.cuenta', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cuenta
    (
        id UNIQUEIDENTIFIER NOT NULL,

        saldo_inicial DECIMAL(19, 2) NOT NULL,

        saldo DECIMAL(19, 2) NOT NULL,

        version BIGINT NOT NULL
            CONSTRAINT df_cuenta_version DEFAULT (0),

        CONSTRAINT pk_cuenta
            PRIMARY KEY (id),

        CONSTRAINT ck_cuenta_saldo_inicial
            CHECK (saldo_inicial >= 0),

        CONSTRAINT ck_cuenta_saldo
            CHECK (saldo >= 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.movimiento', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.movimiento
    (
        id BIGINT IDENTITY(1, 1) NOT NULL,

        cuenta_id UNIQUEIDENTIFIER NOT NULL,

        tipo VARCHAR(10)
            COLLATE Latin1_General_100_BIN2 NOT NULL,

        monto DECIMAL(19, 2) NOT NULL,

        saldo_resultante DECIMAL(19, 2) NOT NULL,

        clave_idempotencia VARCHAR(100)
            COLLATE Latin1_General_100_BIN2 NOT NULL,

        -- Fecha almacenada en UTC.
        fecha DATETIME2(6) NOT NULL,

        CONSTRAINT pk_movimiento
            PRIMARY KEY (id),

        CONSTRAINT fk_movimiento_cuenta
            FOREIGN KEY (cuenta_id)
            REFERENCES dbo.cuenta (id),

        CONSTRAINT ck_movimiento_tipo
            CHECK (tipo IN ('CREDIT', 'DEBIT')),

        CONSTRAINT ck_movimiento_monto
            CHECK (monto > 0),

        CONSTRAINT ck_movimiento_saldo
            CHECK (saldo_resultante >= 0),

        CONSTRAINT uk_movimiento_cuenta_clave
            UNIQUE (cuenta_id, clave_idempotencia)
    );
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.movimiento')
      AND name = N'ix_movimiento_cuenta_fecha'
)
BEGIN
    CREATE INDEX ix_movimiento_cuenta_fecha
        ON dbo.movimiento (cuenta_id, fecha, id)
        INCLUDE (tipo, monto, saldo_resultante);
END;
GO