using ai.orchestration as db from '../db/orchestration-model';
using ai.orchestration.config as cfg from '../db/orchestration-config-model';

service MainService {
    entity Tasks        as projection on db.Task;
    entity TaskTypes    as projection on cfg.TaskType;
    entity ContextNodes as projection on db.ContextNode;

    //entity SubTasks      as projection on db.SubTask;
    entity BotInstances as projection on db.BotInstance
        actions {
            action execute() returns {
                result : String;
                tasks  : array of UUID;
            };
            action executeAsync() returns Boolean; //用于自动运行, web socket
            
            action chatCompletion(content: LargeString) returns BotMessages;
        }

    entity BotMessages  as projection on db.BotMessage
        actions {
            action adopt() returns array of ContextNodes;
        }

    // Unbound actions
    action createTaskWithBots(name : String,
                              description : String,
                              typeId : UUID) returns Tasks;

}

annotate MainService.TaskTypes with @odata.draft.enabled ;