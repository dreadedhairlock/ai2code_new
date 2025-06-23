package com.sap.cap.ai2code.service.impl;

import java.util.List;

// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContext;
// import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Insert;

import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.draft.DraftService;

// import cds.gen.CqnService.CqnService;
// import cds.gen.DraftServiceAdministrativeData_;
// import cds.gen.CqnService.ParameterItems;
// import cds.gen.CqnService.ParameterItems_;
// import cds.gen.CqnService.Pcls;
// import cds.gen.CqnService.Pcls_;
// import cds.gen.CqnService.Records;
// import cds.gen.CqnService.Records_;
// import cds.gen.CqnService.ReportFields;
// import cds.gen.CqnService.ReportFields_;
// import cds.gen.CqnService.Reports;
// import cds.gen.CqnService.Reports_;
// import com.sap.cap.ai2code.exception.BusinessException; // Changed from AIServiceException

// Rename from EntityServiceUtil.java
@Service
public class EntityService {
    // Xsuaa
    // private final UserInfo userInfo;
    // // private final RequestContextHolder requestContextHolder;

    // public EntityService(UserInfo userInfo) {
    // this.userInfo = userInfo;
    // // this.requestContextHolder = requestContextHolder;
    // }

    // Select operations
    public <T extends CdsData> T selectSingle(CqnService service, CqnSelect select, Class<T> type,
            String errorMessage) {
        Result result = service.run(select);
        if (result.rowCount() == 0) {
            // throw new BusinessException(errorMessage); // Changed exception type
        }
        return result.single(type);
    }

    public <T extends CdsData> List<T> selectList(CqnService service, CqnSelect select, Class<T> type) {
        Result result = service.run(select);
        return result.listOf(type);
    }

    public Result select(CqnService service, CqnSelect select) {
        return service.run(select);
    }

    // public Records selectRecordById(CqnService service, String reportId, boolean
    // isActiveEntity) {
    // CqnSelect select = Select.from(Records_.class)
    // .where(b -> {
    // /** 前面RestController里已经手动设置了SecurityContext，CAP
    // 的Security上下文也自动继承了,这边不需要再额外判断了。 */

    // // if (isNonCapFrameworkCallInDraft(isActiveEntity)) {
    // // return b.report_ID().eq(reportId).and(
    // // b.DraftAdministrativeData().CreatedByUser().eq(getCurrentUser())
    // // .or(b.DraftAdministrativeData().CreatedByUser().isNull()));
    // // }
    // return b.report_ID().eq(reportId);

    // })
    // .orderBy(c -> c.createdAt().asc());
    // return selectSingle(service, select, Records.class, "Record_Not_Found");
    // }

    // public List<Records> selectRecordsByReportId(CqnService service, String
    // reportId, Boolean isActiveEntity) {
    // CqnSelect select = Select.from(Records_.class)
    // .where(b -> {

    // // if (isNonCapFrameworkCallInDraft(isActiveEntity)) {
    // // return b.report_ID().eq(reportId).and(
    // // b.DraftAdministrativeData().CreatedByUser().eq(getCurrentUser())
    // // .or(b.DraftAdministrativeData().CreatedByUser().isNull()));
    // // }
    // return b.report_ID().eq(reportId);
    // }
    // // b -> b.report_ID().eq(reportId)
    // )
    // .orderBy(c -> c.createdAt().asc());
    // return selectList(service, select, Records.class);
    // }

    // public List<ReportFields> selectReportFieldsByReportId(CqnService service,
    // String reportId) {
    // CqnSelect select = Select.from(ReportFields_.class)
    // .where(b -> b.report_ID().eq(reportId));
    // return selectList(service, select, ReportFields.class);
    // }

    // public ParameterItems selectParameterItem(CqnService service, String name,
    // String language, String errorMessage) {
    // CqnSelect select = Select.from(ParameterItems_.class)
    // .where(b -> b.name().eq(name).and(b.language().eq(language)));
    // return selectSingle(service, select, ParameterItems.class, errorMessage);
    // }

    // // 修改 selectReportById 方法
    // public Reports selectReportById(CqnService service, String reportId, boolean
    // isActiveEntity, String errorMessage) {

    // CqnSelect select = Select.from(Reports_.class)
    // .where(b -> {
    // // // 判断是否是 CAP 框架调用
    // // if (isNonCapFrameworkCallInDraft(isActiveEntity)) {
    // // return b.ID().eq(reportId).and(b.IsActiveEntity().eq(isActiveEntity),
    // // b.DraftAdministrativeData().CreatedByUser().eq(getCurrentUser())
    // // .or(b.DraftAdministrativeData().CreatedByUser().isNull()));
    // // }
    // return b.ID().eq(reportId).and(b.IsActiveEntity().eq(isActiveEntity));
    // });

    // return selectSingle(service, select, Reports.class, errorMessage);
    // }

    // 添加判断是否是 CAP 框架调用的方法
    private boolean isNonCapFrameworkCallInDraft(Boolean isActiveEntity) {
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean isCapFrameworkCall = false;

        // 检查调用链中是否包含 CAP 框架的类
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("com.sap.cds.services.handler") ||
                    element.getClassName().contains("EventHandler")) {
                isCapFrameworkCall = true;
            }
        }
        if (!isCapFrameworkCall && isActiveEntity) {
            return true;
        }

        return false;
    }

    // /* Get current user using SecurityContextHolder */
    // public String getCurrentUser() {
    //     // Get the current user from the SecurityContextHolder
    //     SecurityContext securityContext = SecurityContextHolder.getContext();
    //     Authentication authentication = securityContext.getAuthentication();
    //     String username = authentication.getName();
    //     // .getAttribute("user", RequestAttributes.SCOPE_REQUEST);
    //     // return user != null ? user.getName() : null;
    //     return username;
    // }

    // Insert operations
    public <T extends CdsData, E extends StructuredType<E>> Result insert(
            CqnService service,
            DraftService serviceDraft,
            Class<E> entityClass,
            T entity,
            Boolean isActiveEntity) {
        if (isActiveEntity) {
            return service.run(Insert.into(entityClass).entry(entity));
        } else {
            return serviceDraft.newDraft(Insert.into(entityClass).entry(entity));
        }
    }

    public <T extends CdsData, E extends StructuredType<E>> Result update(
            CqnService service,
            DraftService serviceDraft,
            Class<E> entityClass,
            T entity,
            Boolean isActiveEntity) {
        if (isActiveEntity) {
            return service.run(Update.entity(entityClass).entry(entity));
        } else {
            return serviceDraft.patchDraft(Update.entity(entityClass).entry(entity));
        }
    }

    public <T extends CdsData, E extends StructuredType<E>> Result delete(
            CqnService service,
            DraftService serviceDraft,
            Class<E> entityClass,
            T entity,
            Boolean isActiveEntity) {
        if (isActiveEntity) {
            return service.run(Delete.from(entityClass).matching(entity));
        } else {
            return serviceDraft.cancelDraft(Delete.from(entityClass).matching(entity));
        }
    }

    // public Result insertRecord(CqnService service, DraftService serviceDraft,
    // Records record, Boolean isActiveEntity) {
    // // Set UTC timestamp for chatTime using Instant directly
    // record.setChatTime(java.time.Instant.now());
    // return insert(service, serviceDraft, Records_.class, record, isActiveEntity);
    // }

    // public Result insertReportField(CqnService service, DraftService
    // serviceDraft,
    // ReportFields field, Boolean isActiveEntity) {
    // return insert(service, serviceDraft, ReportFields_.class, field,
    // isActiveEntity);
    // }

    // public Result insertPcl(CqnService service, DraftService serviceDraft,
    // Pcls pcl, Boolean isActiveEntity) {
    // return insert(service, serviceDraft, Pcls_.class, pcl, isActiveEntity);
    // }

    // public void insertReportFields(CqnService service, DraftService serviceDraft,
    // List<ReportFields> fieldsList, Reports report) {
    // fieldsList.forEach(field -> {
    // field.setReportId(report.getId());
    // field.setIsActiveEntity(report.getIsActiveEntity());
    // insertReportField(service, serviceDraft, field, report.getIsActiveEntity());
    // });
    // }

    // public void updateRecordStatus(CqnService service, DraftService serviceDraft,
    // Records record) {
    // record.setIsAdopted(true);
    // if (record.getIsActiveEntity()) {
    // service.run(Update.entity(Records_.class).data(record));
    // } else {
    // serviceDraft.patchDraft(Update.entity(Records_.class).data(record));
    // }
    // }

    // Delete operations
    // public void deleteReportFieldsByReportId(CqnService service, String reportId)
    // {
    // CqnDelete delete = Delete.from(ReportFields_.class)
    // .where(b -> b.report_ID().eq(reportId));
    // service.run(delete);
    // }

    // public void deletePclsByReportId(CqnService service, String reportId) {
    // CqnDelete delete = Delete.from(Pcls_.class)
    // .where(b -> b.report_ID().eq(reportId));
    // service.run(delete);
    // }

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
                // throw new BusinessException("Failed_To_Set_Entity_Properties", e); // Changed exception type
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
