package com.julio.accounts.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountsOpenAPI() {
        return new OpenAPI().info(
            new Info()
                .title("API de cuentas y movimientos")
                .version("1.0.0")
                .description(
                    "Prueba tecnica Java Senior de Julio Flores. "
                    + "Permite crear cuentas, consultar saldos "
                    + "y registrar creditos y debitos "
                    + "con idempotencia por cuenta."
                )
        );
    }
}