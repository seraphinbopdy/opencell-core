package org.meveo.apiv2.generic;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.persistence.criteria.JoinType;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableGenericPagingAndFiltering.class)
public interface GenericPagingAndFiltering {
    @Nullable
    @Value.Default default Set<String> getGenericFields(){ return Collections.emptySet();}
    @Nullable
    @Value.Default default List<GenericFieldDetails> getGenericFieldDetails(){ return Collections.emptyList();}
    @Nullable
    @Value.Default default Set<String> getNestedEntities(){ return Collections.emptySet();}
    @Nullable
    @Value.Default default Long getNestedDepth(){ return 0L;}
    @Nullable
    String getFullTextFilter();
    @Nullable
    @Value.Default default Map<String, Object> getFilters(){ return Collections.emptyMap();}
    @Nullable
    Long getTotal();
    @Nullable
    Long getLimit();
    @Value.Auxiliary default Long getLimitOrDefault(Long defaultValue){
        return getLimit() != null ? getLimit() : defaultValue;
    }
    @Value.Default default Long getOffset(){
        return 0L;
    }
    @Nullable String getSortBy();
    @Nullable String getSortOrder();
    @Nullable
    @Value.Default default Set<String> getExcluding(){ return Collections.emptySet();}
    @Nullable
    @Value.Default default Set<String> getGroupBy(){
        return Collections.emptySet();
    }
    @Nullable
    @Value.Default default Set<String> getHaving(){
        return Collections.emptySet();
    }
    @Nullable
    @Value.Default default Map<String, String> getTranslations(){ return Collections.emptyMap();}
    
    @Nullable
    JoinType getJoinType();

    @Nullable
    @Value.Default default Boolean getForceCount(){ return false; }

    @Value.Default
    default boolean getIsFilter(){
        return false;
    }
}
