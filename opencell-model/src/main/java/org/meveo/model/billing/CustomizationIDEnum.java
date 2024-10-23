package org.meveo.model.billing;

public enum CustomizationIDEnum {

    URN_CEN_EU("urn:cen.eu:en16931:2017", "Standard EU invoicing compliance"),
    URN_CEN_EU_CONFORMANT("urn:cen.eu:en16931:2017#conformant#urn:ubl.eu:1p0:extended-ctc-fr", "EU invoicing compliance with French-specific extensions");

    private String value;
    private String label;

    CustomizationIDEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static CustomizationIDEnum getFromValue(String dbData) {
        for (CustomizationIDEnum customizationId : CustomizationIDEnum.values()) {
            if (customizationId.getValue().contentEquals(dbData)) {
                return customizationId;
            }
        }
        return CustomizationIDEnum.URN_CEN_EU;
    }

}
