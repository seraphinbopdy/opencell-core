package org.meveo.export;

import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ProductOffering;
import org.meveo.model.cpq.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;


public class DefaultStatusConverter extends ReflectionConverter {

    private Logger log = LoggerFactory.getLogger(this.getClass());



    public DefaultStatusConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public boolean canConvert(Class clazz) {


        return Product.class.isAssignableFrom(clazz) ||
                OfferTemplate.class.isAssignableFrom(clazz) ||
                ProductOffering.class.isAssignableFrom(clazz);
    }

    /**
     * Append class name when serialising an abstract or inheritance class' implementation
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void marshal(Object original, final HierarchicalStreamWriter writer, final MarshallingContext context) {

        if (original instanceof Product) {

            writer.startNode("status");
            writer.setValue(((Product) original).getStatus().getValue());
            writer.endNode();

        }else if (original instanceof  ProductOffering || original instanceof OfferTemplate) {

            writer.startNode("lifeCycleStatus");
            writer.setValue(((ProductOffering) original).getLifeCycleStatus().getValue());
            writer.endNode();
        }
        super.marshal(original, writer, context);
    }


}