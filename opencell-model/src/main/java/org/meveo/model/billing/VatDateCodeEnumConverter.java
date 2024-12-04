package org.meveo.model.billing;

import jakarta.persistence.AttributeConverter;

public class VatDateCodeEnumConverter implements AttributeConverter<VatDateCodeEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(VatDateCodeEnum attribute) {
        return attribute != null ? attribute.getPaidToDays() : null;
    }

    @Override
    public VatDateCodeEnum convertToEntityAttribute(Integer dbData) {
        for (VatDateCodeEnum vatDateCodeEnum : VatDateCodeEnum.values()) {
            if (vatDateCodeEnum.getPaidToDays() == dbData) {
                return vatDateCodeEnum;
            }
        }
        return VatDateCodeEnum.PAID_TO_DATE;
    }
}
