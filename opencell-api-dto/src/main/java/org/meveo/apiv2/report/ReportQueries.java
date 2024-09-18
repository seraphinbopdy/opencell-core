package org.meveo.apiv2.report;

import org.immutables.value.Value;
import org.meveo.apiv2.models.PaginatedResource;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
public interface ReportQueries extends PaginatedResource<ReportQuery> {
}
