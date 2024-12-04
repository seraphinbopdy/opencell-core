package org.meveo.apiv2.article;

import org.immutables.value.Value;
import org.meveo.apiv2.models.PaginatedResource;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
public interface AccountingArticles  extends PaginatedResource<AccountingArticle> {

}
