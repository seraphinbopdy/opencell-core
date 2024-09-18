package org.meveo.apiv2.billing.impl;

import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.billing.ImmutableBatchEntity;
import org.meveo.apiv2.billing.resource.BatchEntityResource;
import org.meveo.apiv2.billing.service.BatchEntityApiService;
import org.meveo.apiv2.ordering.common.LinkGenerator;
import org.meveo.model.billing.BatchEntity;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/**
 * An implementation of batch entity resource.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Interceptors({WsRestApiInterceptor.class})
public class BatchEntityResourceImpl implements BatchEntityResource {

    @Inject
    private BatchEntityApiService service;

    BatchEntityMapper mapper = new BatchEntityMapper();

    @Override
    public Response create(org.meveo.apiv2.billing.BatchEntity resource) {
        BatchEntity batchEntity = mapper.toEntity(resource);
        batchEntity = service.create(batchEntity);
        return Response
                .created(LinkGenerator.getUriBuilderFromResource(BatchEntityResource.class, batchEntity.getId()).build())
                .entity(toResourceBatchEntityWithLink(mapper.toResource(batchEntity)))
                .build();
    }

    @Override
    public Response update(Long id, org.meveo.apiv2.billing.BatchEntity resource) {
        BatchEntity batchEntity =
                service.update(id, mapper.toEntity(resource)).orElseThrow(NotFoundException::new);
        return Response
                .created(LinkGenerator.getUriBuilderFromResource(BatchEntityResource.class, batchEntity.getId()).build())
                .entity(toResourceBatchEntityWithLink(mapper.toResource(batchEntity)))
                .build();
    }

    @Override
    public Response delete(Long id, Request request) {
        return service.delete(id)
                .map(batchEntity -> Response.ok().entity(toResourceBatchEntityWithLink(mapper.toResource(batchEntity))).build())
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Response cancel(Long id) {
        if (service.isEligibleToUpdate(id)) {
            service.cancel(id);
        }
        return Response.ok().entity(LinkGenerator.getUriBuilderFromResource(BatchEntityResource.class, id).build())
                .build();
    }

    private org.meveo.apiv2.billing.BatchEntity toResourceBatchEntityWithLink(org.meveo.apiv2.billing.BatchEntity batchEntity) {
        return ImmutableBatchEntity.copyOf(batchEntity)
                .withLinks(new LinkGenerator.SelfLinkGenerator(BatchEntityResource.class).withId(batchEntity.getId())
                        .withGetAction().withPostAction().withPutAction().withPatchAction().withDeleteAction().build());
    }
}