package br.com.fiap.soat7.infra.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaPubKeyLoader {

    @Value("${JWT_PUBLIC_KEY}") // ✅ correto
    private String publicKeyValue;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() throws Exception {
        RSAPublicKey publicKey = resolvePublicKey(publicKeyValue);
        return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
    }

    static RSAPublicKey resolvePublicKey(String value) throws Exception {
        String trimmed = value == null ? "" : value.trim();

        // se vier PEM direto (GitHub secret/env), usa como conteúdo
        if (trimmed.startsWith("-----BEGIN")) {
            return loadPublicKeyFromPem(trimmed);
        }

        // senão assume path
        return loadPublicKeyFromPath(Path.of(trimmed));
    }

    public static RSAPublicKey loadPublicKeyFromPath(Path pemPath) throws Exception {
        String pem = Files.readString(pemPath, StandardCharsets.UTF_8);
        return loadPublicKeyFromPem(pem);
    }

    public static RSAPublicKey loadPublicKeyFromPem(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] der = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
