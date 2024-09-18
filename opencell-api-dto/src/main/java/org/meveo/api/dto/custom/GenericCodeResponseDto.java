package org.meveo.api.dto.custom;

import static jakarta.xml.bind.annotation.XmlAccessType.FIELD;

import org.meveo.api.dto.response.BaseResponse;

import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "GenericCodeResponse")
@XmlAccessorType(FIELD)
public class GenericCodeResponseDto extends BaseResponse  {

    private String generatedCode;
    private String sequenceType;
    private String pattern;

    public String getGeneratedCode() {
        return generatedCode;
    }

    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}