using ai.orchestration as db from '../db/orchestration-model';
using ai.orchestration.config as config from '../db/orchestration-config-model';

service MainService {
    entity Tasks        as projection on db.Task;
    entity ContextNodes as projection on db.ContextNode;
    entity TaskTypes    as projection on config.TaskType;
    entity BotTypes     as projection on config.BotType;

    //entity SubTasks      as projection on db.SubTask;
    entity BotInstances as projection on db.BotInstance
        actions {
            action execute()                             returns {
                result : String;
                tasks  : array of UUID;
            };
            action executeAsync()                        returns Boolean; //用于自动运行, web socket

            action chatCompletion(content : LargeString) returns BotMessages;
        }

        entity BotMessages  as projection on db.BotMessage
            actions {
                action adopt() returns array of ContextNodes;
            }

    // Unbound actions
    action createTaskWithBots(name : String @mandatory,
                              description : String,
                              typeId : UUID)  returns Tasks;


    action getContextNodesTree(taskId : UUID) returns {
        nodes : array of {
            ID             : UUID null; // Optional for folder
            path           : String;
            label          : String; // Show the label in the tree
            type           : String;
            value          : String null; // Optional for folder
            isFolder       : Boolean;
        }
    }

}

annotate MainService.TaskTypes with @odata.draft.enabled;
