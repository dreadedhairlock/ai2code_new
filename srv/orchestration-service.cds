using ai.orchestration as db from '../db/orchestration-model';
using ai.orchestration.config as config from '../db/orchestration-config-model';
using ai.orchestration.rag as rag from '../db/orchestration-rag-model';

service MainService {
    entity Tasks             as projection on db.Task;
    entity ContextNodes      as projection on db.ContextNode;
    entity TaskTypes         as projection on config.TaskType;


    //entity SubTasks      as projection on db.SubTask;
    entity BotInstances      as projection on db.BotInstance
        actions {
            action execute()                             returns {
                result : String;
                tasks  : array of UUID;
            };
            action chatCompletion(content : LargeString) returns BotMessages;
        }

    entity BotMessages       as projection on db.BotMessage
        actions {
            action adopt() returns ContextNodes;
        }

    // Unbound actions
    action createTaskWithBots(name : String,
                              description : String,
                              typeId : UUID)                                              returns Tasks;

    entity BusinessScenarios as
        projection on rag.BusinessScenarios
        excluding {
            embeddings
        };

    entity CDSViews          as projection on rag.CDSViews;
    //按场景查询匹配的CDS Views
    action cdsViewsSearch(question : String, threshold : Decimal(5, 2))                   returns array of CDSViews;


    entity CDSViewFiles      as projection on rag.CDSViewFiles
        actions {
            action generateEmbeddings() returns String;
            action deleteEmbeddings()   returns String;
        };

    entity Viewfields        as
        projection on rag.Viewfields
        excluding {
            embeddings
        };

    entity RagJoinCond       as projection on rag.RagJoinCond;

    @cds.persistence.skip: true
    @odata.singleton
    entity excelupload {
        @Core.MediaType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        excel : LargeBinary;
    };

    //查询CDS View的Fields 也可直接查询db
    action viewFieldsSearch(question : String, threshold : Decimal(5, 2), langu : String) returns array of Viewfields;
    action viewJoinSearch()                                                               returns array of RagJoinCond;

}
