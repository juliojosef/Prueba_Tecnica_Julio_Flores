USE AccountsDB;
GO

EXEC dbo.usp_ConsultarMovimientos
    @CuentaId = '11111111-1111-1111-1111-111111111111',
    @Desde = '2026-01-11T00:00:00',
    @Hasta = '2026-01-13T00:00:00',
    @Pagina = 1,
    @TamanoPagina = 20;


EXEC dbo.usp_ConsultarMovimientos
    @CuentaId = '11111111-1111-1111-1111-111111111111',
    @Desde = '2026-01-10T00:00:00',
    @Hasta = '2026-01-13T00:00:00',
    @Pagina = 2,
    @TamanoPagina = 2;