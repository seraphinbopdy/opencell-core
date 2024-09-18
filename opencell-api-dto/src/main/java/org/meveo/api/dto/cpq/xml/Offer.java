package org.meveo.api.dto.cpq.xml;

import java.util.ArrayList;
import java.util.List;

import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.model.cpq.offer.QuoteOffer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Offer {
	
	@XmlElement
	private String code;
	@XmlElement
	private String description; 
	@XmlElement
	private String name;
	@XmlElement
	private String longDescription; 
	
	 private List<Attribute> attributes;
	 private List<Product> products;
	private CustomFieldsDto customFields;
	
	
	
	
	public Offer(QuoteOffer quoteOffer,CustomFieldsDto customFields) {
		super();
		this.code = quoteOffer.getOfferTemplate().getCode();
		this.description = quoteOffer.getOfferTemplate().getDescription();
		this.longDescription = quoteOffer.getOfferTemplate().getLongDescription();
		this.name=quoteOffer.getOfferTemplate().getName();
		this.customFields = customFields;
	}
	
	

	public void addProduct(Product product) {
		if(products==null) {
			products=new ArrayList<>();
		}
		products.add(product);
	}
	
	/**
	 * @return the productLine
	 */




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


	
	public List<Attribute> getAttributes() {
		return attributes;
	}


	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}


	public CustomFieldsDto getCustomFields() {
		return customFields;
	}
	public void setCustomFields(CustomFieldsDto customFields) {
		this.customFields = customFields;
	}


	public List<Product> getProducts() {
		return products;
	}


	public void setProducts(List<Product> products) {
		this.products = products;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getLongDescription() {
		return longDescription;
	}


	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}
	
	

	



}
