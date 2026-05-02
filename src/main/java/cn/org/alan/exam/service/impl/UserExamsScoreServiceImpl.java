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
import java.util.List;

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
}
