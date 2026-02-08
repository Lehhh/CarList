package br.com.fiap.soat7.infra.config.security;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyUtilsTest {

    @Test
    void shouldParsePublicKeyFromPem() throws Exception {
        // gera chave RSA real
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        RSAPublicKey original = (RSAPublicKey) kp.getPublic();

        // monta PEM a partir do formato X509 (getEncoded já é SubjectPublicKeyInfo)
        String b64 = Base64.getEncoder().encodeToString(original.getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" +
                b64 + "\n" +
                "-----END PUBLIC KEY-----\n";

        RSAPublicKey parsed = RsaKeyUtils.publicKeyFromPem(pem);

        assertNotNull(parsed);
        assertEquals(original.getModulus(), parsed.getModulus());
        assertEquals(original.getPublicExponent(), parsed.getPublicExponent());
    }
}
