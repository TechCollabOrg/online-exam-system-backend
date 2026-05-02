package cn.org.alan.exam.common.group;

/**
 * 证书表单校验分组，用于区分新增模板与后续扩展场景。
 *
 * @author JinXi
 * @since 2024/5/11
 */
public interface CertificateGroup {

    /** 新增证书模板时的 Bean Validation 分组。 */
    interface CertificateInsertGroup extends CertificateGroup {
    }
}
