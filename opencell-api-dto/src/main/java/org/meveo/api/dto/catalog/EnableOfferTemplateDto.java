package org.meveo.api.dto.catalog;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


@XmlRootElement(name = "EnableOfferTemplateDto")
@XmlType(name = "EnableOfferTemplateDto")
@XmlAccessorType(XmlAccessType.FIELD)
public class EnableOfferTemplateDto {

	@XmlElement
	@Schema(description = "Provided filters to filter Offer Template")
    private Map<String, Object> filters;

	public Map<String, Object> getFilters() {
		return filters;
	}

	public void setFilters(Map<String, Object> filters) {
		this.filters = filters;
	}
    
}
