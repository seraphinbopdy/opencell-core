package org.meveo.model.dunning;

import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author Mbarek-Ay
 * @version 11.0
 */
@Entity
@Table(name = "dunning_stop_reasons")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "dunning_stop_reasons_seq"),
        @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "DunningStopReason.findByStopReason", query = "select d FROM DunningStopReason d where d.stopReason = :stopReason"),
        @NamedQuery(name = "DunningStopReason.findByCodeAndDunningSettingCode", query = "select d FROM DunningStopReason d where d.stopReason = :stopReason and d.dunningSettings.code = :dunningSettingsCode") })
public class DunningStopReason extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    public DunningStopReason() {
        super();
    }

    public DunningStopReason(String stopReason, String description, DunningSettings dunningSettings) {
        super();
        this.stopReason = stopReason;
        this.description = description;
        this.dunningSettings = dunningSettings;
    }

    /**
     * stop reason
     */
    @Column(name = "stop_reason", nullable = false)
    @Size(max = 255, min = 1)
    @NotNull
    private String stopReason;

    /**
     * description
     */
    @Column(name = "description", length = 255)
    @Size(max = 255)
    private String description;

    /**
     * description i18n: internationalization of the description
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_i18n", columnDefinition = "jsonb")
    private Map<String, String> descriptionI18n;

    /**
     * dunning settings associated to the entity
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dunning_settings_id", nullable = false, referencedColumnName = "id")
    @NotNull
    private DunningSettings dunningSettings;

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getDescriptionI18n() {
        return descriptionI18n;
    }

    public void setDescriptionI18n(Map<String, String> descriptionI18n) {
        this.descriptionI18n = descriptionI18n;
    }

    public DunningSettings getDunningSettings() {
        return dunningSettings;
    }

    public void setDunningSettings(DunningSettings dunningSettings) {
        this.dunningSettings = dunningSettings;
    }

}
