package com.iceeboot.framework.service.agent.security;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 密钥、MCP 请求头与沙箱环境变量加密保存，列表接口只返回占位符。 */
@Component
public class AgentSecrets {
    public static final String MASK = "********";
    private static final String PREFIX = "enc:v1:";
    private static final String VERSIONED_PREFIX = "enc:v2:";
    private final AgentProperties properties;

    public AgentSecrets(AgentProperties properties) {
        this.properties = properties;
    }

    public String merge(String value, String previous) {
        if (value == null || MASK.equals(value)) {
            if (previous == null
                    || previous.isEmpty()
                    || previous.startsWith(
                            VERSIONED_PREFIX + properties.getEncryptionKeyId() + ":"))
                return previous;
            // 轮换期间读取到的旧值也必须以当前版本写回，避免并发编辑把旧密文重新带回数据库。
            return reencrypt(previous);
        }
        if (value.isBlank()) return "";
        if (value.startsWith("enc:"))
            throw new IceeBootException(
                    ResponseMessage.Agent.SECRET_CIPHERTEXT_SUBMISSION_FORBIDDEN);
        return encrypt(value);
    }

    public String encrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            validateActiveKey();
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key(properties.getEncryptionKeyId()),
                    new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return VERSIONED_PREFIX
                    + properties.getEncryptionKeyId()
                    + ":"
                    + Base64.getEncoder().encodeToString(payload);
        } catch (IceeBootException e) {
            throw e;
        } catch (Exception e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.SECRET_ENCRYPTION_FAILED,
                    e);
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            String keyId;
            String encoded;
            if (value.startsWith(PREFIX)) {
                keyId = "legacy";
                encoded = value.substring(PREFIX.length());
            } else if (value.startsWith(VERSIONED_PREFIX)) {
                int separator = value.indexOf(':', VERSIONED_PREFIX.length());
                if (separator < 0)
                    throw new IceeBootException(
                            ResultCodeEnum.ERROR.getCode(),
                            ResponseMessage.Agent.SECRET_CIPHERTEXT_INVALID);
                keyId = value.substring(VERSIONED_PREFIX.length(), separator);
                encoded = value.substring(separator + 1);
            } else
                throw new IceeBootException(
                        ResultCodeEnum.ERROR.getCode(),
                        ResponseMessage.Agent.SECRET_CIPHERTEXT_INVALID);
            byte[] payload = Base64.getDecoder().decode(encoded);
            if (payload.length < 28)
                throw new IceeBootException(
                        ResultCodeEnum.ERROR.getCode(),
                        ResponseMessage.Agent.SECRET_CIPHERTEXT_INVALID);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(keyId), new GCMParameterSpec(128, payload, 0, 12));
            return new String(
                    cipher.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8);
        } catch (IceeBootException e) {
            throw e;
        } catch (Exception e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.SECRET_DECRYPTION_FAILED,
                    e);
        }
    }

    @jakarta.annotation.PostConstruct
    public void validateConfiguredKeys() {
        if (properties.getEncryptionKey() != null && !properties.getEncryptionKey().isBlank())
            validateActiveKey();
        for (var entry : properties.getDecryptionKeys().entrySet())
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                if (entry.getKey().equals(properties.getEncryptionKeyId())
                        && !entry.getValue().equals(properties.getEncryptionKey()))
                    throw new IceeBootException(ResponseMessage.Agent.SECRET_KEY_ID_INVALID);
                key(entry.getKey());
            }
    }

    public void validateActiveKey() {
        if ("legacy".equals(properties.getEncryptionKeyId()))
            throw new IceeBootException(ResponseMessage.Agent.SECRET_KEY_ID_INVALID);
        key(properties.getEncryptionKeyId());
    }

    public String reencrypt(String value) {
        return value == null || value.isEmpty() ? value : encrypt(decrypt(value));
    }

    private SecretKeySpec key(String keyId) {
        try {
            if (keyId == null || !keyId.matches("[a-zA-Z0-9_-]{1,40}"))
                throw new IceeBootException(
                        ResultCodeEnum.ERROR.getCode(),
                        ResponseMessage.Agent.SECRET_KEY_ID_INVALID);
            String encoded;
            if (keyId.equals(properties.getEncryptionKeyId()))
                encoded = properties.getEncryptionKey();
            else {
                encoded = properties.getDecryptionKeys().get(keyId);
                if ((encoded == null || encoded.isBlank()) && "legacy".equals(keyId))
                    encoded = properties.getEncryptionKey();
                if (encoded == null || encoded.isBlank())
                    throw new IceeBootException(
                            ResultCodeEnum.ERROR.getCode(),
                            ResponseMessage.Agent.SECRET_KEY_VERSION_MISSING);
            }
            byte[] key = Base64.getDecoder().decode(encoded);
            if (key.length != 32)
                throw new IceeBootException(
                        ResultCodeEnum.ERROR.getCode(),
                        ResponseMessage.Agent.SECRET_MASTER_KEY_INVALID);
            return new SecretKeySpec(key, "AES");
        } catch (IceeBootException e) {
            throw e;
        } catch (Exception e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.SECRET_MASTER_KEY_INVALID,
                    e);
        }
    }

    public String mask(String value) {
        return value == null || value.isBlank() ? "" : MASK;
    }
}
