package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.converter.QuestionConverter;
import cn.org.alan.exam.mapper.ExerciseRecordMapper;
import cn.org.alan.exam.mapper.OptionMapper;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.entity.ExerciseRecord;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.form.question.QuestionExcelFrom;
import cn.org.alan.exam.model.form.question.QuestionFrom;
import cn.org.alan.exam.model.vo.question.QuestionVO;
import cn.org.alan.exam.service.IQuestionService;
import cn.org.alan.exam.utils.file.FileService;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.excel.ExcelUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * 试题全生命周期：录入与选项联动、分页检索、Excel 批量导入、图片题干上传及逻辑删除；
 * 权限上区分教师自有题库与管理员。
 *
 * @author WeiJin
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    @Resource
    private QuestionConverter questionConverter;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private OptionMapper optionMapper;
    @Resource
    private ExerciseRecordMapper exerciseRecordMapper;

    /** 新增单题并写入选项：简答题仅允许一条参考答案选项；客观题批量插入选项。 */
    @Override
    @Transactional
    public Result<String> addSingleQuestion(QuestionFrom questionFrom) {
        // 入参校验
        List<Option> options = questionFrom.getOptions();
        if (questionFrom.getQuType() != 4 && (Objects.isNull(options) || options.size() < 2)) {
            return Result.failed("非简答题的试题选项不能少于两个");
        }
        if (questionFrom.getParentQuId() != null) {
            Question parent = questionMapper.selectById(questionFrom.getParentQuId());
            if (parent == null) {
                return Result.failed("「共用材料父题 ID」不存在，请先在题库中新建材料题并填写正确 id");
            }
        }
        Question question = questionConverter.fromToEntity(questionFrom);
        // 开始添加题干
        questionMapper.insert(question);
        // 根据试题类型添加选项
        if (question.getQuType() == 4) {
            // 简答题添加选项
            Option option = questionFrom.getOptions().get(0);
            option.setQuId(question.getId());
            optionMapper.insert(option);
        } else {
            // 非简答题添加选项
            // 把新建试题获取的id，填入选项中
            options.forEach(option -> {
                option.setQuId(question.getId());
            });
            optionMapper.insertBatch(options);
        }
        return Result.success("单题添加成功", String.valueOf(question.getId()));
    }

    private void fillCompoundStemOnQuestionVo(QuestionVO vo) {
        if (vo == null || vo.getParentQuId() == null) {
            return;
        }
        Question stem = questionMapper.selectById(vo.getParentQuId());
        if (stem == null) {
            return;
        }
        vo.setStemContent(stem.getContent());
        vo.setStemImage(stem.getImage());
    }

    /** 批量删除试题：先清关联刷题记录与选项，再删题干（与 Mapper 入参语义一致）。 */
    @Override
    @Transactional
    public Result<String> deleteBatchByIds(String ids) {
        List<Integer> qIdList = Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
        // 删除用户刷题记录表
        LambdaUpdateWrapper<ExerciseRecord> updateWrapper = new LambdaUpdateWrapper<ExerciseRecord>()
                .in(ExerciseRecord::getQuestionId, qIdList);
        exerciseRecordMapper.delete(updateWrapper);
        // 先删除选项
        optionMapper.deleteBatchIds(qIdList);
        // 再删除试题
        questionMapper.deleteBatchIds(qIdList);
        return Result.success("批量删除试题成功");
    }

    /** 试题分页：按当前用户角色过滤教师题库；支持标题、题型、所属题库筛选。 */
    @Override
    public Result<IPage<QuestionVO>> pagingQuestion(Integer pageNum, Integer pageSize, String title, Integer type, Integer repoId) {
        IPage<QuestionVO> page = new Page<>(pageNum, pageSize);
        // 获取用户和角色代码
        Integer userId = SecurityUtil.getUserId();
        Integer roleCode = SecurityUtil.getRoleCode();
        // 查询分页试题
        page = questionMapper.selectQuestionPage(page, userId, roleCode, title, type, repoId);
        return Result.success("分页查询试题成功", page);
    }

    /** 单题详情（含选项等由 Mapper 组装为 {@link QuestionVO}）。 */
    @Override
    public Result<QuestionVO> querySingle(Integer id) {
        QuestionVO result = questionMapper.selectSingle(id);
        fillCompoundStemOnQuestionVo(result);
        return Result.success("根据试题id获取单题详情成功", result);
    }

    /** 更新题干及所属选项行（逐条 {@code updateById}）。 */
    @Override
    @Transactional
    public Result<String> updateQuestion(QuestionFrom questionFrom) {
        if (questionFrom.getParentQuId() != null) {
            Question parent = questionMapper.selectById(questionFrom.getParentQuId());
            if (parent == null) {
                return Result.failed("「共用材料父题 ID」不存在");
            }
        }
        // 修改试题
        Question question = questionConverter.fromToEntity(questionFrom);
        questionMapper.updateById(question);
        // 修改选项
        List<Option> options = questionFrom.getOptions();
        for (Option option : options) {
            optionMapper.updateById(option);
        }
        return Result.success("修改试题成功");
    }

    /**
     * 将 Excel 模板试题导入指定题库 {@code id}：逐题插入并批量插入选项，简答题选项默认标记为正确；
     * 支持「材料组编号 + 共用材料题干」多行导入为同一父题下多个小题。
     */
    @SneakyThrows(Exception.class)
    @Override
    @Transactional
    public Result<String> importQuestion(Integer id, MultipartFile file) {
        if (!ExcelUtils.isExcel(Objects.requireNonNull(file.getOriginalFilename()))) {
            throw new ServiceRuntimeException("该文件不是一个合法的Excel文件");
        }

        try {
            List<QuestionExcelFrom> rows = ExcelUtils.readMultipartFile(file, QuestionExcelFrom.class);
            Map<String, Integer> stemParentIdByGroup = new LinkedHashMap<>();
            int rowIndex = 0;
            for (QuestionExcelFrom excelRow : rows) {
                rowIndex++;
                String groupCode = QuestionExcelFrom.normalizeStemGroupCode(excelRow.getStemGroupCode());
                if (groupCode == null) {
                    QuestionFrom qf = excelRow.toQuestionFrom();
                    qf.setParentQuId(null);
                    persistImportedQuestion(qf, id);
                    continue;
                }
                if (!stemParentIdByGroup.containsKey(groupCode)) {
                    if (StringUtils.isBlank(excelRow.getSharedStemContent())) {
                        throw new ServiceRuntimeException(String.format(
                                "Excel 第 %d 条：已填材料组编号「%s」，本组第一行必须填写「共用材料题干」",
                                rowIndex, groupCode));
                    }
                    Integer parentId = insertStemParentQuestion(
                            excelRow.getSharedStemContent().trim(),
                            StringUtils.isNotBlank(excelRow.getSharedStemImage())
                                    ? excelRow.getSharedStemImage().trim()
                                    : null,
                            id);
                    stemParentIdByGroup.put(groupCode, parentId);
                }
                QuestionFrom qf = excelRow.toQuestionFrom();
                qf.setParentQuId(stemParentIdByGroup.get(groupCode));
                persistImportedQuestion(qf, id);
            }

            return Result.success("导入试题成功");
        } catch (ServiceRuntimeException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            return Result.failed("导入试题失败：" + e.getMessage());
        }
    }

    /**
     * 插入仅作共用材料的父题（简答题占位 + 一条参考答案选项），不入卷也可被 {@code parent_qu_id} 引用。
     */
    private Integer insertStemParentQuestion(String stemContent, String stemImage, Integer repoId) {
        Question q = new Question();
        q.setQuType(4);
        q.setContent(stemContent);
        q.setRepoId(repoId);
        q.setImage(stemImage);
        q.setAnalysis(null);
        q.setParentQuId(null);
        int r = questionMapper.insert(q);
        if (r <= 0) {
            throw new ServiceRuntimeException("创建共用材料父题失败");
        }
        Option o = new Option();
        o.setQuId(q.getId());
        o.setContent("（本行为共用材料题干，组卷请只勾选下方各小题，勿单独选本题）");
        o.setIsRight(1);
        o.setSort(1);
        o.setImage(null);
        optionMapper.insert(o);
        return q.getId();
    }

    private void persistImportedQuestion(QuestionFrom questionFrom, Integer repoId) {
        Question question = questionConverter.fromToEntity(questionFrom);
        question.setRepoId(repoId);
        questionMapper.insert(question);
        List<Option> options = questionFrom.getOptions();
        final int[] count = {0};
        options.forEach(option -> {
            if (question.getQuType() == 4) {
                option.setIsRight(1);
            }
            option.setSort(++count[0]);
            option.setQuId(question.getId());
        });
        if (!options.isEmpty()) {
            optionMapper.insertBatch(options);
        }
    }

}
