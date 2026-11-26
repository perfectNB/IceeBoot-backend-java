package com.iceeboot.common.service.pay.client;

import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

class OfficialNotificationTest {
    @TempDir Path directory;

    private KeyPair keys() throws Exception {
        var factory = KeyPairGenerator.getInstance("RSA");
        factory.initialize(2048);
        return factory.generateKeyPair();
    }

    @Test
    void alipayValidatesSignatureMerchantAndExactAmount() throws Exception {
        KeyPair platform = keys();
        String privateKey = Base64.getEncoder().encodeToString(platform.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(platform.getPublic().getEncoded());
        var client = new AlipayQrClient("fixture-app", "fixture-seller", privateKey, publicKey, "https://example.invalid/notify");
        Map<String, String> fields = new TreeMap<>(Map.of("app_id", "fixture-app", "seller_id", "fixture-seller",
                "trade_status", "TRADE_SUCCESS", "out_trade_no", "order-1", "trade_no", "trade-1", "total_amount", "12.01"));
        String signature = AlipaySignature.rsaSign(AlipaySignature.getSignContent(fields), privateKey, "UTF-8", "RSA2");
        var request = new MockHttpServletRequest();
        fields.forEach(request::setParameter);
        request.setParameter("sign_type", "RSA2");
        request.setParameter("sign", signature);
        assertThat(client.verify(request)).isEqualTo(new VerifiedPayment("order-1", "trade-1", 1201));
        request.setParameter("total_amount", "13.01");
        assertThatThrownBy(() -> client.verify(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wechatVerifiesTheOriginalBodyAndDecryptsThePayment() throws Exception {
        var platform = keys();
        var merchant = keys();
        Path privateFile = directory.resolve("merchant.pem");
        Path publicFile = directory.resolve("platform.pem");
        Files.writeString(privateFile, pem("PRIVATE KEY", merchant.getPrivate().getEncoded()));
        Files.writeString(publicFile, pem("PUBLIC KEY", platform.getPublic().getEncoded()));
        String secret = "12345678123456781234567812345678";
        String publicKeyId = "PUB_KEY_ID_1";
        var client = new WechatNativeClient("app", "mch", "https://example.invalid/notify",
                privateFile.toString(), "1234", publicFile.toString(), publicKeyId, secret);
        var json = new ObjectMapper();
        var transaction = Map.of("appid", "app", "mchid", "mch", "out_trade_no", "order-1",
                "transaction_id", "txn", "trade_state", "SUCCESS", "amount", Map.of("currency", "CNY", "total", 1201));
        String nonce = "123456789012";
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.getBytes(UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(UTF_8)));
        cipher.updateAAD("transaction".getBytes(UTF_8));
        String ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(json.writeValueAsBytes(transaction)));
        String body = json.writeValueAsString(Map.of("event_type", "TRANSACTION.SUCCESS", "resource_type", "encrypt-resource",
                "resource", Map.of("algorithm", "AEAD_AES_256_GCM", "nonce", nonce, "associated_data", "transaction",
                        "ciphertext", ciphertext, "original_type", "transaction")));
        String time = String.valueOf(Instant.now().getEpochSecond());
        var signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(platform.getPrivate());
        signature.update((time + "\n" + nonce + "\n" + body + "\n").getBytes(UTF_8));
        var request = new MockHttpServletRequest();
        request.addHeader("Wechatpay-Serial", publicKeyId);
        request.addHeader("Wechatpay-Timestamp", time);
        request.addHeader("Wechatpay-Nonce", nonce);
        request.addHeader("Wechatpay-Signature", Base64.getEncoder().encodeToString(signature.sign()));
        assertThat(client.verify(request, body)).isEqualTo(new VerifiedPayment("order-1", "txn", 1201));
        assertThatThrownBy(() -> client.verify(request, body + " ")).isInstanceOf(RuntimeException.class);
    }

    private String pem(String type, byte[] bytes) {
        return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
                + "\n-----END " + type + "-----\n";
    }
}
