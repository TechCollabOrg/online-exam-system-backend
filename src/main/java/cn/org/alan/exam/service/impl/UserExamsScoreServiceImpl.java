package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.ExamGradeMapper;
import cn.org.alan.exam.mapper.ExamMapper;
import cn.org.alan.exam.mapper.UserExamsScoreMapper;
import cn.org.alan.exam.mapper.UserGradeMapper;
import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.UserExamsScore;
import cn.org.alan.exam.model.vo.score.ExportScoreVO;
import cn.org.alan.exam.model.vo.score.GradeScoreVO;
import cn.org.alan.exam.model.vo.score.MyExamScoreRow;
import cn.org.alan.exam.model.vo.score.PeerExamScoreRow;
import cn.org.alan.exam.model.vo.score.StudentExamRankPointVO;
import cn.org.alan.exam.model.vo.score.UserScoreVO;
import cn.org.alan.exam.service.IUserExamsScoreService;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.excel.ExcelUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户考试成绩 {@link UserExamsScore}：教师端分页查询、按班级导出 Excel、成绩统计（限定教师所带班级）。
 *
 * @author WeiJin
 */
@Service
public class UserExamsScoreServiceImpl extends ServiceImpl<UserExamsScoreMapper, UserExamsScore> implements IUserExamsScoreService {
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private UserGradeMapper userGradeMapper;

    /**
     * 多条件分页查询成绩列表（班级、考试、真实姓名模糊），返回 VO 分页。
     */
    @Override
    public Result<IPage<UserScoreVO>> pagingScore(Integer pageNum, Integer pageSize, Integer gradeId, Integer examId, String realName) {
        IPage<UserScoreVO> page = new Page<>(pageNum, pageSize);
        page = userExamsScoreMapper.pagingScore(page, gradeId, examId, realName);
        return Result.success(null, page);
    }

    /**
     * 导出指定考试与班级下的成绩为 Excel，并为每条记录生成名次序号。
     */
    @Override
    public void exportScores(HttpServletResponse response, Integer examId, Integer gradeId) {
        // 获取成绩信息
        List<ExportScoreVO> scores = userExamsScoreMapper.selectScores(examId, gradeId);
        final int[] sort = {0};
        scores.forEach(exportScoreVO -> exportScoreVO.setRanking(++sort[0]));
        // 获取考试名
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>().eq(Exam::getId, examId).select(Exam::getTitle);
        Exam exam = examMapper.selectOne(wrapper);
        // 生成表格并响应
        ExcelUtils.export(response, exam.getTitle(), scores, ExportScoreVO.class);

    }

    /**
     * 教师查看成绩统计：若无关联班级则报错；否则按角色与班级 ID 列表过滤分页数据。
     */
    @Override
    public Result<IPage<GradeScoreVO>> getExamScoreInfo(Integer pageNum, Integer pageSize, String examTitle, Integer gradeId) {
        IPage<GradeScoreVO> page = new Page<>(pageNum, pageSize);
        Integer userId = SecurityUtil.getUserId();
        // 根据用户id查询老师所加入的班级
        List<Integer> gradeIdList = userGradeMapper.getGradeIdListByUserId(userId);
        if (gradeIdList.isEmpty()) {
            throw new ServiceRuntimeException("教师还没加入班级暂无数据");
        }
        Integer roleCode = SecurityUtil.getRoleCode();
        page = userExamsScoreMapper.scoreStatistics(page, gradeId, examTitle, userId, roleCode, gradeIdList);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<List<StudentExamRankPointVO>> getStudentRankTrend() {
        Integer userId = SecurityUtil.getUserId();
        Integer gradeId = SecurityUtil.getGradeId();
        if (gradeId == null) {
            return Result.success("未加入班级，暂无排名数据", Collections.emptyList());
        }
        List<MyExamScoreRow> mine = userExamsScoreMapper.listMyCompletedExamScores(userId);
        if (mine == null || mine.isEmpty()) {
            return Result.success("暂无考试记录", Collections.emptyList());
        }
        List<Integer> examIds = mine.stream()
                .map(MyExamScoreRow::getExamId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<PeerExamScoreRow> peers = examIds.isEmpty()
                ? Collections.emptyList()
                : userExamsScoreMapper.listClassScoresForExams(gradeId, examIds);
        Map<Integer, List<PeerExamScoreRow>> byExam = peers.stream().collect(Collectors.groupingBy(PeerExamScoreRow::getExamId));

        List<StudentExamRankPointVO> out = new ArrayList<>();
        for (MyExamScoreRow row : mine) {
            List<PeerExamScoreRow> list = byExam.getOrDefault(row.getExamId(), Collections.emptyList());
            int classSize = list.size();
            int myScore = row.getUserScore() == null ? Integer.MIN_VALUE : row.getUserScore();
            long higher = list.stream()
                    .mapToInt(p -> p.getUserScore() == null ? Integer.MIN_VALUE : p.getUserScore())
                    .filter(sc -> sc > myScore)
                    .count();
            int rank = (int) higher + 1;

            StudentExamRankPointVO vo = new StudentExamRankPointVO();
            vo.setExamId(row.getExamId());
            vo.setExamTitle(row.getExamTitle());
            vo.setSubjectLabel(deriveSubjectLabel(row.getExamTitle()));
            vo.setUserScore(row.getUserScore());
            vo.setGrossScore(row.getGrossScore());
            vo.setRankInClass(rank);
            vo.setClassAttendCount(classSize);
            vo.setExamTime(row.getLimitTime());
            out.add(vo);
        }
        return Result.success("查询成功", out);
    }

    /**
     * 从试卷标题推断「学科」分组：优先按常见分隔符取前半段，便于「数学-月考一」「数学-月考二」聚成一条曲线。
     */
    private static String deriveSubjectLabel(String examTitle) {
        if (examTitle == null || examTitle.trim().isEmpty()) {
            return "未命名";
        }
        String t = examTitle.trim();
        String[] seps = {" - ", "-", "—", "－", "_", "|", "·", "："};
        for (String sep : seps) {
            int i = t.indexOf(sep);
            if (i > 0) {
                return t.substring(0, i).trim();
            }
        }
        return t;
    }
}
