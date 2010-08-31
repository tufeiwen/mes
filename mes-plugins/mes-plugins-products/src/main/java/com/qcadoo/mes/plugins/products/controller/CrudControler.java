package com.qcadoo.mes.plugins.products.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.qcadoo.mes.core.data.api.DataAccessService;
import com.qcadoo.mes.core.data.api.ViewDefinitionService;
import com.qcadoo.mes.core.data.beans.Entity;
import com.qcadoo.mes.core.data.definition.DataDefinition;
import com.qcadoo.mes.core.data.definition.FieldDefinition;
import com.qcadoo.mes.core.data.definition.GridDefinition;
import com.qcadoo.mes.core.data.definition.ViewDefinition;
import com.qcadoo.mes.core.data.definition.ViewElementDefinition;
import com.qcadoo.mes.core.data.internal.types.BelongsToFieldType;
import com.qcadoo.mes.core.data.search.Restrictions;
import com.qcadoo.mes.core.data.search.ResultSet;
import com.qcadoo.mes.core.data.search.SearchCriteria;
import com.qcadoo.mes.core.data.search.SearchCriteriaBuilder;
import com.qcadoo.mes.core.data.types.EnumeratedFieldType;
import com.qcadoo.mes.core.data.types.FieldTypeFactory;
import com.qcadoo.mes.plugins.products.data.ListData;
import com.qcadoo.mes.plugins.products.data.ListDataUtils;
import com.qcadoo.mes.plugins.products.validation.ValidationResult;

@Controller
public class CrudControler {

    @Autowired
    private ViewDefinitionService viewDefinitionService;

    @Autowired
    private DataAccessService dataAccessService;

    @RequestMapping(value = "test/{viewName}", method = RequestMethod.GET)
    public ModelAndView getView(@PathVariable("viewName") String viewName, @RequestParam Map<String, String> arguments) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("crudView");

        ViewDefinition viewDefinition = viewDefinitionService.getViewDefinition(viewName);
        mav.addObject("viewDefinition", viewDefinition);

        Map<String, Entity> entities = new HashMap<String, Entity>();
        Map<String, Map<Long, String>> dictionaryValues = new HashMap<String, Map<Long, String>>();
        Map<String, Long> parentEntities = new HashMap<String, Long>();
        for (ViewElementDefinition viewElement : viewDefinition.getElements()) {
            if (viewElement.getParentDefinition() != null) {
                String argument = arguments.get(viewElement.getParentDefinition().getEntityName());
                if (argument != null) {
                    Long entityId = Long.parseLong(argument);
                    parentEntities.put(viewElement.getParentDefinition().getEntityName(), entityId);
                }
            }
            if (viewElement.getType() == ViewElementDefinition.TYPE_FORM) {
                String argument = arguments.get(viewElement.getDataDefinition().getEntityName());
                if (argument != null) {
                    Long entityId = Long.parseLong(argument);
                    Entity entity = dataAccessService.get(viewElement.getDataDefinition().getEntityName(), entityId);
                    entities.put(viewElement.getName(), entity);
                }
                for (Entry<String, FieldDefinition> fieldDefEntry : viewElement.getDataDefinition().getFields().entrySet()) {
                    switch (fieldDefEntry.getValue().getType().getNumericType()) {
                        case FieldTypeFactory.NUMERIC_TYPE_BELONGS_TO:
                            BelongsToFieldType belongsToField = (BelongsToFieldType) fieldDefEntry.getValue().getType();
                            Map<Long, String> fieldOptions = belongsToField.lookup(null);
                            dictionaryValues.put(fieldDefEntry.getKey(), fieldOptions);
                            break;
                        case FieldTypeFactory.NUMERIC_TYPE_DICTIONARY:
                        case FieldTypeFactory.NUMERIC_TYPE_ENUM:
                            EnumeratedFieldType enumeratedField = (EnumeratedFieldType) fieldDefEntry.getValue().getType();
                            List<String> options = enumeratedField.values();
                            Map<Long, String> fieldOptionsMap = new HashMap<Long, String>();
                            Long key = (long) 0;
                            for (String option : options) {
                                fieldOptionsMap.put(key++, option);
                            }
                            dictionaryValues.put(fieldDefEntry.getKey(), fieldOptionsMap);
                            break;
                    }
                }

            }
        }
        mav.addObject("entities", entities);
        mav.addObject("parentEntities", parentEntities);
        mav.addObject("dictionaryValues", dictionaryValues);

        return mav;
    }

    @RequestMapping(value = "test/{viewName}/{elementName}/list", method = RequestMethod.GET)
    @ResponseBody
    public ListData getGridData(@PathVariable("viewName") String viewName, @PathVariable("elementName") String elementName,
            @RequestParam Map<String, String> arguments) {

        ViewDefinition viewDefinition = viewDefinitionService.getViewDefinition(viewName);
        ViewElementDefinition element = viewDefinition.getElementByName(elementName);

        GridDefinition gridDefinition = (GridDefinition) element;
        DataDefinition dataDefinition = gridDefinition.getDataDefinition();

        SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteriaBuilder.forEntity(dataDefinition.getEntityName());

        if (arguments.get("parentId") != null && gridDefinition.getParentDefinition() != null) {
            Long parentId = Long.parseLong(arguments.get("parentId"));
            System.out.println(parentId + " - " + gridDefinition.getParentField());
            searchCriteriaBuilder = searchCriteriaBuilder.restrictedWith(Restrictions.belongsTo(gridDefinition.getParentField(),
                    parentId));
        }
        /*
         * if (parentEntityId != null && entityParentField != null) { searchCriteriaBuilder =
         * searchCriteriaBuilder.restrictedWith(Restrictions .belongsTo(entityParentField, parentEntityId)); } if (maxResults !=
         * null) { checkArgument(maxResults >= 0, "Max results must be greater or equals 0"); searchCriteriaBuilder =
         * searchCriteriaBuilder.withMaxResults(maxResults); } if (firstResult != null) { checkArgument(firstResult >= 0,
         * "First result must be greater or equals 0"); searchCriteriaBuilder =
         * searchCriteriaBuilder.withFirstResult(firstResult); } if (sortColumn != null && sortOrder != null) { if
         * ("desc".equals(sortOrder)) { searchCriteriaBuilder = searchCriteriaBuilder.orderBy(Order.desc(sortColumn)); } else {
         * searchCriteriaBuilder = searchCriteriaBuilder.orderBy(Order.asc(sortColumn)); } }
         */
        SearchCriteria searchCriteria = searchCriteriaBuilder.build();

        ResultSet rs = dataAccessService.find(dataDefinition.getEntityName(), searchCriteria);

        return ListDataUtils.generateListData(rs, gridDefinition);
    }

    @RequestMapping(value = "test/{viewName}/{elementName}/save", method = RequestMethod.POST)
    @ResponseBody
    public ValidationResult saveEntity(@PathVariable("viewName") String viewName,
            @PathVariable("elementName") String elementName, @ModelAttribute Entity product, Locale locale) {
        ViewDefinition viewDefinition = viewDefinitionService.getViewDefinition(viewName);
        ViewElementDefinition element = viewDefinition.getElementByName(elementName);

        // ValidationResult validationResult = validationUtils.validateEntity(entity, entityDataDefinition.getFields());
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(false);
        validationResult.setGlobalMessage("aaa");
        Map<String, String> fieldMessages = new HashMap<String, String>();
        fieldMessages.put("name", "nie ma");
        validationResult.setFieldMessages(fieldMessages);

        if (validationResult.isValid()) {
            // dataAccessService.save(element.getDataDefinition().getEntityName(), validationResult.getValidEntity());
        }

        // if (locale != null) {
        // translateValidationResult(validationResult, locale);
        // }

        return validationResult;
    }

    @RequestMapping(value = "test/{viewName}/{elementName}/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteEntities(@PathVariable("viewName") String viewName, @PathVariable("elementName") String elementName,
            @RequestBody List<Integer> selectedRows) {
        ViewDefinition viewDefinition = viewDefinitionService.getViewDefinition(viewName);
        ViewElementDefinition element = viewDefinition.getElementByName(elementName);
        for (Integer recordId : selectedRows) {
            // dataAccessService.delete(element.getDataDefinition().getEntityName(), (long) recordId);
        }
        return "ok";

    }
}
