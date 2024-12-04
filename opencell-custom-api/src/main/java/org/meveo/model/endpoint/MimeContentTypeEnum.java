package org.meveo.model.endpoint;

import org.meveo.admin.exception.InvalidParameterException;

/**
 * REST HTTP request content type
 */
public enum MimeContentTypeEnum {
    /**
     * JSON content type
     */
    APPLICATION_JSON("application/json"),

    /**
     * XML content type
     */
    APPLICATION_XML("application/xml");

    private final String value;

    MimeContentTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * @return Message key for display in GUI
     */
    public String getLabel() {
        return this.getClass().getSimpleName() + "." + this.name();
    }

    /**
     * Get enum from string value
     * 
     * @param mimeType MIME type
     * @return MimeContentType equivalent to a string value of content type
     * @throws InvalidParameterException If MIME type is unknown
     */
    public static MimeContentTypeEnum fromMimeType(String mimeType) throws InvalidParameterException {
        for (MimeContentTypeEnum mimeContentType : MimeContentTypeEnum.values()) {
            if (mimeContentType.getValue().equals(mimeType)) {
                return mimeContentType;
            }
        }
        throw new InvalidParameterException("Unknown MIME type: " + mimeType);
    }
}