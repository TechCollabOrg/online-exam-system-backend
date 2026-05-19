package cn.org.alan.exam.utils;

import cn.org.alan.exam.model.vo.score.ScoreBriefingRowVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 与前端成绩详情页一致的五档（E～A）分段统计。
 */
public final class ScoreBriefingStatsUtil {

    private static final int BELOW_PASS_BUCKETS = 2;
    private static final int ABOVE_PASS_BUCKETS = 3;
    private static final String[] GRADE_LETTERS = {"E", "D", "C", "B", "A"};
    private static final String[] GRADE_TIERS = {"不及格", "不及格", "及格", "良好", "优良"};

    private ScoreBriefingStatsUtil() {
    }

    public static int resolvePassScore(Integer examPassedScore, int fullScore) {
        if (examPassedScore != null && examPassedScore > 0) {
            return examPassedScore;
        }
        if (fullScore > 0) {
            return (int) Math.round(fullScore * 0.6);
        }
        return 0;
    }

    public static List<Map<String, Object>> buildGradeBuckets(List<ScoreBriefingRowVO> rows, int fullScore, int passScore) {
        List<Integer> scores = new ArrayList<>();
        for (ScoreBriefingRowVO row : rows) {
            if (row.getUserScore() != null) {
                scores.add(row.getUserScore());
            }
        }
        int total = fullScore > 0 ? fullScore : (scores.isEmpty() ? 0 : scores.stream().max(Integer::compareTo).orElse(0));
        int pass = clampPass(passScore, total);
        int[] counts = new int[GRADE_LETTERS.length];
        for (int s : scores) {
            int bi = bucketIndex(s, total, pass);
            counts[bi]++;
        }
        int sum = Math.max(scores.size(), 1);
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < GRADE_LETTERS.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("等级", GRADE_LETTERS[i]);
            m.put("档次", GRADE_TIERS[i]);
            m.put("人数", counts[i]);
            m.put("占比", Math.round(counts[i] * 1000.0 / sum) / 10.0 + "%");
            out.add(m);
        }
        return out;
    }

    private static int clampPass(int pass, int full) {
        if (full <= 0) {
            return 0;
        }
        int p = pass;
        if (p <= 0) {
            p = (int) Math.round(full * 0.6);
        }
        if (p >= full) {
            p = (int) Math.round(full * 0.6);
        }
        return p;
    }

    private static int bucketIndex(int score, int full, int pass) {
        int x = Math.min(Math.max(score, 0), full);
        if (x < pass) {
            int mid = pass / 2;
            return x < mid ? 0 : 1;
        }
        int span = full - pass;
        if (span <= 0) {
            return GRADE_LETTERS.length - 1;
        }
        double t1 = pass + span / 3.0;
        double t2 = pass + 2.0 * span / 3.0;
        if (x < t1) {
            return 2;
        }
        if (x < t2) {
            return 3;
        }
        return 4;
    }
}
