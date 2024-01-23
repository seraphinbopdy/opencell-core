package org.meveo.api.dto.response.catalog;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.cpq.ProductDto;

public class SimpleChargeProductResponseDto extends ActionStatus {
    private ProductDto product;

    public ProductDto getProduct() {
        return product;
    }

    public SimpleChargeProductResponseDto setProduct(ProductDto product) {
        this.product = product;
        return this;
    }
}
