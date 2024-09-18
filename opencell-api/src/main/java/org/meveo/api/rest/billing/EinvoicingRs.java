package org.meveo.api.rest.billing;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.meveo.api.dto.billing.PdpStatusDto;
import org.meveo.api.rest.IBaseRs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/billing/einvoicing")
@Tag(name = "E-Invoice", description = "@%E-Invoice")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface EinvoicingRs extends IBaseRs {
	
	@Path("pdp-status")
	@POST
	Response creatOrUpdatePdpStatus(PdpStatusDto pdpStatusDto);
}
