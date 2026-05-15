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
import cn.org.alan.exam.model.form.question.QuestionJsonImportRow;
import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm;
import cn.org.alan.exam.model.vo.question.QuestionVO;
import cn.org.alan.exam.service.IQuestionService;
import cn.org.alan.exam.utils.QuestionSubItemsUtil;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.excel.ExcelUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 试题全生命周期：录入与选项联动、分页检索、Excel/JSON 批量导入、图片题干上传及逻辑删除；
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

    /** 新增单题：客观题写入选项；简答题每空一条选项；复合题（类型5）小题存 sub_items JSON。 */
    @Override
    @Transactional
    public Result<String> addSingleQuestion(QuestionFrom questionFrom) {
        List<Option> options = questionFrom.getOptions();
        int qt = questionFrom.getQuType() == null ? -1 : questionFrom.getQuType();
        if (qt == 5) {
            Result<String> compoundCheck = validateCompoundQuestion(questionFrom);
            if (compoundCheck != null) {
                return compoundCheck;
            }
        } else if (qt != 4 && (Objects.isNull(options) || options.size() < 2)) {
            return Result.failed("非简答题的试题选项不能少于两个");
        }
        Question question = questionConverter.fromToEntity(questionFrom);
        if (qt == 5) {
            question.setSubItems(QuestionSubItemsUtil.toJson(questionFrom.getSubItems()));
        }
        questionMapper.insert(question);
        if (question.getQuType() == 4) {
            List<Option> opts = questionFrom.getOptions() == null ? Collections.emptyList() : questionFrom.getOptions();
            if (opts.isEmpty()) {
                return Result.failed("简答题至少需要一空（一条参考答案选项）");
            }
            int sort = 0;
            for (Option option : opts) {
                option.setQuId(question.getId());
                option.setIsRight(1);
                option.setSort(++sort);
            }
            optionMapper.insertBatch(opts);
        } else if (question.getQuType() != 5) {
            options.forEach(option -> option.setQuId(question.getId()));
            optionMapper.insertBatch(options);
        }
        return Result.success("单题添加成功", String.valueOf(question.getId()));
    }

    /** 批量删除试题：先清关联刷题记录与选项，再删题干（与 Mapper 入参语义一致）。 */
    @Override
    @Transactional
    public Result<String> deleteBatchByIds(String ids) {
        List<Integer> qIdList = Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
        LambdaUpdateWrapper<ExerciseRecord> updateWrapper = new LambdaUpdateWrapper<ExerciseRecord>()
                .in(ExerciseRecord::getQuestionId, qIdList);
        exerciseRecordMapper.delete(updateWrapper);
        optionMapper.deleteBatchIds(qIdList);
        questionMapper.deleteBatchIds(qIdList);
        return Result.success("批量删除试题成功");
    }

    /** 试题分页：按当前用户角色过滤教师题库；支持标题、题型、所属题库筛选。 */
    @Override
    public Result<IPage<QuestionVO>> pagingQuestion(Integer pageNum, Integer pageSize, String title, Integer type, Integer repoId) {
        IPage<QuestionVO> page = new Page<>(pageNum, pageSize);
        Integer userId = SecurityUtil.getUserId();
        Integer roleCode = SecurityUtil.getRoleCode();
        page = questionMapper.selectQuestionPage(page, userId, roleCode, title, type, repoId);
        return Result.success("分页查询试题成功", page);
    }

    /** 单题详情（含选项等由 Mapper 组装为 {@link QuestionVO}）。 */
    @Override
    public Result<QuestionVO> querySingle(Integer id) {
        QuestionVO result = questionMapper.selectSingle(id);
        if (result != null && Integer.valueOf(5).equals(result.getQuType())) {
            Question entity = questionMapper.selectById(id);
            if (entity != null) {
                result.setSubItems(QuestionSubItemsUtil.parseForms(entity.getSubItems()));
            }
        }
        return Result.success("根据试题id获取单题详情成功", result);
    }

    /** 更新题干及所属选项行（逐条 {@code updateById}）。 */
    @Override
    @Transactional
    public Result<String> updateQuestion(QuestionFrom questionFrom) {
        int qt = questionFrom.getQuType() == null ? -1 : questionFrom.getQuType();
        if (qt == 5) {
            Result<String> compoundCheck = validateCompoundQuestion(questionFrom);
            if (compoundCheck != null) {
                return compoundCheck;
            }
        }
        Question question = questionConverter.fromToEntity(questionFrom);
        if (qt == 5) {
            question.setSubItems(QuestionSubItemsUtil.toJson(questionFrom.getSubItems()));
        }
        questionMapper.updateById(question);
        if (qt != 5) {
            List<Option> options = questionFrom.getOptions();
            if (options != null) {
                for (Option option : options) {
                    optionMapper.updateById(option);
                }
            }
        }
        return Result.success("修改试题成功");
    }

    /**
     * 将 Excel 或 JSON 模板试题导入指定题库 {@code id}；
     * 材料组编号多行合并为一条复合题（题型 5），否则按行导入普通题（Excel 与 JSON 字段语义一致）。
     */
    @SneakyThrows(Exception.class)
    @Override
    @Transactional
    public Result<String> importQuestion(Integer id, MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (ExcelUtils.isJson(filename)) {
            try {
                return importQuestionsFromJson(id, file.getBytes());
            } catch (IOException e) {
                return Result.failed("读取上传文件失败：" + e.getMessage());
            }
        }
        if (ExcelUtils.isExcel(filename)) {
            try {
                List<QuestionExcelFrom> rows = ExcelUtils.readMultipartFile(file, QuestionExcelFrom.class);
                importExcelRows(id, rows);
                return Result.success("导入试题成功");
            } catch (ServiceRuntimeException e) {
                return Result.failed(e.getMessage());
            } catch (Exception e) {
                return Result.failed("导入试题失败：" + e.getMessage());
            }
        }
        try {
            byte[] raw = file.getBytes();
            if (looksLikeJsonUtf8(raw)) {
                return importQuestionsFromJson(id, raw);
            }
        } catch (IOException e) {
            return Result.failed("读取上传文件失败：" + e.getMessage());
        }
        throw new ServiceRuntimeException(
                "仅支持 Excel（.xls/.xlsx）或 JSON（.json；无扩展名时须为 UTF-8 的 JSON 数组或 {\"questions\":...}）");
    }

    private void importExcelRows(Integer repoId, List<QuestionExcelFrom> rows) {
        Map<String, List<QuestionExcelFrom>> grouped = new LinkedHashMap<>();
        List<QuestionExcelFrom> singles = new ArrayList<>();
        for (QuestionExcelFrom excelRow : rows) {
            String groupCode = QuestionExcelFrom.normalizeStemGroupCode(excelRow.getStemGroupCode());
            if (groupCode == null) {
                singles.add(excelRow);
                continue;
            }
            grouped.computeIfAbsent(groupCode, k -> new ArrayList<>()).add(excelRow);
        }
        for (QuestionExcelFrom excelRow : singles) {
            if (StringUtils.isBlank(excelRow.getContent())) {
                throw new ServiceRuntimeException("普通试题（未填材料组编号）的题干不能为空");
            }
            persistImportedQuestion(excelRow.toQuestionFrom(), repoId);
        }
        for (Map.Entry<String, List<QuestionExcelFrom>> entry : grouped.entrySet()) {
            List<QuestionExcelFrom> groupRows = entry.getValue();
            if (groupRows.isEmpty()) {
                continue;
            }
            QuestionExcelFrom first = groupRows.get(0);
            if (StringUtils.isBlank(first.getSharedStemContent())) {
                throw new ServiceRuntimeException(String.format(
                        "材料组「%s」第一行必须填写「共用材料题干」", entry.getKey()));
            }
            List<QuestionFrom> rowForms = new ArrayList<>();
            for (QuestionExcelFrom row : groupRows) {
                rowForms.add(row.toQuestionFrom());
            }
            persistImportedQuestion(buildCompoundQuestion(first, rowForms), repoId);
        }
    }

    /**
     * 跳过空白与 UTF-8 BOM 后，若首字符为 {@code [} 或对象起始符，则视为 JSON 文本（与扩展名无关）。
     */
    private static boolean looksLikeJsonUtf8(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return false;
        }
        int i = 0;
        if (raw.length >= 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        while (i < raw.length) {
            byte b = raw[i];
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
                i++;
                continue;
            }
            return b == '[' || b == '{';
        }
        return false;
    }

    /**
     * JSON 根为试题对象数组，或 {@code {"questions":[...]}}；单题字段见 {@link QuestionJsonImportRow}。
     */
    private Result<String> importQuestionsFromJson(Integer id, byte[] fileBytes) {
        try {
            List<QuestionJsonImportRow> rows = parseJsonQuestionRows(fileBytes);
            if (rows.isEmpty()) {
                return Result.failed("JSON 中未包含任何试题");
            }
            Map<String, List<QuestionJsonImportRow>> grouped = new LinkedHashMap<>();
            List<QuestionJsonImportRow> singles = new ArrayList<>();
            for (QuestionJsonImportRow jsonRow : rows) {
                String groupCode = QuestionExcelFrom.normalizeStemGroupCode(jsonRow.getStemGroupCode());
                if (groupCode == null) {
                    singles.add(jsonRow);
                    continue;
                }
                grouped.computeIfAbsent(groupCode, k -> new ArrayList<>()).add(jsonRow);
            }
            int rowIndex = 0;
            for (QuestionJsonImportRow jsonRow : singles) {
                rowIndex++;
                persistImportedQuestion(jsonRow.toQuestionFrom(rowIndex), id);
            }
            for (Map.Entry<String, List<QuestionJsonImportRow>> entry : grouped.entrySet()) {
                List<QuestionJsonImportRow> groupRows = entry.getValue();
                if (groupRows.isEmpty()) {
                    continue;
                }
                QuestionJsonImportRow first = groupRows.get(0);
                if (StringUtils.isBlank(first.getSharedStemContent())) {
                    throw new ServiceRuntimeException(String.format(
                            "JSON 材料组「%s」第一题须填写 sharedStemContent（共用材料题干）", entry.getKey()));
                }
                List<QuestionFrom> rowForms = new ArrayList<>();
                int subIndex = 0;
                for (QuestionJsonImportRow row : groupRows) {
                    rowForms.add(row.toQuestionFrom(++subIndex, true));
                }
                QuestionFrom compound = buildCompoundQuestionFromJsonFirst(first, rowForms);
                persistImportedQuestion(compound, id);
            }
            return Result.success("导入试题成功");
        } catch (ServiceRuntimeException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            return Result.failed("导入试题失败：" + e.getMessage());
        }
    }

    private List<QuestionJsonImportRow> parseJsonQuestionRows(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        if (text.isEmpty()) {
            throw new ServiceRuntimeException("JSON 文件为空");
        }
        Object parsed = JSON.parse(text);
        JSONArray arr;
        if (parsed instanceof JSONArray) {
            arr = (JSONArray) parsed;
        } else if (parsed instanceof JSONObject) {
            JSONArray nested = ((JSONObject) parsed).getJSONArray("questions");
            if (nested == null) {
                throw new ServiceRuntimeException("JSON 格式错误：使用对象根节点时须包含 questions 数组字段");
            }
            arr = nested;
        } else {
            throw new ServiceRuntimeException("JSON 格式错误：根节点须为题对象数组 [...] 或 {\"questions\":[...]}");
        }
        List<QuestionJsonImportRow> rows = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            rows.add(parseOneJsonQuestionRow(o));
        }
        return rows;
    }

    private static QuestionJsonImportRow parseOneJsonQuestionRow(JSONObject o) {
        QuestionJsonImportRow row = new QuestionJsonImportRow();
        row.setQuType(o.getInteger("quType"));
        row.setContent(o.getString("content"));
        row.setAnalysis(o.getString("analysis"));
        row.setImage(o.getString("image"));
        row.setStemGroupCode(o.getString("stemGroupCode"));
        row.setSharedStemContent(o.getString("sharedStemContent"));
        row.setSharedStemImage(o.getString("sharedStemImage"));
        JSONArray opts = o.getJSONArray("options");
        List<QuestionJsonImportRow.JsonOption> list = new ArrayList<>();
        if (opts != null) {
            for (int j = 0; j < opts.size(); j++) {
                JSONObject jo = opts.getJSONObject(j);
                QuestionJsonImportRow.JsonOption opt = new QuestionJsonImportRow.JsonOption();
                opt.setContent(jo.getString("content"));
                opt.setIsRight(QuestionJsonImportRow.normalizeIsRight(jo.get("isRight")));
                opt.setImage(jo.getString("image"));
                list.add(opt);
            }
        }
        row.setOptions(list);
        return row;
    }

    private static QuestionFrom buildCompoundQuestion(QuestionExcelFrom first, List<QuestionFrom> rowForms) {
        QuestionFrom compound = new QuestionFrom();
        compound.setQuType(5);
        compound.setContent(first.getSharedStemContent().trim());
        compound.setImage(StringUtils.isNotBlank(first.getSharedStemImage())
                ? first.getSharedStemImage().trim() : null);
        compound.setAnalysis(first.getAnalysis());
        compound.setSubItems(buildSubItemsFromRowForms(rowForms));
        return compound;
    }

    private static QuestionFrom buildCompoundQuestionFromJsonFirst(QuestionJsonImportRow first, List<QuestionFrom> rowForms) {
        QuestionFrom compound = new QuestionFrom();
        compound.setQuType(5);
        compound.setContent(first.getSharedStemContent().trim());
        compound.setImage(StringUtils.isNotBlank(first.getSharedStemImage())
                ? first.getSharedStemImage().trim() : null);
        compound.setAnalysis(StringUtils.isBlank(first.getAnalysis()) ? null : first.getAnalysis().trim());
        compound.setSubItems(buildSubItemsFromRowForms(rowForms));
        return compound;
    }

    private static List<QuestionSubItemForm> buildSubItemsFromRowForms(List<QuestionFrom> rowForms) {
        List<QuestionSubItemForm> subItems = new ArrayList<>();
        int sort = 0;
        for (QuestionFrom rowForm : rowForms) {
            QuestionSubItemForm sub = new QuestionSubItemForm();
            sub.setSort(++sort);
            sub.setContent(rowForm.getContent());
            sub.setQuType(rowForm.getQuType());
            sub.setOptions(toSubItemOptions(rowForm.getOptions()));
            subItems.add(sub);
        }
        return subItems;
    }

    private static List<QuestionSubItemOptionForm> toSubItemOptions(List<Option> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        List<QuestionSubItemOptionForm> out = new ArrayList<>();
        for (Option opt : options) {
            QuestionSubItemOptionForm f = new QuestionSubItemOptionForm();
            f.setContent(opt.getContent());
            f.setImage(opt.getImage());
            f.setIsRight(opt.getIsRight());
            out.add(f);
        }
        return out;
    }

    private void persistImportedQuestion(QuestionFrom questionFrom, Integer repoId) {
        if (Integer.valueOf(5).equals(questionFrom.getQuType())) {
            Result<String> check = validateCompoundQuestion(questionFrom);
            if (check != null) {
                throw new ServiceRuntimeException(check.getMsg());
            }
        }
        Question question = questionConverter.fromToEntity(questionFrom);
        question.setRepoId(repoId);
        if (Integer.valueOf(5).equals(questionFrom.getQuType())) {
            question.setSubItems(QuestionSubItemsUtil.toJson(questionFrom.getSubItems()));
        }
        questionMapper.insert(question);
        if (Integer.valueOf(5).equals(question.getQuType())) {
            return;
        }
        List<Option> options = questionFrom.getOptions() == null ? Collections.emptyList() : questionFrom.getOptions();
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

    private Result<String> validateCompoundQuestion(QuestionFrom questionFrom) {
        if (StringUtils.isBlank(questionFrom.getContent())) {
            return Result.failed("复合题须填写共用材料（题目内容）");
        }
        List<QuestionSubItemForm> subItems = questionFrom.getSubItems();
        if (subItems == null || subItems.isEmpty()) {
            return Result.failed("复合题至少需要一道小题");
        }
        int idx = 0;
        for (QuestionSubItemForm sub : subItems) {
            idx++;
            if (sub == null) {
                return Result.failed("第 " + idx + " 道小题数据无效");
            }
            if (sub.getQuType() == null || sub.getQuType() < 1 || sub.getQuType() > 4) {
                return Result.failed("第 " + idx + " 道小题题型无效");
            }
            if (!QuestionSubItemsUtil.hasMeaningfulStemContent(sub.getContent())) {
                return Result.failed("第 " + idx + " 道小题题干不能为空（可输入文字或在编辑器中插入图片）");
            }
            List<QuestionSubItemOptionForm> opts = sub.getOptions();
            if (sub.getQuType() == 4) {
                if (opts == null || opts.isEmpty()) {
                    return Result.failed("第 " + idx + " 道简答小题至少需要一空");
                }
            } else if (opts == null || opts.size() < 2) {
                return Result.failed("第 " + idx + " 道客观小题选项不能少于两个");
            }
            if (sub.getQuType() != 4 && opts != null) {
                long rightCount = opts.stream().filter(o -> o != null && Integer.valueOf(1).equals(o.getIsRight())).count();
                if (sub.getQuType() == 1 || sub.getQuType() == 3) {
                    if (rightCount != 1) {
                        return Result.failed("第 " + idx + " 道小题：单选/判断题只能有一个正确答案");
                    }
                } else if (sub.getQuType() == 2 && rightCount < 2) {
                    return Result.failed("第 " + idx + " 道小题：多选题至少两个正确答案");
                }
            }
        }
        return null;
    }

}
