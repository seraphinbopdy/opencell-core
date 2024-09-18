package org.meveo.model.settings;

import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.BusinessEntity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;

/**
 * Represents advanced configuration settings.
 */
@Entity
@Cacheable
@Table(name = "advanced_settings")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "advanced_settings_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "AdvancedSettings.getGroupConfiguration", query = "select as from AdvancedSettings as where as.group=:group ", hints = { @QueryHint(name = "org.hibernate.cacheable", value = "true") }) })
public class AdvancedSettings extends BusinessEntity {
    /**
     * Contains the old configuration origin if migrated.
     */
    @Column(name = "origin", length = 255)
    protected String origin;
    /**
     * Indicates the configuration's functional purpose.
     */
    @Column(name = "category", length = 255)
    protected String category;
    /**
     * Indicates the property-related group.
     */
    @Column(name = "property_group", length = 255)
    protected String group;
    /**
     * Contains the property value.
     */
    @Column(name = "property_value", length = 255)
    protected String value;
    /**
     * Contains the property's full class name for casting.
     */
    @Column(name = "property_type", length = 255)
    protected String type;
    /**
     * Translated descriptions in JSON format with language code as a key and translated description as a value
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_i18n", columnDefinition = "jsonb")
    private Map<String, String> descriptionI18n;

    /**
     * Get the old configuration origin.
     *
     * @return The old configuration origin.
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Set the old configuration origin.
     *
     * @param origin The old configuration origin to set.
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Get the configuration's functional purpose.
     *
     * @return The configuration's functional purpose.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the configuration's functional purpose.
     *
     * @param category The configuration's functional purpose to set.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Get the property-related group.
     *
     * @return The property-related group.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Set the property-related group.
     *
     * @param group The property-related group to set.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Get the property value.
     *
     * @return The property value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the property value.
     *
     * @param value The property value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the property's full class name for casting.
     *
     * @return The property's full class name for casting.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the property's full class name for casting.
     *
     * @param type The property's full class name for casting to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getDescriptionI18n() {
        return descriptionI18n;
    }

    public void setDescriptionI18n(Map<String, String> descriptionI18n) {
        this.descriptionI18n = descriptionI18n;
    }
}