package com.iceeboot.agent.security;

import com.iceeboot.agent.config.AgentProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

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
    if (value.startsWith(PREFIX)) throw new IllegalArgumentException("不能直接提交密文");
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
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("敏感配置加密失败", e);
    }
  }

  public String decrypt(String value) {
    if (value == null || value.isEmpty()) return "";
    if (!value.startsWith(PREFIX)) throw new IllegalStateException("敏感配置不是有效密文");
    try {
      byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, payload, 0, 12));
      return new String(cipher.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("敏感配置解密失败，请检查加密主密钥", e);
    }
  }

  private SecretKeySpec key() {
    try {
      byte[] key = Base64.getDecoder().decode(properties.getEncryptionKey());
      if (key.length != 32) throw new IllegalArgumentException();
      return new SecretKeySpec(key, "AES");
    } catch (Exception e) {
      throw new IllegalArgumentException("请配置 AI_CONFIG_ENCRYPTION_KEY：Base64 编码的 32 字节主密钥");
    }
  }

  public String mask(String value) {
    return value == null || value.isBlank() ? "" : MASK;
  }
}
