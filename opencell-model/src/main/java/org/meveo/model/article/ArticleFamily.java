package org.meveo.model.article;

import static jakarta.persistence.FetchType.LAZY;

import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.billing.AccountingCode;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "billing_article_family")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @org.hibernate.annotations.Parameter(name = "sequence_name", value = "billing_article_family_seq"),
        @Parameter(name = "increment_size", value = "1") })
@Cacheable
public class ArticleFamily extends BusinessCFEntity {

    private static final long serialVersionUID = 6592652289497255389L;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "accounting_code_id")
    private AccountingCode accountingCode;

    @ManyToOne
    @JoinColumn(name = "article_family_ref_id")
    private ArticleFamily articleFamily;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_i18n", columnDefinition = "jsonb")
    private Map<String, String> descriptionI18n;

    public ArticleFamily() {
    }

    public ArticleFamily(Long id) {
        this.id = id;
    }

    public ArticleFamily(String code, String description, AccountingCode accountingCode) {
        this.code = code;
        this.description = description;
        this.accountingCode = accountingCode;
    }

    public AccountingCode getAccountingCode() {
        return accountingCode;
    }

    public void setAccountingCode(AccountingCode accountingCode) {
        this.accountingCode = accountingCode;
    }

    public ArticleFamily getArticleFamily() {
        return articleFamily;
    }

    public void setArticleFamily(ArticleFamily articleFamily) {
        this.articleFamily = articleFamily;
    }

    public Map<String, String> getDescriptionI18n() {
        return descriptionI18n;
    }

    public void setDescriptionI18n(Map<String, String> descriptionI18n) {
        this.descriptionI18n = descriptionI18n;
    }
}
