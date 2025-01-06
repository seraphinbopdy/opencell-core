/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.api.rest.catalog.impl;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.api.catalog.PricePlanMatrixApi;
import org.meveo.api.catalog.PricePlanMatrixColumnApi;
import org.meveo.api.catalog.PricePlanMatrixLineApi;
import org.meveo.api.catalog.PricePlanMatrixVersionApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.DatePeriodDto;
import org.meveo.api.dto.catalog.TradingPricePlanInputDto;
import org.meveo.api.dto.catalog.TradingPricePlanVersionDto;
import org.meveo.api.dto.catalog.PricePlanMatrixColumnDto;
import org.meveo.api.dto.catalog.PricePlanMatrixDto;
import org.meveo.api.dto.catalog.PricePlanMatrixLineDto;
import org.meveo.api.dto.catalog.PricePlanMatrixVersionDto;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.dto.response.catalog.GetListPricePlanMatrixVersionResponseDto;
import org.meveo.api.dto.response.catalog.GetPricePlanMatrixColumnResponseDto;
import org.meveo.api.dto.response.catalog.GetPricePlanMatrixLineResponseDto;
import org.meveo.api.dto.response.catalog.GetPricePlanMatrixResponseDto;
import org.meveo.api.dto.response.catalog.GetPricePlanMatrixResponseDto.PricePlanData;
import org.meveo.api.dto.response.catalog.GetPricePlanResponseDto;
import org.meveo.api.dto.response.catalog.GetPricePlanVersionResponseDto;
import org.meveo.api.dto.response.catalog.PricePlanMatrixLinesDto;
import org.meveo.api.dto.response.catalog.PricePlanMatrixesResponseDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.catalog.PricePlanRs;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.api.restful.util.GenericPagingAndFilteringUtils;
import org.meveo.apiv2.ordering.common.LinkGenerator;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.PricePlanMatrixColumn;
import org.meveo.model.catalog.PricePlanMatrixLine;
import org.meveo.model.catalog.PricePlanMatrixValue;
import org.meveo.model.catalog.PricePlanMatrixVersion;
import org.meveo.model.cpq.enums.VersionStatusEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.catalog.impl.PricePlanMatrixService;
import org.meveo.service.catalog.impl.PricePlanMatrixVersionService;

/**
 * @author Edward P. Legaspi
 **/
@RequestScoped
@Interceptors({WsRestApiInterceptor.class})
public class PricePlanRsImpl extends BaseRs implements PricePlanRs {

    @Inject
    private PricePlanMatrixApi pricePlanApi;
    @Inject
    private PricePlanMatrixVersionApi pricePlanMatrixVersionApi;
    @Inject
    private PricePlanMatrixColumnApi pricePlanMatrixColumnApi;
    @Inject
    private PricePlanMatrixLineApi pricePlanMatrixLineApi;
    @Inject
    private PricePlanMatrixVersionService pricePlanMatrixVersionService;    
    @Inject
    private PricePlanMatrixService pricePlanMatrixService;

    @Override
    public ActionStatus create(PricePlanMatrixDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
        	PricePlanMatrix pricePlanMatrix=pricePlanApi.create(postData);
        	if(pricePlanMatrix!=null) {
        		result.setEntityId(pricePlanMatrix.getId());
        		result.setEntityCode(pricePlanMatrix.getCode());
        	}
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus update(PricePlanMatrixDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            pricePlanApi.update(postData);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public GetPricePlanResponseDto find(String pricePlanCode, boolean returnPricePlanMatrixLine) {
        GetPricePlanResponseDto result = new GetPricePlanResponseDto();

        try {
            result.setPricePlan(pricePlanApi.find(pricePlanCode, returnPricePlanMatrixLine));
        } catch (Exception e) {
            processException(e, result.getActionStatus());
        }

        return result;
    }

    @Override
    public ActionStatus remove(String pricePlanCode) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            pricePlanApi.remove(pricePlanCode);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public PricePlanMatrixesResponseDto listPricePlanByEventCode(String eventCode) {
        PricePlanMatrixesResponseDto result = new PricePlanMatrixesResponseDto();

        try {
            result.getPricePlanMatrixes().setPricePlanMatrix(pricePlanApi.list(eventCode));
        } catch (Exception e) {
            processException(e, result.getActionStatus());
        }

        return result;
    }

    @Override
    public PricePlanMatrixesResponseDto listGetAll() {

        PricePlanMatrixesResponseDto result = new PricePlanMatrixesResponseDto();

        try {
            result = pricePlanApi.list(GenericPagingAndFilteringUtils.getInstance().getPagingAndFiltering());
        } catch (Exception e) {
            processException(e, result.getActionStatus());
        }

        return result;
    }

    @Override
    public ActionStatus createOrUpdate(PricePlanMatrixDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            PricePlanMatrix pricePlanMatrix = pricePlanApi.createOrUpdate(postData);
            if(pricePlanMatrix != null) {
        		result.setEntityId(pricePlanMatrix.getId());
        		result.setEntityCode(pricePlanMatrix.getCode());
        	}
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus enable(String code) {
        ActionStatus result = new ActionStatus();

        try {
            pricePlanApi.enableOrDisable(code, true);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus disable(String code) {
        ActionStatus result = new ActionStatus();

        try {
            pricePlanApi.enableOrDisable(code, false);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public Response createOrUpdateMatrixPricePlanVersion(PricePlanMatrixVersionDto pricePlanMatrixVersionDto) {
        try {
            PricePlanMatrixVersion pricePlanMatrixVersion = pricePlanMatrixVersionApi.createOrUpdate(pricePlanMatrixVersionDto);
            if (pricePlanMatrixVersionDto.getColumns() != null) {
                for (PricePlanMatrixColumnDto columnDto : pricePlanMatrixVersionDto.getColumns()) {
                    try {
                        pricePlanMatrixColumnApi.create(pricePlanMatrixVersionDto.getPricePlanMatrixCode(),pricePlanMatrixVersionDto.getVersion(), columnDto);
                    }catch(EntityAlreadyExistsException exp){
                        pricePlanMatrixColumnApi.update(pricePlanMatrixVersionDto.getPricePlanMatrixCode(),pricePlanMatrixVersionDto.getVersion(), columnDto);
                    }
                }
            }
            if (pricePlanMatrixVersionDto.getLines() != null) {
                for (PricePlanMatrixLineDto line : pricePlanMatrixVersionDto.getLines()) {
                    pricePlanMatrixLineApi.addPricePlanMatrixLine(pricePlanMatrixVersionDto.getPricePlanMatrixCode(), pricePlanMatrixVersion.getCurrentVersion(), line);
                }
            }
            return Response.ok(new GetPricePlanVersionResponseDto(pricePlanMatrixVersionApi.load(pricePlanMatrixVersion.getId()))).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, new GetPricePlanVersionResponseDto().getActionStatus());
        }
    }
    
    @Override
    public Response updateMatrixPricePlanVersion(PricePlanMatrixVersionDto pricePlanMatrixVersionDto) {
        try {
            PricePlanMatrixVersion pricePlanMatrixVersion = pricePlanMatrixVersionApi.updatePricePlanMatrixVersion(pricePlanMatrixVersionDto);
            if (pricePlanMatrixVersionDto.getColumns() != null) {
                for (PricePlanMatrixColumnDto columnDto : pricePlanMatrixVersionDto.getColumns()) {
                    try {
                        pricePlanMatrixColumnApi.create(pricePlanMatrixVersionDto.getPricePlanMatrixCode(),pricePlanMatrixVersionDto.getVersion(), columnDto);
                    }catch(EntityAlreadyExistsException exp){
                        pricePlanMatrixColumnApi.update(pricePlanMatrixVersionDto.getPricePlanMatrixCode(),pricePlanMatrixVersionDto.getVersion(), columnDto);
                    }
                }
            }
            if (pricePlanMatrixVersionDto.getLines() != null) {
                for (PricePlanMatrixLineDto line : pricePlanMatrixVersionDto.getLines()) {
                    pricePlanMatrixLineApi.addPricePlanMatrixLine(pricePlanMatrixVersionDto.getPricePlanMatrixCode(), pricePlanMatrixVersion.getCurrentVersion(), line);
                }
            }
            return Response.ok(new GetPricePlanVersionResponseDto(pricePlanMatrixVersionApi.load(pricePlanMatrixVersion.getId()))).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, new GetPricePlanVersionResponseDto().getActionStatus());
        }
    }


    @Override
    public Response removeMatrixPricePlanVersion(String pricePlanMatrixCode, int pricePlanMatrixVersion) {
        ActionStatus result = new ActionStatus();
        try {
            pricePlanMatrixVersionApi.removePricePlanMatrixVersion(pricePlanMatrixCode, pricePlanMatrixVersion);
            return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result);
        }
    }

    @Override
    public Response updatePricePlanMatrixVersionStatus(String pricePlanMatrixCode, int pricePlanMatrixVersion, VersionStatusEnum status) {
        GetPricePlanVersionResponseDto result = new GetPricePlanVersionResponseDto();
        try {
            result = pricePlanMatrixVersionApi.updateProductVersionStatus(pricePlanMatrixCode, pricePlanMatrixVersion, status);
            return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result.getActionStatus());
        }
    }

    @Override
    public Response listPricePlanMatrixVersions(PagingAndFiltering pagingAndFiltering) {
        try {
            GetListPricePlanMatrixVersionResponseDto result = pricePlanMatrixVersionApi.listPricePlanMatrixVersions(pagingAndFiltering);
            return Response.ok(result).build();
        }catch (MeveoApiException e) {
            return errorResponse(e, new GetListPricePlanMatrixVersionResponseDto().getActionStatus());
        }
    }

    @Override
    public Response duplicatePricePlanVersion(String pricePlanMatrixCode, int pricePlanMatrixVersion,DatePeriodDto periodDto) {
        GetPricePlanVersionResponseDto result = new GetPricePlanVersionResponseDto();
        try {
            result = pricePlanMatrixVersionApi.duplicatePricePlanMatrixVersion(pricePlanMatrixCode, pricePlanMatrixVersion,periodDto);
            return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result.getActionStatus());
        }
    }

    @Override
    public Response create(String pricePlanMatrixCode, int pricePlanMatrixVersion, PricePlanMatrixColumnDto postData) {
        GetPricePlanMatrixColumnResponseDto response = new GetPricePlanMatrixColumnResponseDto();
        try {
            PricePlanMatrixColumnDto pricePlanMatrixColumn = pricePlanMatrixColumnApi.create(pricePlanMatrixCode, pricePlanMatrixVersion, postData);
            response.setPricePlanMatrixColumnDto(pricePlanMatrixColumn);
            return Response.created(LinkGenerator.getUriBuilderFromResource(PricePlanRs.class, pricePlanMatrixColumn.getId()).build())
                    .entity(response)
                    .build();
        } catch (MeveoApiException e) {
            return errorResponse(e, response.getActionStatus());
        }
    }

    @Override
    public Response update(String pricePlanMatrixCode, int pricePlanMatrixVersion,PricePlanMatrixColumnDto postData) {
        GetPricePlanMatrixColumnResponseDto response = new GetPricePlanMatrixColumnResponseDto();
        try {
            PricePlanMatrixColumn pricePlanMatrixColumn = pricePlanMatrixColumnApi.update(pricePlanMatrixCode, pricePlanMatrixVersion, postData);
            response.setPricePlanMatrixColumnDto(new PricePlanMatrixColumnDto(pricePlanMatrixColumn));
            return Response.ok(response).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, response.getActionStatus());
        }
    }

    @Override
    public Response findPricePlanMatrixColumn(String pricePlanMatrixColumnCode) {
        GetPricePlanMatrixColumnResponseDto response = new GetPricePlanMatrixColumnResponseDto();
        try {
            PricePlanMatrixColumnDto pricePlanMatrixColumnDto = pricePlanMatrixColumnApi.find(pricePlanMatrixColumnCode);
            response.setPricePlanMatrixColumnDto(pricePlanMatrixColumnDto);
            return Response.ok(response).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, response.getActionStatus());
        }
    }

    @Override
    public Response removePricePlanMatrixColumnCode(String pricePlanMatrixCode, int pricePlanMatrixVersion, String pricePlanMatrixColumnCode) {
        ActionStatus result = new ActionStatus();
        try {
            pricePlanMatrixColumnApi.removePricePlanColumn( pricePlanMatrixCode, pricePlanMatrixVersion,pricePlanMatrixColumnCode);
            return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result);
        }
    }


    @Override
    public Response addPricePlanMatrixLines(String pricePlanMatrixCode, int pricePlanMatrixVersion,PricePlanMatrixLinesDto pricePlanMatrixLinesDto) {
    	GetPricePlanVersionResponseDto result = new GetPricePlanVersionResponseDto();
        try {
        	result = pricePlanMatrixLineApi.addPricePlanMatrixLines(pricePlanMatrixCode, pricePlanMatrixVersion, pricePlanMatrixLinesDto);

        	  return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result.getActionStatus());
        }
    }

    @Override
    public ActionStatus removePricePlanMatrixLine(Long ppmLineId) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            pricePlanMatrixLineApi.remove(ppmLineId);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;

    }

    @Override
    public ActionStatus removePricePlanMatrixLines(PricePlanMatrixLinesDto pricePlanMatrixLinesDto) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        try {
            pricePlanMatrixLineApi.remove(pricePlanMatrixLinesDto);
        } catch (Exception e) {
            processException(e, result);
        }
        return result;
    }
    
    @Override
    public Response getPricePlanMatrixLine(Long ppmLineId) {
        GetPricePlanMatrixLineResponseDto response = new GetPricePlanMatrixLineResponseDto();
        try {

            PricePlanMatrixLineDto pricePlanMatrixLineDto = pricePlanMatrixLineApi.load(ppmLineId);
            response.setPricePlanMatrixLineDto(pricePlanMatrixLineDto);
            return Response.ok(response).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, response.getActionStatus());
        }
    }

	@Override
	public Response updatePricePlanMatrixLines(String pricePlanMatrixCode, int pricePlanMatrixVersion, PricePlanMatrixLinesDto pricePlanMatrixLinesDto) {
		GetPricePlanVersionResponseDto result = new GetPricePlanVersionResponseDto();
        try {
        	result = pricePlanMatrixLineApi.updatePricePlanMatrixLines(pricePlanMatrixCode, pricePlanMatrixVersion, pricePlanMatrixLinesDto);

        	  return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result.getActionStatus());
        }
	}
	
	@Override
    public Response updateWithoutDeletePricePlanMatrixLines(String pricePlanMatrixCode, int pricePlanMatrixVersion, PricePlanMatrixLinesDto pricePlanMatrixLinesDto) {
        GetPricePlanVersionResponseDto result = new GetPricePlanVersionResponseDto();
        try {
            result = pricePlanMatrixLineApi.updateWithoutDeletePricePlanMatrixLines(pricePlanMatrixCode, pricePlanMatrixVersion, pricePlanMatrixLinesDto);
            return Response.ok(result).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, result.getActionStatus());
        }
    }
	
	@Override
	public Response duplicatePricePlan(String pricePlanMatrixCode, String pricePlanMatrixNewCode, int version, String priceVersionType) {
		  GetPricePlanResponseDto result = new GetPricePlanResponseDto();

	        try {
	            result.setPricePlan(pricePlanApi.duplicatePricePlan(pricePlanMatrixCode, pricePlanMatrixNewCode, version, priceVersionType));
	        } catch (Exception e) {
	            processException(e, result.getActionStatus());
	        }

	        return Response.ok(result).build();
	}

    @Override
    public Response deleteTradingPricePlanMatrixLines(Long pricePlanMatrixVersion, String tradingCurrencyCode) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        try {
            pricePlanMatrixVersionApi.removeAllTradingPricePlanLinesByVersion(pricePlanMatrixVersion, tradingCurrencyCode);
        } catch (MeveoApiException e) {
            return errorResponse(e, result);
        }
        return Response.ok(result).build();
    }
    
	public Response createTradingPricePlanVersion(TradingPricePlanVersionDto postData) {
		ActionStatus result = new ActionStatus();
		try {
            result.setEntityId(pricePlanMatrixVersionApi.createTradingPricePlanVersion(postData).getId());
			return Response.ok(result).build();
		} catch (Exception e) {
			processException(e, result);
		}
		return Response.ok(result).build();

	}

	@Override
    public Response updateTradingPricePlanVersion(Long cppvId, TradingPricePlanVersionDto postData) {
        ActionStatus result = new ActionStatus();
        try {
            pricePlanMatrixVersionApi.updateTradingPricePlanVersion(cppvId, postData);
        } catch (Exception e) {
            processException(e, result);
        }
        return Response.ok(result).build();
    }

	@Override
	public Response deleteTradingPricePlanVersion(Long cppvId) {
		ActionStatus result = new ActionStatus();
		try {
            pricePlanMatrixVersionApi.deleteTradingPricePlanVersion(cppvId);
			return Response.ok(result).build();
		} catch (Exception e) {
			processException(e, result);
		}
		return Response.ok(result).build();
	}

	@Override
	public Response disableAllTradingPricePlan(TradingPricePlanInputDto tradingPricePlanInputDto) {
		ActionStatus result = new ActionStatus();
		try {
            pricePlanMatrixVersionApi.disableAllTradingPricePlan(tradingPricePlanInputDto);
			return Response.ok(result).build();
		} catch (Exception e) {
			processException(e, result);
		}
		return Response.ok(result).build();
	}

	@Override
	public Response enableAllTradingPricePlan(TradingPricePlanInputDto tradingPricePlanInputDto) {
		ActionStatus result = new ActionStatus();
		try {
            pricePlanMatrixVersionApi.enableAllTradingPricePlan(tradingPricePlanInputDto);
			return Response.ok(result).build();
		} catch (Exception e) {
			processException(e, result);
		}
		return Response.ok(result).build();
	}

	@Override
	public Response enableTradingVersionPricePlan(Long tradingPricePlanVersionId) {
		ActionStatus result = new ActionStatus();
		try {
            pricePlanMatrixVersionApi.enableTradingVersionPricePlan(tradingPricePlanVersionId);
			return Response.ok(result).build();
		} catch (Exception e) {
			processException(e, result);
		}
		return Response.ok(result).build();
	}

	@Override
	public Response disableTradingVersionPricePlan(Long tradingPricePlanVersionId) {
		ActionStatus result = new ActionStatus();
		try {
            pricePlanMatrixVersionApi.disableTradingVersionPricePlan(tradingPricePlanVersionId);
			return Response.ok(result).build();
		} catch (Exception e) {
			processException(e, result);
		}
		return Response.ok(result).build();
	}

    @Override
    public Response calculateTradingPricePlanMatrixLine(TradingPricePlanVersionDto postData) {
        ActionStatus result = new ActionStatus();
        try {
            pricePlanMatrixVersionApi.calculateTradingPricePlanMatrixLine(postData);
            return Response.ok(result).build();
        } catch (Exception e) {
            processException(e, result);
        }
        return Response.ok(result).build();
    }
    
    @Override
    public Response getPricePlanMatrixByCharge(String chargeCode, String pricePlanCode) {
        GetPricePlanMatrixResponseDto result = new GetPricePlanMatrixResponseDto();
        
        try {
            // Get price plans for charge code
            List<PricePlanMatrix> pricePlans = pricePlanMatrixService.listByChargeCode(chargeCode);
            
            if (pricePlans == null || pricePlans.isEmpty()) {
                throw new EntityDoesNotExistsException(PricePlanMatrix.class, 
                    String.format("No price plan found for charge code '%s'", chargeCode));
            }
            result.setChargeCode(chargeCode);
            // Filter by price plan code or get latest published version
            PricePlanMatrix pricePlan;
            if (pricePlanCode != null) {
                pricePlan = pricePlans.stream()
                    .filter(pp -> pp.getCode().equals(pricePlanCode))
                    .findFirst()
                    .orElseThrow(() -> new EntityDoesNotExistsException(PricePlanMatrix.class, 
                        String.format("Price plan with code '%s' not found", pricePlanCode)));
            } else {
                // Get the price plan with latest published version
                pricePlan = pricePlans.get(0); 
                PricePlanMatrixVersion publishedVersion = pricePlanMatrixVersionService.getLastPublishedVersion(pricePlan.getCode());
                if (publishedVersion != null) {
                    pricePlan = publishedVersion.getPricePlanMatrix();
                }
            }
            // Build response
            PricePlanData pricePlanData = new PricePlanData();
            pricePlanData.setCode(pricePlan.getCode());
            
            try {
                PricePlanMatrixVersion version = pricePlanMatrixVersionService.getLastPricePlanMatrixtVersionWithCollections(pricePlan.getCode());
                if (version != null) {
                    pricePlanData.setVersion(version.getCurrentVersion());
                    pricePlanData.setPrice(version.getPrice());
                    pricePlanData.setPriceEl(version.getPriceEL());
                    pricePlanData.setStartDate(version.getValidity() != null ? version.getValidity().getFrom() : null);
                    pricePlanData.setEndDate(version.getValidity() != null ? version.getValidity().getTo() : null);
                    pricePlanData.setStatus(version.getStatus());
                    
                    // Get columns
                    pricePlanData.setColumns(version.getColumns().stream()
                        .map(PricePlanMatrixColumn::getCode)
                        .collect(Collectors.toList()));
                    // Build matrix data
                    Map<String, Map<String, String>> matrix = new HashMap<>();
                    for (PricePlanMatrixLine line : version.getLines()) {
                        Map<String, String> lineValues = new HashMap<>();
                        
                        for (PricePlanMatrixValue value : line.getPricePlanMatrixValues()) {
                            String columnCode = value.getPricePlanMatrixColumn().getCode();
                            String cellValue = resolvePricePlanValue(value);
                            lineValues.put(columnCode, cellValue);
                        }
                        lineValues.put("description", line.getDescription());
                        lineValues.put("price", line.getValue() != null ? line.getValue().toString() : null);
                        lineValues.put("priceEl", line.getValueEL());
                        
                        matrix.put(line.getId().toString(), lineValues);
                    }
                    pricePlanData.setMatrix(matrix);
                }
            } catch (Exception e) {
                log.error("Error loading price plan version for {}: {}", pricePlan.getCode(), e.getMessage());
                throw new BusinessApiException("Error loading price plan data: " + e.getMessage());
            }
            
            result.setPricePlan(pricePlanData);
            return Response.ok(result).build();
        } catch (EntityDoesNotExistsException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ActionStatus(ActionStatusEnum.FAIL, e.getMessage()))
                .build();
        } catch (Exception e) {
            if (e instanceof BusinessApiException) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ActionStatus(ActionStatusEnum.FAIL, e.getMessage()))
                    .build();
            }
            return errorResponse(new BusinessApiException(e.getMessage()), result.getActionStatus());
        }
    }
    private String resolvePricePlanValue(PricePlanMatrixValue value) {
        // Handle range values first
        if (value.getFromDateValue() != null || value.getToDateValue() != null) {
            // Date range format: "fromDate|toDate"
            return (DateUtils.formatDateWithPattern(value.getFromDateValue(), "yyyy-MM-dd") + "|" + 
                    DateUtils.formatDateWithPattern(value.getToDateValue(), "yyyy-MM-dd"))
                    .replaceAll("null", "");
        }
        
        if (value.getFromDoubleValue() != null || value.getToDoubleValue() != null) {
            // Numeric range format: "fromValue|toValue" 
            return (value.getFromDoubleValue() + "|" + value.getToDoubleValue())
                    .replaceAll("null", ""); 
        }
        // Handle single values
        if (value.getStringValue() != null) {
            return value.getStringValue();
        } else if (value.getDoubleValue() != null) {
            return value.getDoubleValue().toString();
        } else if (value.getLongValue() != null) {
            return value.getLongValue().toString();
        } else if (value.getDateValue() != null) {
            return DateUtils.formatDateWithPattern(value.getDateValue(), "yyyy-MM-dd");
        } else if (value.getBooleanValue() != null) {
            return value.getBooleanValue().toString();
        }
        
        return null;
    }
}
