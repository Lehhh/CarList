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
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaPubKeyLoader {

    @Value("${JWT_PUBLIC_KEY}") // ✅ correto
    private String publicKeyValue;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() throws Exception {
        RSAPublicKey publicKey = loadPublicKeyFromBase64Env(publicKeyValue);
        return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
    }


    public static RSAPublicKey loadPublicKeyFromBase64Env(String base64Env) throws Exception {
        String pem = new String(Base64.getDecoder().decode(base64Env));
        return loadPublicKeyX509FromPem(pem);
    }

    private static RSAPublicKey loadPublicKeyX509FromPem(String pem) throws Exception {
        String content = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(spec);
        return (RSAPublicKey) pub;
    }
}
