package br.com.fiap.soat7.infra.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebHandler;

class SecurityConfigTest {

    @Test
    void shouldPermitSwaggerAndWebhook_andRequireAuthForOthers() {

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .GET("/v3/api-docs/test", req -> ServerResponse.ok().bodyValue("ok"))
                .GET("/swagger-ui/index.html", req -> ServerResponse.ok().bodyValue("ok"))
                .GET("/api/1/sales/webhook/test", req -> ServerResponse.ok().bodyValue("ok"))
                .GET("/api/1/sales/available", req -> ServerResponse.ok().bodyValue("ok"))
                .build();

        WebHandler webHandler = RouterFunctions.toWebHandler(routes);

        SecurityWebFilterChain chain =
                new SecurityConfig().filterChain(ServerHttpSecurity.http());

        WebTestClient client = WebTestClient
                .bindToWebHandler(webHandler)
                .webFilter(new WebFilterChainProxy(chain)) // aplica segurança como WebFilter
                .build();

        // permitAll
        client.get().uri("/v3/api-docs/test").exchange().expectStatus().isOk();
        client.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
        client.get().uri("/api/1/sales/webhook/test").exchange().expectStatus().isOk();

        // authenticated
        client.get().uri("/api/1/sales/available").exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
