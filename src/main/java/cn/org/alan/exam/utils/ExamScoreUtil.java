package cn.org.alan.exam.utils;



import cn.org.alan.exam.common.exception.ServiceRuntimeException;

import cn.org.alan.exam.model.vo.exam.ExamGradeListVO;
import cn.org.alan.exam.model.vo.record.ExamRecordVO;

import cn.org.alan.exam.model.vo.score.ExportScoreVO;

import cn.org.alan.exam.model.vo.score.GradeScoreVO;

import cn.org.alan.exam.model.vo.score.ScoreBriefingRowVO;

import cn.org.alan.exam.model.vo.score.StudentExamRankPointVO;

import cn.org.alan.exam.model.vo.score.UserScoreVO;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;



import java.math.BigDecimal;

import java.math.RoundingMode;

import java.util.Arrays;

import java.util.Collections;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.stream.Collectors;



/**

 * 考试分值：数据库存整数（实际分 × {@link #SCALE}），接口与组卷页使用可带两位小数的展示分。

 */

public final class ExamScoreUtil {



    public static final int SCALE = 100;



    private static final int DISPLAY_SCALE = 2;



    private ExamScoreUtil() {

    }



    /** 展示分 → 库内存储（四舍五入到分） */

    public static int toStorage(BigDecimal display) {

        if (display == null) {

            return 0;

        }

        return display.multiply(BigDecimal.valueOf(SCALE))

                .setScale(0, RoundingMode.HALF_UP)

                .intValue();

    }



    public static int toStorage(double display) {

        return toStorage(BigDecimal.valueOf(display));

    }



    public static int toStorage(String display) {

        if (StringUtils.isBlank(display)) {

            throw new ServiceRuntimeException("分值不能为空");

        }

        try {

            return toStorage(new BigDecimal(display.trim()));

        } catch (NumberFormatException e) {

            throw new ServiceRuntimeException("分值格式错误，请使用最多两位小数的数字");

        }

    }



    /** 库内存储 → 展示分 */

    public static BigDecimal toDisplay(Integer storage) {

        if (storage == null) {

            return BigDecimal.ZERO.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);

        }

        return BigDecimal.valueOf(storage)

                .divide(BigDecimal.valueOf(SCALE), DISPLAY_SCALE, RoundingMode.HALF_UP);

    }



    public static double toDisplayDouble(Integer storage) {

        return toDisplay(storage).doubleValue();

    }



    public static Integer toDisplayInteger(Integer storage) {

        if (storage == null) {

            return null;

        }

        BigDecimal display = toDisplay(storage);

        if (display.stripTrailingZeros().scale() <= 0) {

            return display.intValue();

        }

        return null;

    }



    public static List<Integer> parseCsvToStorageList(String raw, String fieldName) {

        if (StringUtils.isBlank(raw)) {

            throw new ServiceRuntimeException(fieldName + "不能为空");

        }

        try {

            return Arrays.stream(raw.split(","))

                    .map(String::trim)

                    .filter(StringUtils::isNotBlank)

                    .map(ExamScoreUtil::toStorage)

                    .collect(Collectors.toList());

        } catch (ServiceRuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new ServiceRuntimeException(fieldName + "格式错误");

        }

    }



    /**

     * 解析 quIds 与 quScores（展示分、逗号分隔），返回题目 ID → 库存分值。

     */

    public static Map<Integer, Integer> parseQuScoreMap(String quIds, String quScores) {

        if (StringUtils.isBlank(quIds) || StringUtils.isBlank(quScores)) {

            return Collections.emptyMap();

        }

        String[] ids = quIds.split(",");

        String[] scores = quScores.split(",");

        if (ids.length != scores.length) {

            throw new ServiceRuntimeException("题目与分值数量不一致");

        }

        Map<Integer, Integer> map = new LinkedHashMap<>();

        for (int i = 0; i < ids.length; i++) {

            String idStr = ids[i].trim();

            String scoreStr = scores[i].trim();

            if (idStr.isEmpty() || scoreStr.isEmpty()) {

                continue;

            }

            int quId = Integer.parseInt(idStr);

            int score = toStorage(scoreStr);

            if (score <= 0) {

                throw new ServiceRuntimeException("题目 ID " + quId + " 分值必须大于 0");

            }

            map.put(quId, score);

        }

        return map;

    }



    public static int sumStorage(Map<Integer, Integer> storageScores) {

        return storageScores.values().stream().mapToInt(Integer::intValue).sum();

    }



    /** 库内存储 → 展示分；{@code null} 仍为 {@code null}。 */

    public static Double toDisplayDoubleOrNull(Integer storage) {

        return storage == null ? null : toDisplayDouble(storage);

    }



    /** 将 MyBatis 映射到 {@link Double} 的库内整数转为展示分。 */

    public static Double storageFieldToDisplay(Double storageLike) {

        if (storageLike == null) {

            return null;

        }

        return toDisplay(BigDecimal.valueOf(storageLike)

                .setScale(0, RoundingMode.HALF_UP)

                .intValue()).doubleValue();

    }



    /** SQL AVG 等可能带小数的库内分 → 展示分。 */

    public static Double avgStorageToDisplay(Double avgStorage) {

        if (avgStorage == null) {

            return null;

        }

        return BigDecimal.valueOf(avgStorage)

                .divide(BigDecimal.valueOf(SCALE), DISPLAY_SCALE, RoundingMode.HALF_UP)

                .doubleValue();

    }



    public static void applyDisplay(UserScoreVO vo) {

        if (vo == null) {

            return;

        }

        vo.setUserScore(storageFieldToDisplay(vo.getUserScore()));

    }



    public static void applyDisplay(GradeScoreVO vo) {

        if (vo == null) {

            return;

        }

        vo.setPassedScore(storageFieldToDisplay(vo.getPassedScore()));

        vo.setAvgScore(avgStorageToDisplay(vo.getAvgScore()));

        vo.setMaxScore(storageFieldToDisplay(vo.getMaxScore()));

        vo.setMinScore(storageFieldToDisplay(vo.getMinScore()));

    }



    public static void applyDisplay(ExamRecordVO vo) {

        if (vo == null) {

            return;

        }

        vo.setPassedScore(storageFieldToDisplay(vo.getPassedScore()));

        vo.setGrossScore(storageFieldToDisplay(vo.getGrossScore()));

        vo.setUserScore(storageFieldToDisplay(vo.getUserScore()));

        vo.setRadioScore(storageFieldToDisplay(vo.getRadioScore()));

        vo.setMultiScore(storageFieldToDisplay(vo.getMultiScore()));

        vo.setJudgeScore(storageFieldToDisplay(vo.getJudgeScore()));

        vo.setSaqScore(storageFieldToDisplay(vo.getSaqScore()));

    }

    public static void applyDisplay(ExamGradeListVO vo) {

        if (vo == null) {

            return;

        }

        vo.setPassedScore(storageFieldToDisplay(vo.getPassedScore()));

        vo.setGrossScore(storageFieldToDisplay(vo.getGrossScore()));

        vo.setRadioScore(storageFieldToDisplay(vo.getRadioScore()));

        vo.setMultiScore(storageFieldToDisplay(vo.getMultiScore()));

        vo.setJudgeScore(storageFieldToDisplay(vo.getJudgeScore()));

        vo.setSaqScore(storageFieldToDisplay(vo.getSaqScore()));

        vo.setCompoundScore(storageFieldToDisplay(vo.getCompoundScore()));

    }



    public static void applyDisplay(ExportScoreVO vo) {

        if (vo == null) {

            return;

        }

        vo.setScore(storageFieldToDisplay(vo.getScore()));

    }



    public static void applyDisplay(StudentExamRankPointVO vo) {

        if (vo == null) {

            return;

        }

        vo.setUserScore(storageFieldToDisplay(vo.getUserScore()));

        vo.setGrossScore(storageFieldToDisplay(vo.getGrossScore()));

    }



    public static void applyDisplay(ScoreBriefingRowVO vo) {

        if (vo == null) {

            return;

        }

        vo.setUserScore(storageFieldToDisplay(vo.getUserScore()));

    }

}


