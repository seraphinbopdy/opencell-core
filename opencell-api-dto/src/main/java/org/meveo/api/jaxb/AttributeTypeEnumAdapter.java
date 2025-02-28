package org.meveo.api.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.meveo.model.cpq.enums.AttributeTypeEnum;
import org.meveo.model.shared.DateUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * This is Adaptor class which has main responsibility to convert from java.util.Date to format string of date. For unmarshaling will support a number of formats:
 * DateUtils.DATE_TIME_PATTERN, meveo.dateTimeFormat (defaults to "dd/MM/yyyy HH:mm"), DateUtils.DATE_FORMAT, meveo.dateFormat (defaults to "dd/MM/yyyy")
 *
 * @author Abdellatif BARI
 */
public class AttributeTypeEnumAdapter extends XmlAdapter<String, AttributeTypeEnum> {

    @Override
    public AttributeTypeEnum unmarshal(String attributeTypeEnumData) {
        if (attributeTypeEnumData == null || attributeTypeEnumData.length() == 0) {
            return null;
        } else {
			try {
				var attributeTypeEnum = AttributeTypeEnum.valueOf(attributeTypeEnumData);
				return attributeTypeEnum;
			} catch(IllegalArgumentException e) {
				
				throw new AttributeTypeDeserializationException("Invalid value for 'attributeType': " + attributeTypeEnumData + ". Valid values are : [ " + Arrays.asList(AttributeTypeEnum.values()).stream().map(AttributeTypeEnum::name).collect(Collectors.joining(", ")) + " ]");
			}
        }
    }
    

    @Override
    public String marshal(AttributeTypeEnum object) {
        if (object == null) {
            return null;
        } else {
            return object.name();
        }
    }
}
