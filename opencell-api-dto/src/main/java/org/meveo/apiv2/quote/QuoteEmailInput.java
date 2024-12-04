package org.meveo.apiv2.quote;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableQuoteEmailInput.class)
public interface QuoteEmailInput {

    @Schema(description = "Discount plan attached to quote offer")
    @Nullable
    String getQuoteCode();

    @Schema(description = "quote version")
    @NotNull
    Integer getQuoteVersion();

    @Schema(description = "list from users to send email")
    @Nullable
    List<String> getFrom();

    @Schema(description = "list to users to send email")
    @Nullable
    List<String> getTo();

    @Schema(description = "list cc users to send email")
    @Nullable
    List<String> getCc();

    @Schema(description = "subject of email if not provided default subject will be used")
    @Nullable
    String getSubject();

    @Schema(description = "body of email if not provided default body will be used")
    @Nullable
    String getContent();
}
