package org.meveo.apiv2.catalog.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.cpq.ProductDto;
import org.meveo.api.dto.response.catalog.SimpleChargeProductResponseDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.apiv2.catalog.SimpleRecurrentProductDto;
import org.meveo.apiv2.catalog.service.ProductManagementApiService;
import org.meveo.commons.utils.JsonUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Response;

@Stateless
@Interceptors({ WsRestApiInterceptor.class })
public class ProductManagementRsImpl implements ProductManagementRs {
    
    @Inject
    private ProductManagementApiService productManagementApiService;
    
    @Override
    public Response createProductSimpleOneshot(SimpleOneshotProductDto postData) {

        ActionStatus actionStatus = new ActionStatus();
        productManagementApiService.createProductSimpleOneShot(postData);
        
        return Response.ok(actionStatus).build();
    }

    @Override
    public Response createProductSimpleRecurrent(SimpleRecurrentProductDto postData) {

        ProductDto productSimpleRecurrent = productManagementApiService.createProductSimpleRecurrent(postData);

        SimpleChargeProductResponseDto simpleChargeProductResponseDto = new SimpleChargeProductResponseDto().setProduct(productSimpleRecurrent);
        simpleChargeProductResponseDto.setStatus(ActionStatusEnum.SUCCESS);
        return Response.ok(simpleChargeProductResponseDto).build();
    }
}
