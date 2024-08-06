package org.meveo.apiv2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.meveo.apiv2.generic.core.mapper.module.GenericModule;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericJacksonProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public GenericJacksonProvider() {
        mapper = new ObjectMapper();
        GenericModule genericModule = GenericModule.Builder.getBuilder().build();
        mapper.registerModule(genericModule);
        mapper.registerModule(new JaxbAnnotationModule());
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}

