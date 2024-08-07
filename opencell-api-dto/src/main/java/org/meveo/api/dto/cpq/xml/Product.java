package org.meveo.api.dto.cpq.xml;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.dto.cpq.PriceDTO;
import org.meveo.model.quote.QuoteProduct;

@XmlAccessorType(XmlAccessType.FIELD)
public class Product {
	
	@XmlElement
	private String productLine;
	@XmlElement
	private BigDecimal quantity;  
	private List<Attribute> attributes;
	private CustomFieldsDto customFields;
	private List<PriceDTO> prices;
	@XmlElement
	private String code;
	@XmlElement
	private String description;
	
	
	
	public  Product(QuoteProduct quoteProduct , CustomFieldsDto customFields, List<PriceDTO> prices) {
		super();
		org.meveo.model.cpq.Product cpqProduct=quoteProduct.getProductVersion().getProduct();
		if(cpqProduct.getProductLine() != null) {
			this.productLine = quoteProduct.getProductVersion().getProduct().getProductLine().getCode();
		}
	    this.quantity = quoteProduct.getQuantity();
		this.customFields = customFields;
		this.prices = prices;
		this.code=cpqProduct.getCode();
		this.description=cpqProduct.getDescription();
	}
	/**
	 * @return the productLine
	 */
	public String getProductLine() {
		return productLine;
	}
	/**
	 * @param productLine the productLine to set
	 */
	public void setProductLine(String productLine) {
		this.productLine = productLine;
	}
	
 
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	
	public List<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
	public CustomFieldsDto getCustomFields() {
		return customFields;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setCustomFields(CustomFieldsDto customFields) {
		this.customFields = customFields;
	}
	/**
	 * @return the prices
	 */
	public List<PriceDTO> getPrices() {
		return prices;
	}
	/**
	 * @param prices the prices to set
	 */
	public void setPrices(List<PriceDTO> prices) {
		this.prices = prices;
	}
	
	
	
	
	
	
}
