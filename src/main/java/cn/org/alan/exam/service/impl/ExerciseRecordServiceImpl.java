package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.converter.ExerciseConverter;
import cn.org.alan.exam.converter.RecordConverter;
import cn.org.alan.exam.mapper.*;
import cn.org.alan.exam.model.entity.*;
import cn.org.alan.exam.model.form.exercise.ExerciseFillAnswerFrom;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import cn.org.alan.exam.model.vo.question.QuestionVO;
import cn.org.alan.exam.model.vo.exercise.AnswerInfoVO;
import cn.org.alan.exam.model.vo.exercise.QuestionSheetVO;
import cn.org.alan.exam.model.vo.record.ExamRecordDetailVO;
import cn.org.alan.exam.utils.QuestionSubItemsUtil;
import cn.org.alan.exam.model.vo.record.ExamRecordVO;
import cn.org.alan.exam.model.vo.record.ExerciseRecordDetailVO;
import cn.org.alan.exam.model.vo.record.ExerciseRecordVO;
import cn.org.alan.exam.service.IExerciseRecordService;
import cn.org.alan.exam.service.IOptionService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 练习模块：按题库与题型拉取答题卡、提交单题答案并记录对错、练习统计分页、考试与练习答题明细回放；
 * 管理员/教师/学生查询范围由角色码分支控制。
 *
 * @author WeiJin
 */
@Service
public class ExerciseRecordServiceImpl extends ServiceImpl<ExerciseRecordMapper, ExerciseRecord>
        implements IExerciseRecordService {

    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private RecordConverter recordConverter;
    @Resource
    private ExamQuestionMapper examQuestionMapper;
    @Resource
    private OptionMapper optionMapper;
    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;
    @Resource
    private IOptionService optionService;
    @Resource
    private UserExerciseRecordMapper userExerciseRecordMapper;
    @Resource
    private RepoMapper repoMapper;
    @Resource
    private ExerciseConverter exerciseConverter;
    @Resource
    private ExerciseRecordMapper exerciseRecordMapper;

    /** 某题库下指定题型的刷题答题卡列表（Mapper 按用户过滤已练状态等）。 */
    @Override
    public Result<List<QuestionSheetVO>> getQuestionSheet(Integer repoId, Integer quType) {
        List<QuestionSheetVO> list = questionMapper.selectQuestionSheet(repoId, quType, SecurityUtil.getUserId());
        return Result.success("获取获取试题答题卡列表成功", list);
    }

    /**
     * 正式考试记录分页：管理员全站、教师仅自建试卷、学生仅本人。
     */
    @Override
    public Result<IPage<ExamRecordVO>> getExamRecordPage(Integer pageNum, Integer pageSize, String examName, Boolean isASC) {
        // 创建page对象
        Page<ExamRecordVO> examPage = new Page<>(pageNum, pageSize);
        
        // 获取当前用户ID和角色
        Integer userId = SecurityUtil.getUserId();
        Integer roleCode = SecurityUtil.getRoleCode();
        
        // 根据不同角色查询不同的试卷
        if (roleCode==3) {
            // 管理员查询所有已作答的试卷
            examPage = examMapper.getAllExamRecordPage(examPage, examName, isASC);
        } else if (roleCode==2) {
            // 教师查询自己创建的试卷
            examPage = examMapper.getTeacherExamRecordPage(examPage, userId, examName, isASC);
        } else {
            // 学生查询自己的试卷
            examPage = examMapper.getExamRecordPage(examPage, userId, examName, isASC);
        }
        
        return Result.success("分页查询已考试试卷成功", examPage);
    }

    /**
     * 某场考试答题回放明细：{@code userId} 为空则取当前用户；组装选项、正误、本人答案等。
     */
    @Override
    public Result<List<ExamRecordDetailVO>> getExamRecordDetail(Integer examId, Integer userId) {
        if(userId==null){
            userId =SecurityUtil.getUserId();
        }
        // 1、题干 2、选项 3、自己的答案 4、正确的答案 5、是否正确 6、试题分析
        List<ExamRecordDetailVO> examRecordDetailVOS = new ArrayList<>();
        // 查询该考试的试题
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSort);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        List<Integer> quIds = examQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toList());
        if (quIds.isEmpty()) {
            return Result.success("查询考试的信息成功", examRecordDetailVOS);
        }
        List<Question> questions = questionMapper.selectBatchIds(quIds);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        for (ExamQuestion eq : examQuestions) {
            Question temp = questionMap.get(eq.getQuestionId());
            if (temp == null) {
                continue;
            }
            ExamRecordDetailVO examRecordDetailVO = new ExamRecordDetailVO();
            examRecordDetailVO.setImage(temp.getImage());
            examRecordDetailVO.setTitle(temp.getContent());
            examRecordDetailVO.setQuType(temp.getQuType());
            examRecordDetailVO.setAnalyse(temp.getAnalysis());
            Integer quType = temp.getQuType();

            if (Integer.valueOf(5).equals(quType)) {
                List<QuestionSubItemVO> subItems = QuestionSubItemsUtil.parseToVoList(temp.getSubItems());
                examRecordDetailVO.setSubItemList(subItems);
                examRecordDetailVO.setOption(null);
                examRecordDetailVO.setRightOption(null);
            } else {
                LambdaQueryWrapper<Option> optionWrapper = new LambdaQueryWrapper<>();
                optionWrapper.eq(Option::getQuId, temp.getId()).orderByAsc(Option::getSort);
                List<Option> options = optionMapper.selectList(optionWrapper);
                if (Integer.valueOf(4).equals(quType)) {
                    examRecordDetailVO.setOption(options.isEmpty() ? null : options);
                    if (!options.isEmpty()) {
                        examRecordDetailVO.setRightOption(options.get(0).getContent());
                    }
                } else {
                    examRecordDetailVO.setOption(options);
                    ArrayList<Integer> strings = new ArrayList<>();
                    for (Option temp1 : options) {
                        if (temp1.getIsRight() == 1) {
                            strings.add(temp1.getSort());
                        }
                    }
                    List<String> stringList = strings.stream().map(String::valueOf).collect(Collectors.toList());
                    examRecordDetailVO.setRightOption(String.join(",", stringList));
                }
            }

            LambdaQueryWrapper<ExamQuAnswer> examQuAnswerWrapper = new LambdaQueryWrapper<>();
            examQuAnswerWrapper.eq(ExamQuAnswer::getUserId, userId)
                    .eq(ExamQuAnswer::getExamId, examId)
                    .eq(ExamQuAnswer::getQuestionId, temp.getId());
            ExamQuAnswer examQuAnswer = examQuAnswerMapper.selectOne(examQuAnswerWrapper);
            if (examQuAnswer == null) {
                examRecordDetailVO.setMyOption(null);
                examRecordDetailVO.setIsRight(-1);
                examRecordDetailVOS.add(examRecordDetailVO);
                continue;
            }
            switch (quType) {
                case 1:
                    // 设置自己的选项
                    LambdaQueryWrapper<Option> optionLambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                    optionLambdaQueryWrapper1.eq(Option::getId, examQuAnswer.getAnswerId());
                    Option op1 = optionMapper.selectOne(optionLambdaQueryWrapper1);
                    examRecordDetailVO.setMyOption(Integer.toString(op1.getSort()));
                    // 设置是否正确
                    Option byId1 = optionService.getById(examQuAnswer.getAnswerId());
                    if (byId1.getIsRight() == 1) {
                        examRecordDetailVO.setIsRight(1);
                    } else {
                        examRecordDetailVO.setIsRight(0);
                    }
                    break;
                case 2:
                    // 将回答 id 解析为列表
                    String answerId = examQuAnswer.getAnswerId();
                    List<Integer> opIds = Arrays.stream(answerId.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    // 添加选项顺序
                    List<Integer> sorts = new ArrayList<>();
                    for (Integer opId : opIds) {
                        LambdaQueryWrapper<Option> optionLambdaQueryWrapper2 = new LambdaQueryWrapper<>();
                        optionLambdaQueryWrapper2.eq(Option::getId, opId);
                        Option option = optionMapper.selectOne(optionLambdaQueryWrapper2);
                        sorts.add(option.getSort());
                    }
                    // 设置自己选的选项，选项为顺序 1 为 A，2 为 B...
                    List<String> shortList = sorts.stream().map(String::valueOf).collect(Collectors.toList());
                    String myOption = String.join(",", shortList);
                    examRecordDetailVO.setMyOption(myOption);
                    // 查找正确答案
                    LambdaQueryWrapper<Option> optionWrapper1 = new LambdaQueryWrapper<>();
                    optionWrapper1.eq(Option::getIsRight, 1)
                            .eq(Option::getQuId, temp.getId());
                    List<Option> examQuAnswers = optionMapper.selectList(optionWrapper1);
                    // 判断是否正确
                    examRecordDetailVO.setIsRight(1);
                    for (Option temp1 : examQuAnswers) {
                        boolean contains = opIds.contains(temp1.getId());
                        if (!contains) {
                            // 只要有一个答案不是正确的则判断为错误
                            examRecordDetailVO.setIsRight(0);
                            break;
                        }
                    }
                    break;
                case 3:
                    // 查询自己的的选项
                    LambdaQueryWrapper<Option> optionLambdaQueryWrapper3 = new LambdaQueryWrapper<>();
                    optionLambdaQueryWrapper3.eq(Option::getId, examQuAnswer.getAnswerId());
                    Option op3 = optionMapper.selectOne(optionLambdaQueryWrapper3);
                    examRecordDetailVO.setMyOption(Integer.toString(op3.getSort()));
                    // 查询是否正确
                    Option byId3 = optionService.getById(examQuAnswer.getAnswerId());
                    if (byId3.getIsRight() == 1) {
                        examRecordDetailVO.setIsRight(1);
                    } else {
                        examRecordDetailVO.setIsRight(0);
                    }
                    break;
                case 4:
                    examRecordDetailVO.setMyOption(examQuAnswer.getAnswerContent());
                    examRecordDetailVO.setIsRight(examQuAnswer.getIsRight() != null ? examQuAnswer.getIsRight() : -1);
                    break;
                case 5:
                    examRecordDetailVO.setMyOption(examQuAnswer.getAnswerContent());
                    QuestionSubItemsUtil.applyCompoundStudentAnswers(
                            examRecordDetailVO.getSubItemList(), examQuAnswer.getAnswerContent());
                    examRecordDetailVO.setIsRight(examQuAnswer.getIsRight() != null ? examQuAnswer.getIsRight() : -1);
                    break;
                default:
                    break;
            }
            examRecordDetailVOS.add(examRecordDetailVO);
        }

        return Result.success("查询考试的信息成功", examRecordDetailVOS);
    }

    /** 当前用户已练习过的题库分页（实体仍为 {@link Repo} 分页再转 VO）。 */
    @Override
    public Result<IPage<ExerciseRecordVO>> getExerciseRecordPage(Integer pageNum, Integer pageSize ,String repoName) {
        // 创建page对象
        Page<Repo> repoPage = new Page<>(pageNum, pageSize);
        // 查询该用户已考试的考试id
        Integer userId = SecurityUtil.getUserId();
        Page<Repo> exercisePageResult = repoMapper.selectUserExerciseRecord(repoPage,userId,repoName);
        // 实体转换
        Page<ExerciseRecordVO> exerciseRecordVOPage = recordConverter.pageRepoEntityToVo(exercisePageResult);
        return Result.success("查询成功", exerciseRecordVOPage);
    }

    /**
     * 刷题详情：参数 {@code exerciseId} 实为题库 ID，遍历库内试题并结合 {@link ExerciseRecord} 展示作答与正误。
     */
    @Override
    public Result<List<ExerciseRecordDetailVO>> getExerciseRecordDetail(Integer exerciseId) {
        // 1、题干 2、选项 3、自己的答案 4、正确的答案 5、是否正确 6、试题分析
        List<ExerciseRecordDetailVO> exerciseRecordDetailVOS = new ArrayList<>();
        // 查询该考试的试题
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper = new LambdaQueryWrapper<>();
        questionLambdaQueryWrapper.eq(Question::getRepoId, exerciseId);
        List<Question> questions1 = questionMapper.selectList(questionLambdaQueryWrapper);
        for (Question temp : questions1) {
            ExerciseRecordDetailVO exerciseRecordDetailVO = new ExerciseRecordDetailVO();
            exerciseRecordDetailVO.setImage(temp.getImage());
            exerciseRecordDetailVO.setTitle(temp.getContent());
            exerciseRecordDetailVO.setAnalyse(temp.getAnalysis());
            exerciseRecordDetailVO.setQuType(temp.getQuType());
            fillCompoundStemOnExerciseRecordDetail(temp, exerciseRecordDetailVO);
            // 查询试题选项
            LambdaQueryWrapper<Option> optionWrapper = new LambdaQueryWrapper<>();
            optionWrapper.eq(Option::getQuId, temp.getId());
            List<Option> options = optionMapper.selectList(optionWrapper);
            if (temp.getQuType() == 4) {
                exerciseRecordDetailVO.setOption(null);
            } else {
                exerciseRecordDetailVO.setOption(options);
            }

            if (temp.getQuType() == 4 && options.size() > 0) {
                exerciseRecordDetailVO.setRightOption(options.get(0).getContent());
            } else {
                String current = "";
                ArrayList<Integer> strings = new ArrayList<>();
                for (Option temp1 : options) {
                    if (temp1.getIsRight() == 1) {
                        strings.add(temp1.getSort());
                    }
                }
                List<String> stringList = strings.stream().map(String::valueOf).collect(Collectors.toList());
                String result = String.join(",", stringList);

                exerciseRecordDetailVO.setRightOption(result);
            }
            LambdaQueryWrapper<ExerciseRecord> exerciseRecordLambdaQueryWrapper = new LambdaQueryWrapper<>();
            exerciseRecordLambdaQueryWrapper.eq(ExerciseRecord::getUserId, SecurityUtil.getUserId())
                    .eq(ExerciseRecord::getRepoId, exerciseId)
                    .eq(ExerciseRecord::getQuestionId, temp.getId());
            ExerciseRecord exerciseRecord = exerciseRecordMapper.selectOne(exerciseRecordLambdaQueryWrapper);

            // 如果某题没有作答
            if (exerciseRecord == null) {
                exerciseRecordDetailVO.setMyOption(null);
                exerciseRecordDetailVO.setIsRight(-1);
                exerciseRecordDetailVOS.add(exerciseRecordDetailVO);
                continue;
            }
            switch (temp.getQuType()) {
                case 1:
                    // 设置自己的选项
                    LambdaQueryWrapper<Option> optionLambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                    optionLambdaQueryWrapper1.eq(Option::getId, exerciseRecord.getAnswer());
                    Option op1 = optionMapper.selectOne(optionLambdaQueryWrapper1);
                    exerciseRecordDetailVO.setMyOption(Integer.toString(op1.getSort()));
                    // 设置是否正确
                    Option byId1 = optionService.getById(exerciseRecord.getAnswer());
                    if (byId1.getIsRight() == 1) {
                        exerciseRecordDetailVO.setIsRight(1);
                    } else {
                        exerciseRecordDetailVO.setIsRight(0);
                    }
                    break;
                case 2:
                    // 将回答id解析为列表
                    String answerId = exerciseRecord.getAnswer();
                    List<Integer> opIds = Arrays.stream(answerId.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    // 添加选项顺序
                    List<Integer> sorts = new ArrayList<>();
                    for (Integer opId : opIds) {
                        LambdaQueryWrapper<Option> optionLambdaQueryWrapper2 = new LambdaQueryWrapper<>();
                        optionLambdaQueryWrapper2.eq(Option::getId, opId);
                        Option option = optionMapper.selectOne(optionLambdaQueryWrapper2);
                        sorts.add(option.getSort());
                    }
                    // 设置自己选的选项，选项为顺序 1为A，2为B...
                    List<String> shortList = sorts.stream().map(String::valueOf).collect(Collectors.toList());
                    String myOption = String.join(",", shortList);
                    exerciseRecordDetailVO.setMyOption(myOption);
                    // 查找正确答案
                    LambdaQueryWrapper<Option> optionWrapper1 = new LambdaQueryWrapper<>();
                    optionWrapper1.eq(Option::getIsRight, 1)
                            .eq(Option::getQuId, temp.getId());
                    List<Option> examQuAnswers = optionMapper.selectList(optionWrapper1);
                    // 判断是否正确
                    exerciseRecordDetailVO.setIsRight(1);
                    for (Option temp1 : examQuAnswers) {
                        boolean contains = opIds.contains(temp1.getId());
                        if (!contains) {
                            // 只要有一个正确答案不在用户选择中则判断为错误
                            exerciseRecordDetailVO.setIsRight(0);
                            break;
                        }
                    }
                    break;
                case 3:
                    // 查询自己的的选项
                    LambdaQueryWrapper<Option> optionLambdaQueryWrapper3 = new LambdaQueryWrapper<>();
                    optionLambdaQueryWrapper3.eq(Option::getId, exerciseRecord.getAnswer());
                    Option op3 = optionMapper.selectOne(optionLambdaQueryWrapper3);
                    exerciseRecordDetailVO.setMyOption(Integer.toString(op3.getSort()));
                    // 查询是否正确
                    Option byId3 = optionService.getById(exerciseRecord.getAnswer());
                    if (byId3.getIsRight() == 1) {
                        exerciseRecordDetailVO.setIsRight(1);
                    } else {
                        exerciseRecordDetailVO.setIsRight(0);
                    }
                    break;
                case 4:
                    exerciseRecordDetailVO.setMyOption(null);
                    exerciseRecordDetailVO.setIsRight(-1);
                    break;
                default:
                    break;
            }
            exerciseRecordDetailVOS.add(exerciseRecordDetailVO);
        }
        return Result.success("查询刷题详情成功", exerciseRecordDetailVOS);
    }

    /**
     * 提交刷题答案：客观题比对选项集合判分；首答插入 {@link ExerciseRecord} 并维护 {@link UserExerciseRecord} 计数，重复作答则更新。
     */
    @Override
    @Transactional
    public Result<QuestionVO> fillAnswer(ExerciseFillAnswerFrom exerciseFillAnswerFrom) {
        ExerciseRecord exerciseRecord = exerciseConverter.fromToEntity(exerciseFillAnswerFrom);
        //默认用户回答正确
        boolean flag = true;
        exerciseRecord.setIsRight(1);

        //对客观题做题正确与否校验
        if (exerciseFillAnswerFrom.getQuType() != 4) {
            List<Integer> options = Arrays.stream(exerciseRecord.getAnswer().split(","))
                    .map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
            List<Integer> rightOptions = new ArrayList<>();
            optionMapper.selectAllByQuestionId(exerciseRecord.getQuestionId()).forEach(option -> {
                if (option.getIsRight() == 1) {
                    rightOptions.add(option.getId());
                }
            });
            if (options.size() != rightOptions.size()) {
                flag = false;
            } else {
                for (Integer option : options) {
                    if (!rightOptions.contains(option)) {
                        flag = false;
                        exerciseRecord.setIsRight(0);
                        break;
                    }
                }
            }
        }
        if (flag) {
            exerciseRecord.setIsRight(1);
        } else {
            exerciseRecord.setIsRight(0);
        }
        //对是否第一次该题判断
        LambdaQueryWrapper<ExerciseRecord> exerciseRecordLambdaQueryWrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, SecurityUtil.getUserId())
                .eq(ExerciseRecord::getRepoId, exerciseRecord.getRepoId())
                .eq(ExerciseRecord::getQuestionId, exerciseRecord.getQuestionId());
        ExerciseRecord databaseExerciseRecord = exerciseRecordMapper.selectOne(exerciseRecordLambdaQueryWrapper);
        boolean exercised = !Optional.ofNullable(databaseExerciseRecord).isPresent();
        if (exercised) {
            //未做过该题，新增记录
            exerciseRecordMapper.insert(exerciseRecord);
            //获取该题库填作答记录
            LambdaQueryWrapper<UserExerciseRecord> exerciseRecordWrapper = new LambdaQueryWrapper<UserExerciseRecord>()
                    .eq(UserExerciseRecord::getUserId, SecurityUtil.getUserId())
                    .eq(UserExerciseRecord::getRepoId, exerciseRecord.getRepoId());
            UserExerciseRecord userExerciseRecord = userExerciseRecordMapper.selectOne(exerciseRecordWrapper);

            if (!Optional.ofNullable(userExerciseRecord).isPresent()) {
                //该题库用户首次刷题，添加一条记录
                LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>()
                        .eq(Question::getRepoId, exerciseRecord.getRepoId());
                int totalCount = questionMapper.selectCount(questionWrapper).intValue();
                UserExerciseRecord insertUserExerciseRecord = new UserExerciseRecord();
                insertUserExerciseRecord.setExerciseCount(1);
                insertUserExerciseRecord.setRepoId(exerciseRecord.getRepoId());
                insertUserExerciseRecord.setTotalCount(totalCount);
                userExerciseRecordMapper.insert(insertUserExerciseRecord);
            } else {
                //修改题库总数，避免后续新增试题
                LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                        .eq(Question::getId, exerciseRecord.getRepoId());

                //该题库非首次刷题，修改刷题数
                UserExerciseRecord updateUserExerciseRecord = new UserExerciseRecord();
                updateUserExerciseRecord.setTotalCount(questionMapper.selectCount(wrapper).intValue());
                updateUserExerciseRecord.setId(userExerciseRecord.getId());
                updateUserExerciseRecord.setExerciseCount(userExerciseRecord.getExerciseCount() + 1);
                userExerciseRecordMapper.updateById(updateUserExerciseRecord);
            }
        } else {
            //已做过，修改答案
            exerciseRecord.setId(databaseExerciseRecord.getId());
            exerciseRecordMapper.updateById(exerciseRecord);
        }

        //获取试题信息，返回给用户
        QuestionVO questionVO = questionMapper.selectSingle(exerciseRecord.getQuestionId());
        fillCompoundStemOnQuestionVo(questionVO);

        //针对不同题型做出不同响应
        //主观题响应
        if (exerciseRecord.getQuestionType() == 4) {
            return Result.success(null, questionVO);
        }

        return flag ? Result.success("回答正确", questionVO) : Result.success("回答错误", questionVO);
    }

    /** 查询单题详情（含选项等，供刷题页展示）。 */
    @Override
    public Result<QuestionVO> getSingle(Integer id) {
        QuestionVO questionVO = questionMapper.selectDetail(id);
        fillCompoundStemOnQuestionVo(questionVO);
        return Result.success("查询单题成功", questionVO);
    }

    /**
     * 某题库某题的用户作答摘要：合并题干信息与上次作答内容及是否正确。
     */
    @Override
    public Result<AnswerInfoVO> getAnswerInfo(Integer repoId, Integer quId) {
        QuestionVO questionVO = questionMapper.selectSingle(quId);
        fillCompoundStemOnQuestionVo(questionVO);
        AnswerInfoVO answerInfoVO = exerciseConverter.quVOToAnswerInfoVO(questionVO);
        LambdaQueryWrapper<ExerciseRecord> exerciseRecordLambdaQueryWrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getRepoId, repoId)
                .eq(ExerciseRecord::getQuestionId, quId)
                .eq(ExerciseRecord::getUserId, SecurityUtil.getUserId());
        ExerciseRecord exerciseRecord = exerciseRecordMapper.selectOne(exerciseRecordLambdaQueryWrapper);
        answerInfoVO.setAnswerContent(exerciseRecord.getAnswer());
        return exerciseRecord.getIsRight() == 1 ?
                Result.success("回答正确", answerInfoVO) : Result.success("回答错误", answerInfoVO);

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

    private void fillCompoundStemOnExerciseRecordDetail(Question child, ExerciseRecordDetailVO vo) {
        if (child == null || child.getParentQuId() == null) {
            return;
        }
        Question stem = questionMapper.selectById(child.getParentQuId());
        if (stem == null) {
            return;
        }
        vo.setParentQuId(child.getParentQuId());
        vo.setStemContent(stem.getContent());
        vo.setStemImage(stem.getImage());
    }
}