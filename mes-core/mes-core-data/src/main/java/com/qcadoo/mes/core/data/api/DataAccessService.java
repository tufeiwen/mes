package com.qcadoo.mes.core.data.api;

import com.qcadoo.mes.core.data.beans.Entity;
import com.qcadoo.mes.core.data.search.ResultSet;
import com.qcadoo.mes.core.data.search.SearchCriteria;
import com.qcadoo.mes.core.data.validation.ValidationResults;

/**
 * @apiviz.uses com.qcadoo.mes.core.data.beans.Entity
 * @apiviz.uses com.qcadoo.mes.core.data.search.SearchCriteria
 * @apiviz.uses com.qcadoo.mes.core.data.search.ResultSet
 */
public interface DataAccessService {

    ValidationResults save(String entityName, Entity... entities);

    Entity get(String entityName, Long entityId);

    void delete(String entityName, Long entityId);

    ResultSet find(String entityName, SearchCriteria searchCriteria);
}
