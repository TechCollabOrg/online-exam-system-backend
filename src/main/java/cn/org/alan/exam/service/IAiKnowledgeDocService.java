package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiKnowledgeDocForm;
import cn.org.alan.exam.model.vo.ai.AiKnowledgeDocVO;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.org.alan.exam.model.entity.AiKnowledgeDoc;

import java.util.List;

public interface IAiKnowledgeDocService extends IService<AiKnowledgeDoc> {

    Result<List<AiKnowledgeDocVO>> listAll(String keyword);

    Result<AiKnowledgeDocVO> getDetail(Integer id);

    Result<String> addDoc(AiKnowledgeDocForm form);

    Result<String> updateDoc(Integer id, AiKnowledgeDocForm form);

    Result<String> deleteDoc(Integer id);

    /** 表为空时从 classpath/ai-knowledge/*.md 导入一次（便于迁移旧版） */
    Result<String> importBuiltinIfEmpty();
}
