package org.meveo.apiv2.catalog.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.apiv2.catalog.service.ProductManagementApiService;

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
}
