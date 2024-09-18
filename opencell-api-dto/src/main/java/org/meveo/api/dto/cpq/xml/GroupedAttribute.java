package org.meveo.api.dto.cpq.xml;

import org.meveo.model.cpq.QuoteAttribute;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class GroupedAttribute {

	private QuoteAttribute attribute;
	
	public GroupedAttribute(org.meveo.model.cpq.QuoteAttribute attribute) { 
      this.attribute=attribute;
	}

	public QuoteAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(QuoteAttribute attribute) {
		this.attribute = attribute;
	}
 
	
	
	
	
}
