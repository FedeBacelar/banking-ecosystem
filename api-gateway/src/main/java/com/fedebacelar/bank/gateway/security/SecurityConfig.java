package com.fedebacelar.bank.gateway.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/web/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/customers/**").hasRole("CUSTOMER_READ")
                        .pathMatchers(HttpMethod.POST, "/api/customers/**").hasRole("CUSTOMER_WRITE")
                        .pathMatchers(HttpMethod.PATCH, "/api/customers/**").hasRole("CUSTOMER_WRITE")
                        .pathMatchers(HttpMethod.GET, "/api/accounts/**").hasRole("ACCOUNT_READ")
                        .pathMatchers(HttpMethod.POST, "/api/accounts/**").hasRole("ACCOUNT_WRITE")
                        .pathMatchers(HttpMethod.PATCH, "/api/accounts/**").hasRole("ACCOUNT_WRITE")
                        .pathMatchers("/api/customers/**", "/api/accounts/**").denyAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRealmRoleConverter()))
                )
                .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakRealmRoleConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, extractRealmAuthorities(jwt)));
    }

    private Collection<GrantedAuthority> extractRealmAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> roleValues)) {
            return List.of();
        }

        return roleValues.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

}
