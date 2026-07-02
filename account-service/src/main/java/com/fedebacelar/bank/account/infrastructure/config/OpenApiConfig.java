package com.fedebacelar.bank.account.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "keycloak-oauth2";

    @Bean
    OpenAPI accountServiceOpenApi(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8090/realms/banking-ecosystem}") String issuerUri
    ) {
        String openIdConnectBaseUrl = issuerUri + "/protocol/openid-connect";

        return new OpenAPI()
                .info(new Info()
                        .title("account-service API")
                        .version("0.0.1")
                        .description("Account service API for banking accounts, account identifiers, and account lifecycle."))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, oauth2Scheme(openIdConnectBaseUrl)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private SecurityScheme oauth2Scheme(String openIdConnectBaseUrl) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl(openIdConnectBaseUrl + "/auth")
                                .tokenUrl(openIdConnectBaseUrl + "/token")
                                .scopes(new Scopes()
                                        .addString("openid", "OpenID Connect authentication")
                                        .addString("profile", "User profile claims"))));
    }
}
