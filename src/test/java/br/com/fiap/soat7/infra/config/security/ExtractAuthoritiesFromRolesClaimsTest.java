package br.com.fiap.soat7.infra.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ExtractAuthoritiesFromRolesClaimsTest {

        private SecurityConfig target;
        private Method method;

        @BeforeEach
        void setup() throws Exception {
            target = new SecurityConfig(); // sua classe real

            method = SecurityConfig.class.getDeclaredMethod(
                    "extractAuthoritiesFromRolesClaims",
                    Jwt.class
            );

            method.setAccessible(true);
        }

        @SuppressWarnings("unchecked")
        private Collection<GrantedAuthority> invoke(Jwt jwt) throws Exception {
            return (Collection<GrantedAuthority>) method.invoke(target, jwt);
        }

        private Jwt jwt(Map<String, Object> claims) {
            return new Jwt(
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"),
                    claims
            );
        }

        @Test
        void deveUsarClaimRole_quandoRolesNull() throws Exception {
            Jwt jwt = jwt(Map.of("role", "admin"));

            var result = invoke(jwt);

            assertEquals(1, result.size());
            assertEquals("ROLE_admin", result.iterator().next().getAuthority());
        }

        @Test
        void deveIgnorarRoleBlank() throws Exception {
            Jwt jwt = jwt(Map.of("role", "   "));

            var result = invoke(jwt);

            assertTrue(result.isEmpty());
        }

        @Test
        void deveConverterListaRoles() throws Exception {
            Jwt jwt = jwt(Map.of("roles", List.of("admin", "user")));

            var result = invoke(jwt).stream().map(GrantedAuthority::getAuthority).toList();

            assertEquals(List.of("ROLE_admin", "ROLE_user"), result);
        }

        @Test
        void deveFazerTrim_filtrarBlank_eAdicionarPrefixo() throws Exception {
            Jwt jwt = jwt(Map.of(
                    "roles", List.of(" admin ", " ", "", "manager")
            ));

            var result = invoke(jwt).stream().map(GrantedAuthority::getAuthority).toList();

            assertEquals(List.of("ROLE_admin", "ROLE_manager"), result);
        }

        @Test
        void deveRespeitarRoleJaComPrefixo() throws Exception {
            Jwt jwt = jwt(Map.of(
                    "roles", List.of("ROLE_ADMIN")
            ));

            var result = invoke(jwt).stream().map(GrantedAuthority::getAuthority).toList();

            assertEquals(List.of("ROLE_ADMIN"), result);
        }

        @Test
        void deveRemoverDuplicados() throws Exception {
            Jwt jwt = jwt(Map.of(
                    "roles", List.of("admin", "admin", "ROLE_admin")
            ));

            var result = invoke(jwt).stream().map(GrantedAuthority::getAuthority).toList();

            // distinct()
            assertEquals(1, result.size());
            assertEquals("ROLE_admin", result.get(0));
        }

        @Test
        void deveIgnorarClaimRole_seRolesNaoVazio() throws Exception {
            Jwt jwt = jwt(Map.of(
                    "roles", List.of("user"),
                    "role", "admin"
            ));

            var result = invoke(jwt).stream().map(GrantedAuthority::getAuthority).toList();

            // roles tem prioridade
            assertEquals(List.of("ROLE_user"), result);
        }
    }

