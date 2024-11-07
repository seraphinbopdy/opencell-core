package org.meveo.apiv2.payments.resource;

import static java.util.Optional.ofNullable;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.payments.AccountOperationsDetails;
import org.meveo.apiv2.payments.CustomerBalance;
import org.meveo.apiv2.report.ImmutableSuccessResponse;
import org.meveo.service.payments.impl.CustomerBalanceService;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@Interceptors({ WsRestApiInterceptor.class })
public class CustomerBalanceResourceImpl implements CustomerBalanceResource {

    @Inject
    private CustomerBalanceService customerBalanceService;

    private final CustomerBalanceMapper mapper = new CustomerBalanceMapper();

    @Override
    public Response create(CustomerBalance resource) {
        org.meveo.model.payments.CustomerBalance customerBalance = mapper.toEntity(resource);
        if(customerBalanceService.findByCode(customerBalance.getCode()) != null) {
            throw new BadRequestException("Customer balance with code " + customerBalance.getCode() + " already exists");
        }
        if ((customerBalance.getOccTemplates() == null || customerBalance.getOccTemplates().isEmpty()) && (customerBalance.getBalanceEl() == null || customerBalance.getBalanceEl().isEmpty())) {
            throw new BadRequestException("At least one of the two fields must be filled in - Occ templates list and balanceEL");
        }
        try {
            customerBalanceService.create(customerBalance);
        } catch (BusinessException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        return Response.ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\",\"message\":\"Customer balance successfully created\"},\"id\":"
                        + customerBalance.getId() +"} ")
                .build();

    }

    @Override
    public Response update(Long id, CustomerBalance resource) {
        org.meveo.model.payments.CustomerBalance customerBalance = mapper.toEntity(resource);
        customerBalance.setId(id);
        if ((customerBalance.getOccTemplates() == null || customerBalance.getOccTemplates().isEmpty()) && (customerBalance.getBalanceEl() == null || customerBalance.getBalanceEl().isEmpty())) {
            throw new BadRequestException("At least one of the two fields must be filled in - Occ templates list and balanceEL");
        }
        customerBalanceService.update(customerBalance);
        return Response
                .ok(ImmutableSuccessResponse.builder()
                        .status("SUCCESS")
                        .message("Customer balance successfully updated")
                        .build())
                .build();
    }

    @Override
    public Response delete(Long id) {
        org.meveo.model.payments.CustomerBalance customerBalance =
                ofNullable(customerBalanceService.findById(id)).orElseThrow(()
                        -> new NotFoundException("Customer balance does not exist"));
        if(customerBalance.isDefaultBalance()) {
            throw new BadRequestException("Can not remove default customer balance");
        }
        customerBalanceService.remove(customerBalance);
        return Response
                .ok(ImmutableSuccessResponse.builder()
                        .status("SUCCESS")
                        .message("Customer balance successfully deleted")
                        .build())
                .build();
    }
    
    @Override
    public Response getAccountOperations(AccountOperationsDetails resource) {
        if(resource != null) {
            return Response.ok().entity(customerBalanceService.getAccountOperations(resource)).build();
        } else {
            throw new BadRequestException("The customerBalance and customerAccount are mandatory to get the AccountOperations");
        }
    }
}