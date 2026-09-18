USE master;
GO

IF DB_ID(N'AccountsDB') IS NULL
BEGIN
    CREATE DATABASE AccountsDB;
END;
GO

USE AccountsDB;
GO

SELECT DB_NAME() AS base_actual;