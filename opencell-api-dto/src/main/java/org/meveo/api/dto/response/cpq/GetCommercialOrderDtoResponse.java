package org.meveo.api.dto.response.cpq;

import org.meveo.api.dto.cpq.order.CommercialOrderDto;
import org.meveo.api.dto.response.BaseResponse;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;



/**
 * @author TARIK FA.
 *
 */
@SuppressWarnings("serial")
@XmlRootElement(name = "GetCommercialOrderDtoResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetCommercialOrderDtoResponse extends BaseResponse{

	private CommercialOrderDto commercialOrderDto;

	/**
	 * @return the commercialOrderDto
	 */
	public CommercialOrderDto getCommercialOrderDto() {
		return commercialOrderDto;
	}

	/**
	 * @param commercialOrderDto the commercialOrderDto to set
	 */
	public void setCommercialOrderDto(CommercialOrderDto commercialOrderDto) {
		this.commercialOrderDto = commercialOrderDto;
	}
	

	
	
	
}
