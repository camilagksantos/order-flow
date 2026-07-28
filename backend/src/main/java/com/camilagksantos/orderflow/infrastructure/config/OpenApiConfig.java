package com.camilagksantos.orderflow.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("order-flow API")
                        .description("E-commerce REST API with hexagonal architecture and event-driven order processing")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Camila Kfouri")
                                .url("https://www.linkedin.com/in/camila-kfouri/")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}