package br.com.fiap.soat7.infra.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaPubKeyLoaderTest {

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private static String toPemPublic(RSAPublicKey pub) {
        String base64 = Base64.getEncoder().encodeToString(pub.getEncoded());
        // quebra em 64 chars só pra ficar PEM “bonitinho”
        return "-----BEGIN PUBLIC KEY-----\n"
                + base64.replaceAll("(.{64})", "$1\n")
                + "\n-----END PUBLIC KEY-----\n";
    }

    @Test
    void loadPublicKeyFromBase64Env_shouldLoadRsaPublicKey() throws Exception {
        KeyPair kp = generateRsaKeyPair();
        String pem = toPemPublic((RSAPublicKey) kp.getPublic());

        String pemB64 = Base64.getEncoder()
                .encodeToString(pem.getBytes(StandardCharsets.UTF_8));

        RSAPublicKey loaded = RsaPubKeyLoader.loadPublicKeyFromBase64Env(pemB64);

        assertNotNull(loaded);
        assertEquals("RSA", loaded.getAlgorithm());
        assertEquals(((RSAPublicKey) kp.getPublic()).getModulus(), loaded.getModulus());
        assertEquals(((RSAPublicKey) kp.getPublic()).getPublicExponent(), loaded.getPublicExponent());
    }

    @Test
    void loadPublicKeyFromBase64Env_shouldThrowWhenBase64IsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                RsaPubKeyLoader.loadPublicKeyFromBase64Env("###not-base64###")
        );
    }

    @Test
    void loadPublicKeyFromBase64Env_shouldThrowWhenPemIsNotAValidPublicKey() {
        String pemFake = "-----BEGIN PUBLIC KEY-----\nabc\n-----END PUBLIC KEY-----\n";
        String pemFakeB64 = Base64.getEncoder()
                .encodeToString(pemFake.getBytes(StandardCharsets.UTF_8));

        assertThrows(Exception.class, () ->
                RsaPubKeyLoader.loadPublicKeyFromBase64Env(pemFakeB64)
        );
    }

    @Test
    void reactiveJwtDecoder_shouldBeCreatedFromEnvValue() throws Exception {
        KeyPair kp = generateRsaKeyPair();
        String pem = toPemPublic((RSAPublicKey) kp.getPublic());

        String pemB64 = Base64.getEncoder()
                .encodeToString(pem.getBytes(StandardCharsets.UTF_8));

        RsaPubKeyLoader cfg = new RsaPubKeyLoader();

        // injeta o @Value private via reflection
        Field f = RsaPubKeyLoader.class.getDeclaredField("publicKeyValue");
        f.setAccessible(true);
        f.set(cfg, pemB64);

        ReactiveJwtDecoder decoder = cfg.reactiveJwtDecoder();

        assertNotNull(decoder);
        assertTrue(decoder.getClass().getName().contains("NimbusReactiveJwtDecoder"));
    }
}
