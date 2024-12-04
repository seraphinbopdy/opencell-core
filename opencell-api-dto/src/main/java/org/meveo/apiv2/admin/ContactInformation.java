package org.meveo.apiv2.admin;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableContactInformation.class)
public interface ContactInformation extends Resource{

	@Nullable
	String getEmail();

	@Nullable
	String getPhone();

	@Nullable
	String getMobile();

	@Nullable
	String getFax();
	
}
