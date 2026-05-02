package cn.org.alan.exam.utils;

import java.security.SecureRandom;

/**
 * 使用 {@link SecureRandom} 从字母数字池抽样，生成邀请码/证书校验码等不可预测字符串。
 *
 * @author Alan
 */
public class ClassTokenGenerator {

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 生成指定长度的随机口令（均匀取自 {@link #CHAR_POOL}）。
     *
     * @param length 字符个数，须为正数
     * @return 随机口令
     */
    public static String generateClassToken(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder tokenBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            tokenBuilder.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return tokenBuilder.toString();
    }
}