package org.meveo.api.dto.cpq.xml;

import java.util.List;

import org.meveo.model.billing.InvoiceSubCategory;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
@XmlAccessorType(XmlAccessType.FIELD)
public class SubCategory {
    @XmlAttribute
    private String code;
    @XmlAttribute
    private String label;
    @XmlAttribute
    private Integer sortIndex;
    @XmlElementWrapper(name = "accountingArticles")
    @XmlElement(name = "accountingArticle")
    private List<AccountingArticle> articleLines;
    @XmlElementWrapper(name = "accountingArticlesDiscounts")
    @XmlElement(name = "accountingArticleDiscount")
    private List<AccountingArticle> articleLinesDiscounts;

    public SubCategory(InvoiceSubCategory invoiceSubCategory, List<AccountingArticle> articleLines,
                       List<AccountingArticle> articleLinesDiscounts, String tradingLanguage) {
        this.code = invoiceSubCategory.getCode();
        this.label = invoiceSubCategory.getDescriptionI18nNullSafe().get(tradingLanguage) == null ? invoiceSubCategory.getDescription() : invoiceSubCategory.getDescriptionI18n().get(tradingLanguage);
        this.sortIndex = invoiceSubCategory.getSortIndex();
        this.articleLines = articleLines;
        this.articleLinesDiscounts = articleLinesDiscounts;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

	public List<AccountingArticle> getArticleLines() {
		return articleLines;
	}

	public void setArticleLines(List<AccountingArticle> articleLines) {
		this.articleLines = articleLines;
	}

    public List<AccountingArticle> getArticleLinesDiscounts() {
        return articleLinesDiscounts;
    }

    public void setArticleLinesDiscounts(List<AccountingArticle> articleLinesDiscounts) {
        this.articleLinesDiscounts = articleLinesDiscounts;
    }
}
