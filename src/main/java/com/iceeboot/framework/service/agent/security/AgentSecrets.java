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
    private final AgentProperties properties;

    public AgentSecrets(AgentProperties properties) {
        this.properties = properties;
    }

    public String merge(String value, String previous) {
        if (value == null || MASK.equals(value)) return previous;
        if (value.isBlank()) return "";
        if (value.startsWith(PREFIX))
            throw new IceeBootException(
                    ResponseMessage.Agent.SECRET_CIPHERTEXT_SUBMISSION_FORBIDDEN);
        return encrypt(value);
    }

    public String encrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
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
        if (!value.startsWith(PREFIX))
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.SECRET_CIPHERTEXT_INVALID);
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, payload, 0, 12));
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

    private SecretKeySpec key() {
        try {
            byte[] key = Base64.getDecoder().decode(properties.getEncryptionKey());
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
