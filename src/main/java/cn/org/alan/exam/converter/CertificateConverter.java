package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Certificate;
import cn.org.alan.exam.model.form.cretificate.CertificateForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.stereotype.Component;

/**
 * 证书表单与 {@link Certificate} 实体映射（显式声明证书名称字段对齐）。
 *
 * @author JinXi
 */
@Component
@Mapper(componentModel="spring")
public interface CertificateConverter {

    /**
     * 创建/更新证书时的表单拷贝到实体。
     */
    @Mappings({
            @Mapping(target = "certificateName",source = "certificateName")
    })
    Certificate fromToEntity(CertificateForm certificateForm);
}
