package org.meveo.apiv2.billing.impl;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.billing.InvoiceCancellationInput;
import org.meveo.apiv2.billing.resource.InvoicesResource;
import org.meveo.apiv2.billing.service.InvoiceApiService;

import java.util.Map;

@Interceptors({WsRestApiInterceptor.class})
public class InvoicesResourceImpl implements InvoicesResource {

    @Inject
    private InvoiceApiService invoiceApiService;

    @Override
    public Response cancellation(InvoiceCancellationInput invoiceCancellationInput) {
        validateInputFilters(invoiceCancellationInput.getFilters());
        return Response.ok()
                .entity(invoiceApiService.cancelInvoice(invoiceCancellationInput))
                .build();
    }

    private void validateInputFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            throw new BadRequestException("Filter must have at least one filter");
        }
        Integer billingRunId = (Integer) filters.get("billingRun.id");
        if (billingRunId == null) {
            throw new BadRequestException("Filter must have billingRun.id");
        }
    }
}
