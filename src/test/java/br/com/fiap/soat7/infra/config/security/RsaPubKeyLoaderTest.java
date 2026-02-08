package br.com.fiap.soat7.infra.config.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaPubKeyLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadPublicKeyFromPemFile() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        RSAPublicKey original = (RSAPublicKey) kp.getPublic();

        String b64 = Base64.getEncoder().encodeToString(original.getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" +
                b64 + "\n" +
                "-----END PUBLIC KEY-----\n";

        Path pemPath = tempDir.resolve("pub.pem");
        Files.writeString(pemPath, pem);

        RSAPublicKey loaded = RsaPubKeyLoader.loadPublicKey(pemPath);

        assertNotNull(loaded);
        assertEquals(original.getModulus(), loaded.getModulus());
        assertEquals(original.getPublicExponent(), loaded.getPublicExponent());
    }
}
