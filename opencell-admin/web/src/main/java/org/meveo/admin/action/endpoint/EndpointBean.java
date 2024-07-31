package org.meveo.admin.action.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.el.ELException;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.admin.ViewBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.api.endpoint.service.EndpointApi;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.endpoint.Endpoint;
import org.meveo.model.endpoint.EndpointParameterMapping;
import org.meveo.model.endpoint.EndpointPathParameter;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.endpoint.EndpointService;
import org.meveo.service.script.Accessor;
import org.meveo.service.script.ScriptUtils;
import org.meveo.web.endpoint.EndpointServlet;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

/**
 * Backend bean to manage a single Endpoint
 */
@Named
@ViewScoped
@ViewBean
public class EndpointBean extends BaseBean<Endpoint> {

    private static final long serialVersionUID = 2605489199518721846L;

    @Inject
    private EndpointService endpointService;

    @Inject
    private EndpointApi endpointApi;

    private DualListModel<String> pathParametersDL;

    private List<EndpointParameterMapping> parameterMappings = new ArrayList<>();

    private List<String> returnedVariableNames;

    private boolean pathDirty = false;

    private List<Accessor> scriptSetters;

    private List<Accessor> scriptGetters;

    @Override
    protected IPersistenceService<Endpoint> getPersistenceService() {
        return endpointService;
    }

    public EndpointBean() {
        super(Endpoint.class);
    }

    @Override
    protected String getDefaultSort() {
        return "code";
    }

    public DualListModel<String> getPathParametersDL() {

        List<String> perksTarget;
        if (pathParametersDL == null) {
            List<String> perksSource = new ArrayList<>();

            if (entity.getService() != null && CollectionUtils.isNotEmpty(scriptSetters)) {
                Set<String> parameterTarget = new HashSet<>();
                perksTarget = new ArrayList<>();
                if (entity.getPathParameters() != null && CollectionUtils.isNotEmpty(entity.getPathParameters())) {
                    entity.getPathParameters().stream().forEach(item -> parameterTarget.add(item.getScriptParameter()));
                }

                for (Accessor setterProperty : scriptSetters) {
                    if (!parameterTarget.contains(setterProperty.getName())) {
                        perksSource.add(setterProperty.getName());

                    } else {
                        perksTarget.add(setterProperty.getName());
                    }
                }
                pathParametersDL = new DualListModel<String>(perksSource, perksTarget);

            } else {
                perksTarget = new ArrayList<>();
                pathParametersDL = new DualListModel<String>(perksSource, perksTarget);
            }

//        } else if (scriptInstance != null && !scriptInstance.getCode().equals(serviceCode)) {
//            List<FunctionIO> functionIOList = scriptInstance.getInputs();
//            List<String> perksSource = new ArrayList<>();
//            if (CollectionUtils.isNotEmpty(functionIOList)) {
//                for (FunctionIO functionIO : functionIOList) {
//                    perksSource.add(functionIO.getName());
//                }
//            }
//            perksTarget = new ArrayList<>();
//            pathParametersDL = new DualListModel<String>(perksSource, perksTarget);
        }

        return pathParametersDL;
    }

    public void setPathParametersDL(DualListModel<String> pathParametersDL) {
        this.pathParametersDL = pathParametersDL;

        List<String> pathParams = this.pathParametersDL.getTarget();
        if (pathParams != null && CollectionUtils.isNotEmpty(pathParams)) {
            if (entity.getPathParameters() == null) {
                entity.setPathParameters(new ArrayList<>());
            }
            final List<EndpointPathParameter> entityPathParameters = new ArrayList<>(entity.getPathParameters());
            pathParams.stream().filter(e -> entityPathParameters.stream().noneMatch(f -> e.equals(f.getScriptParameter()))).collect(Collectors.toList()).forEach(g -> {
                EndpointPathParameter endpointPathParameter = new EndpointPathParameter();
                endpointPathParameter.setScriptParameter(g);
                entity.getPathParameters().add(endpointPathParameter);
            });

            entityPathParameters.stream().filter(e -> pathParams.stream().noneMatch(f -> e.getScriptParameter().equals(f))).collect(Collectors.toList()).forEach(g -> entity.getPathParameters().remove(g));
        }
    }

    public List<EndpointParameterMapping> getParameterMappings() {

        parameterMappings.clear();
        if (pathParametersDL != null && CollectionUtils.isNotEmpty(pathParametersDL.getSource())) {

            Map<String, EndpointParameterMapping> endpointParameterMappingMap = new HashMap<>();
            if (entity.getParametersMapping() != null) {
                entity.getParametersMapping().forEach(item -> endpointParameterMappingMap.put(item.getScriptParameter(), item));
            }

            for (Accessor setterProperty : scriptSetters) {
                if (pathParametersDL.getSource().contains(setterProperty.getName())) {
                    if (endpointParameterMappingMap.containsKey(setterProperty.getName())) {
                        parameterMappings.add(endpointParameterMappingMap.get(setterProperty.getName()));

                    } else {
                        EndpointParameterMapping endpointParameterMapping = new EndpointParameterMapping();
                        endpointParameterMapping.setScriptParameter(setterProperty.getName());
                        endpointParameterMapping.setMultivaluedAsBoolean(setterProperty.isMultivalued());
                        endpointParameterMapping.setDescription(setterProperty.getDescription());
                        parameterMappings.add(endpointParameterMapping);
                    }
                }
            }
        }

        return parameterMappings;
    }

    public void setParameterMappings(List<EndpointParameterMapping> parameterMappings) {
        this.parameterMappings = parameterMappings;
    }

    /**
     * When return variable changes, reset serialize result fields.
     */
    public void onReturnVariableChange() {

        if (entity.getService() == null || entity.getReturnedVariableName() == null) {
            entity.setSerializeResult(false);
            entity.setReturnedValueExample(null);
            entity.setJsonataTransformer(null);
            return;
        }

        String returnType = ScriptUtils.findScriptVariableType(entity.getService(), entity.getReturnedVariableName());
        entity.setSerializeResult(returnType.startsWith("Map") || returnType.startsWith("List"));
    }

    /**
     * When script instance changes, reset returned variable name list, returned variable name and serialize result fields.
     */
    public void onScriptChange() {

        if (entity.getService() != null) {
            scriptSetters = ScriptUtils.getSetters(entity.getService());
            scriptGetters = ScriptUtils.getGetters(entity.getService());
        } else {
            scriptSetters = null;
            scriptGetters = null;
        }

        pathParametersDL = null;
        parameterMappings = new ArrayList<>();
        returnedVariableNames = null;
        entity.setReturnedVariableName(null);
        entity.setSerializeResult(false);
        entity.setPathParameters(null);
        entity.setParametersMapping(null);
        entity.setPath(null);

        getPathParametersDL();
    }

    /**
     * When code changes, update base path.
     */
    public void onCodeChange() {
        entity.setBasePath(StringUtils.cleanupSpecialCharactersAndSpaces(entity.getCode()).toLowerCase());
    }

    /**
     * Get a full Endpoint URL
     * 
     * @return Endpoint URL.
     */
    public String getEndpointUrl() {

        return getEndpointUrl(entity.getEndpointUrl());
    }

    /**
     * Get a full Endpoint URL
     * 
     * @param endpointPath Endpoint path.
     * @return Endpoint URL.
     */
    public String getEndpointUrl(String endpointPath) {

        String OcUrl = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getContextPath();

        return OcUrl + EndpointServlet.REST_PATH + endpointPath;
    }

    /**
     * Get a list of possible returned variable names.
     * 
     * @return List of returned variable names.
     */
    public List<String> getReturnedVariableNames() {
        if (returnedVariableNames == null && entity.getService() != null) {
            returnedVariableNames = ScriptUtils.getGetterPropertyNames(entity.getService());

        }
        return returnedVariableNames;
    }

    public void setReturnedVariableNames(List<String> returnedVariableNames) {
        this.returnedVariableNames = returnedVariableNames;
    }

    @ActionMethod
    @Override
    public String saveOrUpdate(boolean killConversation) throws BusinessException, ELException {
        if (CollectionUtils.isNotEmpty(parameterMappings)) {
            if (entity.getParametersMapping() == null) {
                entity.setParametersMapping(new ArrayList<>());
            }
            final List<EndpointParameterMapping> entityParameterMappings = new ArrayList<>(entity.getParametersMapping());
            parameterMappings.stream().filter(e -> entityParameterMappings.stream().noneMatch(f -> e.equals(f))).collect(Collectors.toList()).forEach(g -> entity.getParametersMapping().add(g));
            entityParameterMappings.stream().filter(e -> parameterMappings.stream().noneMatch(f -> e.equals(f))).collect(Collectors.toList()).forEach(g -> entity.getParametersMapping().remove(g));

        } else if (entity.getParametersMapping() != null) {
            entity.getParametersMapping().clear();
        }

        if (entity.getPathParameters() != null && !(pathParametersDL != null && CollectionUtils.isNotEmpty(pathParametersDL.getTarget()))) {
            entity.getPathParameters().clear();
        }

        String message = entity.isTransient() ? "save.successful" : "update.successful";

        var dto = endpointApi.toDto(entity);
        try {
            entity = endpointApi.createOrUpdate(dto);

            setObjectId(entity.getId());

        } catch (Exception e) {
            messages.error("Entity can't be saved. Please retry.");
            throw new BusinessException(e);
        }
        messages.info(new BundleKey("messages", message));

        if (killConversation) {
            endConversation();
        }

        return getListViewName();
    }

    public void onPathParametersTransfer(TransferEvent event) throws Exception {

        if (!pathDirty) {
            StringBuilder newPath = new StringBuilder("");
            String sep = "/";
            for (EndpointPathParameter endpointPathParameter : entity.getPathParameters()) {
                newPath.append(sep).append("{").append(endpointPathParameter).append("}");
            }

            entity.setPath(newPath.toString());
        }
    }

    public boolean isPathDirty() {
        return pathDirty;
    }

    public void setPathDirty(boolean pathDirty) {
        this.pathDirty = pathDirty;
    }
}