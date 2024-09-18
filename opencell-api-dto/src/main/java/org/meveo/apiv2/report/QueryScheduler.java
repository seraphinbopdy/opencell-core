package org.meveo.apiv2.report;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.admin.User;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableQueryScheduler.class)
public interface QueryScheduler extends Resource {

	@Schema(description = "Query scheduler file format")
    String getFileFormat();

    @Schema(description = "Users to notify")
    @Nullable
    List<User> getUsersToNotify();
    
    @Schema(description = "Emails to notify")
    @Nullable
    List<String> getEmailsToNotify();
    
    @Schema(description = "Query scheduler year")
    @Nullable
    String getYear();
    
    @Schema(description = "Query scheduler month")
    @Nullable
    String getMonth();
    
    @Schema(description = "Query scheduler every month")
    boolean getEveryMonth();
    
    @Schema(description = "Query scheduler day of month")
    @Nullable
    String getDayOfMonth();
    
    @Schema(description = "Query scheduler every day of month")
    boolean getEveryDayOfMonth();
    
    @Schema(description = "Query scheduler day of week")
    @Nullable
    String getDayOfWeek();
    
    @Schema(description = "Query scheduler every day of week")
    boolean getEveryDayOfWeek();
    
    @Schema(description = "Query scheduler hour")
    @Nullable
    String getHour();
    
    @Schema(description = "Query scheduler every hour")
    boolean getEveryHour();
    
    @Schema(description = "Query scheduler minute")
    @Nullable
    String getMinute();
    
    @Schema(description = "Query scheduler every minute")
    boolean getEveryMinute();
    
    @Schema(description = "Query scheduler second")
    @Nullable
    String getSecond();
    
    @Schema(description = "Query scheduler every second")
    boolean getEverySecond();
}