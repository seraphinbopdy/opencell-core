package org.meveo.apiv2.generic;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.meveo.apiv2.JaxRsActivatorApiV2;

@Stateless
public class VersionImpl implements Version {

	@Override
	public Response getVersions() {
		return Response.ok().entity(JaxRsActivatorApiV2.VERSION_INFO).type(MediaType.APPLICATION_JSON_TYPE).build();
	}

}
