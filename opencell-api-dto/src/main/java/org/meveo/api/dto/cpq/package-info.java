
@XmlJavaTypeAdapters({
	@XmlJavaTypeAdapter(value = DateTimeAdapter.class, type = Date.class),
	@XmlJavaTypeAdapter(value = AttributeTypeEnumAdapter.class, type = AttributeTypeEnum.class)
})
package org.meveo.api.dto.cpq;

import java.util.Date;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.meveo.api.jaxb.AttributeTypeEnumAdapter;
import org.meveo.api.jaxb.DateTimeAdapter;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.meveo.model.cpq.enums.AttributeTypeEnum;
