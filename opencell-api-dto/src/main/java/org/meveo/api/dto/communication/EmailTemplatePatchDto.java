package org.meveo.api.dto.communication;


import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "EmailTemplate")
@XmlAccessorType(XmlAccessType.FIELD)
public class EmailTemplatePatchDto extends MessageTemplateDto  {

    private static final long serialVersionUID = 1739876218558380262L;

    private String subject;

    private String htmlContent;

    private transient List<TranslatedHtmlContentDto> translatedHtmlContent;

    private transient List<TranslatedSubjectDto> translatedSubject;

    public EmailTemplatePatchDto() {
        super();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public List<TranslatedHtmlContentDto> getTranslatedHtmlContent() {
        return translatedHtmlContent;
    }

    public void setTranslatedHtmlContent(List<TranslatedHtmlContentDto> translatedHtmlContent) {
        this.translatedHtmlContent = translatedHtmlContent;
    }

    public List<TranslatedSubjectDto> getTranslatedSubject() {
        return translatedSubject;
    }

    public void setTranslatedSubject(List<TranslatedSubjectDto> translatedSubject) {
        this.translatedSubject = translatedSubject;
    }


}
