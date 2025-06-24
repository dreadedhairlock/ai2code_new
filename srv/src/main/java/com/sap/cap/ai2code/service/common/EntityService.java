package com.sap.cap.ai2code.service.common;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.draft.DraftService;

// import customer.ai2code.exception.BusinessException;
/**
 * Generic Entity Service providing reusable CRUD operations for SAP CAP
 * entities. Handles both active entities and draft entities seamlessly.
 */
@Service
public class EntityService {

    /**
     * Select a single record and throw exception if not found
     */
    public <T extends CdsData> T selectSingle(CqnService service, CqnSelect select,
            Class<T> type, String errorMessage) {
        Result result = service.run(select);
        if (result.rowCount() == 0) {
            throw new BusinessException(errorMessage);
        }
        return result.single(type);
    }

    /**
     * Select a single record and return Optional (no exception)
     */
    public <T extends CdsData> Optional<T> selectSingleOptional(CqnService service,
            CqnSelect select, Class<T> type) {
        Result result = service.run(select);
        if (result.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(result.single(type));
    }

    /**
     * Select a list of records
     */
    public <T extends CdsData> List<T> selectList(CqnService service, CqnSelect select, Class<T> type) {
        Result result = service.run(select);
        return result.listOf(type);
    }

    /**
     * Execute select and return raw Result
     */
    public Result select(CqnService service, CqnSelect select) {
        return service.run(select);
    }

    /**
     * Insert a single entity (supports both active and draft)
     */
    public <T extends CdsData, E extends StructuredType<E>> Result insert(
            CqnService service, DraftService draftService, Class<E> entityClass,
            T entity, boolean isActiveEntity) {

        if (isActiveEntity) {
            return service.run(Insert.into(entityClass).entry(entity));
        } else {
            return draftService.newDraft(Insert.into(entityClass).entry(entity));
        }
    }

    /**
     * Insert multiple entities in batch
     */
    // public <T extends CdsData, E extends StructuredType<E>> void batchInsert(
    //         CqnService service, DraftService draftService, Class<E> entityClass,
    //         List<T> entities, boolean isActiveEntity) {

    //     for (T entity : entities) {
    //         insert(service, draftService, entityClass, entity, isActiveEntity);
    //     }
    // }

    /**
     * Update a single entity (supports both active and draft)
     */
    public <T extends CdsData, E extends StructuredType<E>> Result update(
            CqnService service, DraftService draftService, Class<E> entityClass,
            T entity, boolean isActiveEntity) {

        if (isActiveEntity) {
            return service.run(Update.entity(entityClass).entry(entity));
        } else {
            return draftService.patchDraft(Update.entity(entityClass).entry(entity));
        }
    }

    /**
     * Delete a single entity (supports both active and draft)
     */
    public <T extends CdsData, E extends StructuredType<E>> Result delete(
            CqnService service, DraftService draftService, Class<E> entityClass,
            T entity, boolean isActiveEntity) {

        if (isActiveEntity) {
            return service.run(Delete.from(entityClass).matching(entity));
        } else {
            return draftService.cancelDraft(Delete.from(entityClass).matching(entity));
        }
    }

    /**
     * Check if a record exists
     */
    public boolean exists(CqnService service, CqnSelect select) {
        Result result = service.run(select);
        return result.rowCount() > 0;
    }

    /**
     * Get count of records matching the query
     */
    public long count(CqnService service, CqnSelect select) {
        Result result = service.run(select);
        return result.rowCount();
    }

    /**
     * Execute any CQN statement and return result
     */
    public Result executeSelect(CqnService service, CqnSelect select) {
        return service.run(select);
    }

    public Result executeInsert(CqnService service, CqnInsert insert) {
        return service.run(insert);
    }

    public Result executeUpdate(CqnService service, CqnUpdate update) {
        return service.run(update);
    }

    public Result executeDelete(CqnService service, CqnDelete delete) {
        return service.run(delete);
    }
    public <T extends CdsData, E extends StructuredType<E>> void batchInsert(
            CqnService service,
            DraftService serviceDraft,
            Class<E> entityClass,
            List<T> entities,
            String reportId,
            Boolean isActiveEntity) {
        entities.forEach(entity -> {
            // Use reflection to set common properties
            try {
                entity.getClass().getMethod("setReportId", String.class)
                        .invoke(entity, reportId);
                entity.getClass().getMethod("setIsActiveEntity", Boolean.class)
                        .invoke(entity, isActiveEntity);
            } catch (Exception e) {
                throw new BusinessException("Failed_To_Set_Entity_Properties", e); // Changed exception type
            }
            insert(service, serviceDraft, entityClass, entity, isActiveEntity);
        });
    }

    // private <T extends CdsData> void insertEntity(
    // CqnService service,
    // DraftService serviceDraft,
    // T entity,
    // Boolean isActiveEntity) {
    // if (entity instanceof Records) {
    // insertRecord(service, serviceDraft, (Records) entity, isActiveEntity);
    // } else if (entity instanceof Pcls) {
    // insertPcl(service, serviceDraft, (Pcls) entity, isActiveEntity);
    // } else if (entity instanceof ReportFields) {
    // insertReportField(service, serviceDraft, (ReportFields) entity,
    // isActiveEntity);
    // }
    // }

    // public void updateReport(
    // CqnService service,
    // DraftService serviceDraft,
    // Reports report) {
    // if (report.getIsActiveEntity()) {
    // service.run(Update.entity(Reports_.class).data(report));
    // } else {
    // serviceDraft.patchDraft(Update.entity(Reports_.class).data(report));
    // }
    // }
}