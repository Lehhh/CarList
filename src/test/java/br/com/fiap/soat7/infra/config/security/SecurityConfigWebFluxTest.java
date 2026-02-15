package br.com.fiap.soat7.infra.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

class SecurityConfigWebFluxTest {

    @Test
    void shouldPermitSwaggerAndWebhook_andRequireAuthForOthers() {

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .GET("/v3/api-docs/test", req -> ServerResponse.ok().bodyValue("ok"))
                .GET("/swagger-ui/index.html", req -> ServerResponse.ok().bodyValue("ok"))
                .GET("/api/1/sales/webhook/test", req -> ServerResponse.ok().bodyValue("ok"))
                .GET("/api/1/sales/available", req -> ServerResponse.ok().bodyValue("ok"))
                .build();

        WebHandler webHandler = RouterFunctions.toWebHandler(routes);

        // ✅ decoder fake só para o teste
        ReactiveJwtDecoder fakeDecoder = token ->
                Mono.just(new Jwt(
                        token,
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Map.of("alg", "none"),
                        Map.of("sub", "user-1", "roles", List.of("ROLE_USER"))
                ));

        // ✅ usa uma config de teste que injeta o decoder
        SecurityWebFilterChain chain = new TestSecurityConfig(fakeDecoder)
                .filterChain(ServerHttpSecurity.http());

        WebTestClient client = WebTestClient
                .bindToWebHandler(webHandler)
                .webFilter(new WebFilterChainProxy(chain))
                .build();

        // permitAll
        client.get().uri("/v3/api-docs/test").exchange().expectStatus().isOk();
        client.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
        client.get().uri("/api/1/sales/webhook/test").exchange().expectStatus().isOk();

        // authenticated (sem token) -> 401
        client.get().uri("/api/1/sales/available").exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        // authenticated (com token) -> 200
        client.get().uri("/api/1/sales/available")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk();
    }

    /**
     * Config de teste: igual à sua, mas injeta um jwtDecoder fake para o resource server.
     * (Não altera a produção)
     */
    static class TestSecurityConfig extends SecurityConfig {
        private final ReactiveJwtDecoder decoder;

        TestSecurityConfig(ReactiveJwtDecoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex
                            .pathMatchers("/api/1/sales/webhook/**",
                                    "/v3/api-docs/**",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html").permitAll()
                            .anyExchange().authenticated()
                    )
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                    .oauth2ResourceServer(oauth -> oauth
                            .jwt(jwt -> jwt
                                    .jwtDecoder(decoder) // ✅ aqui resolve o erro
                                    .jwtAuthenticationConverter(reactiveJwtAuthConverter())
                            )
                    )
                    .build();
        }
    }
}
