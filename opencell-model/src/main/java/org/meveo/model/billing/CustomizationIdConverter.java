package org.meveo.model.billing;

import jakarta.persistence.AttributeConverter;

public class CustomizationIdConverter implements AttributeConverter<Object, String> {

	@Override
	public String convertToDatabaseColumn(Object attribute) {
		return attribute != null ? ((CustomizationIDEnum)attribute).getValue() : null;
	}

	@Override
	public Object convertToEntityAttribute(String dbData) {
		for (CustomizationIDEnum customizationId : CustomizationIDEnum.values()) {
			if (customizationId.getValue().contentEquals(dbData)) {
				return customizationId.getValue();
			}
		}
		return CustomizationIDEnum.URN_CEN_EU.getValue();
	}
}
