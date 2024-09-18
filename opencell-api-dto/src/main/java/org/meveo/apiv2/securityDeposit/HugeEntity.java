package org.meveo.apiv2.securityDeposit;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableHugeEntity.class)
public interface HugeEntity {

    String getEntityClass();

    List<String> getHugeLists();

    List<String> getMandatoryFilterFields();
}
