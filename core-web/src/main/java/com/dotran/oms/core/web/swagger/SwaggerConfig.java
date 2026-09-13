package com.dotran.oms.core.web.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerConfig {

    private final SwaggerProperties properties;

    @Bean
    public OpenAPI openAPI() {

        Info info = new Info()
                .title(properties.getTitle())
                .description(properties.getDescription())
                .version(properties.getVersion());

        if (properties.getContactName() != null) {
            info.contact(new Contact()
                    .name(properties.getContactName()));
        }

        if (properties.getLicenseName() != null) {
            info.license(new License()
                    .name(properties.getLicenseName()));
        }

        OpenAPI openAPI = new OpenAPI()
                .info(info);

        List<Server> servers = properties.getServers()
                .stream()
                .map(server -> new Server()
                        .url(server.getUrl())
                        .description(server.getDescription()))
                .toList();

        if (!servers.isEmpty()) {
            openAPI.servers(servers);
        }

        return openAPI;
    }

    @Bean
    public List<GroupedOpenApi> groupedOpenApis() {

        return properties.getGroups()
                .stream()
                .map(group -> GroupedOpenApi.builder()
                        .group(group.getName())
                        .packagesToScan(
                                group.getPackagesToScan()
                                        .toArray(new String[0])
                        )
                        .build())
                .toList();
    }
}
