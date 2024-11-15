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
 *
 */
@Entity
@Table(name = "dunning_pause_reasons")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "dunning_pause_reasons_seq"),
        @Parameter(name = "increment_size", value = "1") })
@NamedQueries({
        @NamedQuery(name = "DunningPauseReasons.findByCodeAndDunningSettingCode", query = "select d FROM DunningPauseReason d where d.pauseReason = :pauseReason and d.dunningSettings.code = :dunningSettingsCode") })

public class DunningPauseReason extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    public DunningPauseReason() {
        super();
        // TODO Auto-generated constructor stub
    }

    public DunningPauseReason(@Size(max = 1000) String pauseReason, @Size(max = 255) String description) {
        super();
        this.pauseReason = pauseReason;
        this.description = description;
    }

    /**
     * pause reason
     */
    @Column(name = "pause_reason", nullable = false)
    @Size(max = 255, min = 1)
    @NotNull
    private String pauseReason;

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

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
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
