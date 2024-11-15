package org.meveo.apiv2.billing;

import org.immutables.value.Value;
import org.meveo.apiv2.models.PaginatedResource;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
public interface Invoices extends PaginatedResource<Invoice> {
}
