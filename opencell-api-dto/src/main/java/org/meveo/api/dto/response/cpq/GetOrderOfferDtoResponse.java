package org.meveo.api.dto.response.cpq;

import org.meveo.api.dto.cpq.order.OrderOfferDto;
import org.meveo.api.dto.response.BaseResponse;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;



/**
 * @author Mbarek-Ay
 *
 */
@SuppressWarnings("serial")
@XmlRootElement(name = "GetOrderOfferDtoResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetOrderOfferDtoResponse extends BaseResponse{

	/**
	 * order offer data
	 */
	private OrderOfferDto orderOfferDto;

	/**
	 * @return the orderOfferDto
	 */
	public OrderOfferDto getOrderOfferDto() {
		return orderOfferDto;
	}

	/**
	 * @param orderOfferDto the orderOfferDto to set
	 */
	public void setOrderOfferDto(OrderOfferDto orderOfferDto) {
		this.orderOfferDto = orderOfferDto;
	}

	 
	
	
}
