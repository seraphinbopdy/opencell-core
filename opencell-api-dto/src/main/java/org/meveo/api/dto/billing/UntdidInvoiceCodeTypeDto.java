package org.meveo.api.dto.billing;

import org.meveo.api.dto.BaseEntityDto;
import org.meveo.model.billing.UntdidInvoiceCodeType;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class UntdidInvoiceCodeTypeDto extends BaseEntityDto {
	
	private String code;
	
	private String interpretation16931;
	
	private String name;
	
	

	public UntdidInvoiceCodeTypeDto() {
		super();
	}

	public UntdidInvoiceCodeTypeDto(String code, String interpretation16931, String name) {
		super();
		this.code = code;
		this.interpretation16931 = interpretation16931;
		this.name = name;
	}
	
	public UntdidInvoiceCodeTypeDto(UntdidInvoiceCodeType invoiceCodeType) {
		if(invoiceCodeType != null) {
			this.code = invoiceCodeType.getCode();
			this.interpretation16931 = invoiceCodeType.getInterpretation16931();
			this.name= invoiceCodeType.getName();
		}
	}

	public String getInterpretation16931() {
		return interpretation16931;
	}

	public void setInterpretation16931(String interpretation16931) {
		this.interpretation16931 = interpretation16931;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
