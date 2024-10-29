package org.meveo.apiv2.customtable;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.generic.GenericFieldDetails;
import org.meveo.apiv2.generic.ImmutableGenericFieldDetails;

import org.meveo.apiv2.generic.services.GenericFileExportManager;
import org.meveo.apiv2.settings.globalSettings.service.AdvancedSettingsApiService;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.settings.AdvancedSettings;
import org.meveo.service.custom.CustomTableService;

@Interceptors({ WsRestApiInterceptor.class })
public class CustomTableResourceImpl implements CustomTableResource {

	@Inject
	private CustomTableService customTableService;

	@Inject
	private GenericFileExportManager genericExportManager;
	
	@Inject
	private AdvancedSettingsApiService advancedSettingsApiService;

	@Override
	public Response export(String customTableCode, String fileFormat) {
		if (!"CSV".equals(fileFormat) && !"EXCEL".equals(fileFormat)) {
			throw new BadRequestException("Accepted formats for export are (CSV or EXCEL).");
		}

		CustomEntityTemplate cet = ofNullable(customTableService.getCET(customTableCode)).orElseThrow(
				() -> new NotFoundException("The custom table code " + customTableCode + " does not exits"));
		
		List<CustomFieldTemplate> cfts = ofNullable(customTableService.getCFTs(cet)).orElseThrow(
				() -> new NotFoundException("The custom table code " + customTableCode + " does not have custom fields"));
		
		List<Map<String, Object>> data = ofNullable(customTableService.exportCustomTable(cet)).orElseThrow(
				() -> new NotFoundException("The custom table code " + customTableCode + " is empty"));
		
        String fieldsSeparator = advancedSettingsApiService.findByCode("standardExports.fieldsSeparator").map(AdvancedSettings::getValue).map(Pattern::quote).orElse(",");
        String decimalSeparator = advancedSettingsApiService.findByCode("standardExports.decimalSeparator").map(AdvancedSettings::getValue).orElse("."); 
        String fileNameExtension = advancedSettingsApiService.findByCode("standardExports.fileNameExtension").map(AdvancedSettings::getValue).orElse("csv"); 

		String filePath = genericExportManager.export(customTableCode, data, fileFormat, getGenericFieldDetails(cfts), getOrdredColumn(cfts), "FR", fieldsSeparator, decimalSeparator, fileNameExtension);

		return Response.ok()
				.entity("{\"actionStatus\":{\"status\":\"SUCCESS\",\"message\":\"\"}, \"data\":{ \"filePath\":\"" + filePath + "\"}}")
				.build();
	}
	
	private Map<String, GenericFieldDetails> getGenericFieldDetails(List<CustomFieldTemplate> cfts) {
		return cfts.stream().collect(Collectors.toMap(cft -> cft.getDbFieldname(),
				cft -> ImmutableGenericFieldDetails.builder().name(cft.getDbFieldname()).transformation(getTransformationCFT(cft)).build()));
	}
	
	private List<String> getOrdredColumn(List<CustomFieldTemplate> cfts) {
		return cfts.stream().map(cft -> cft.getDbFieldname()).collect(Collectors.toList());
	}
	
	private String getTransformationCFT(CustomFieldTemplate cft) {
		String pattern = null;
		if (cft.getFieldType() == CustomFieldTypeEnum.DATE) {
			pattern = "dd/MM/yyyy";
		}
		return pattern;
	}

}