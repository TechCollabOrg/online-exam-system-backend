package cn.org.alan.exam.utils;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES/CBC/NoPadding + Base64 编解码的简化封装；密钥与 IV 写死在类内（适合与遗留客户端对齐，生产建议改为配置中心）。
 *
 * @author Alan
 */
@Component
public class SecretUtils {
    /** 16 字节 AES 密钥（与 IV 相同长度要求）。 */
    private static String KEY = "63eeac68cf074c8c";

    /** CBC 模式初始化向量。 */
    private static String IV = "63eeac68cf074c8c";

    /**
     * 使用内置 KEY/IV 加密明文，明文长度会被填充到块大小的整数倍。
     *
     * @param data 原始字符串
     * @return Base64 密文；异常时可能返回 {@code null}
     */
    public static String encrypt(String data) {
        return encrypt(data, KEY, IV);
    }

    /**
     * 使用内置 KEY/IV 解密密文（方法名 {@code desEncrypt} 为历史命名，实为 AES 解密）。
     *
     * @param data Base64 密文
     * @return 明文；异常时可能返回 {@code null}
     */
    public static String desEncrypt(String data) {
        return desEncrypt(data, KEY, IV);
    }

    /**
     * 加密方法
     *
     * @param data 要加密的数据
     * @param key  加密key
     * @param iv   加密iv
     * @return 加密的结果
     */
    private static String encrypt(String data, String key, String iv) {
        try {
            //"算法/模式/补码方式"NoPadding PkcsPadding
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            int blockSize = cipher.getBlockSize();

            byte[] dataBytes = data.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);

//            return new Base64().encodeToString(encrypted);
            return Base64.encodeBase64String(encrypted);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密方法
     *
     * @param data 要解密的数据
     * @param key  解密key
     * @param iv   解密iv
     * @return 解密的结果
     */
    private static String desEncrypt(String data, String key, String iv) {
        try {
            byte[] encrypted1 = new Base64().decode(data);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] original = cipher.doFinal(encrypted1);
            return new String(original).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}

