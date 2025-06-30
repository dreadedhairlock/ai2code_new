using {
  cuid,
  managed
} from '@sap/cds/common';

using {
  ai.orchestration.config.TaskType          as TaskType,
  ai.orchestration.config.BotType           as BotType,
  ai.orchestration.config.BotInstanceStatus as BotInstanceStatus
} from './orchestration-config-model';

namespace ai.orchestration;

/** Task entity, supports multi-level sub-tasks, recorded in context. */
entity Task : cuid, managed {

  name         : String(100);
  description  : String;
  contextPath  : String(1000); // Context path for the task, e.g., datasource.children[3]
  sequence     : Integer; // Execution order for sub-tasks
  isMain       : Boolean default true; // Redundant flag to mark main tasks
  botInstance  : Association to BotInstance;
  type         : Association to TaskType;
  botInstances : Composition of many BotInstance
                   on botInstances.task = $self;
  contextNodes : Composition of many ContextNode
                   on contextNodes.task = $self; // All context nodes under this task
}

/** Single context node, flattened structure to support tree reconstruction */
entity ContextNode : cuid, managed {
  path  : String(1000); // Unique path, e.g., a.b.c[0].d
  label : String(200); // Node label
  type  : String(50); // Type (e.g., text, markdown, code, object, array)
  value : LargeString; // Node value/content
  task  : Association to Task; // Parent task
//readonly  : Boolean default false; // Optional: whether the node is read-only
// Extendable: sorting, validation, metadata, etc.
}

/** Bot execution instance */
entity BotInstance : cuid, managed {
  sequence : Integer;
  result   : LargeString;
  type     : Association to BotType;
  status   : Association to BotInstanceStatus default 'CREATED';
  task     : Association to Task;
  tasks    : Composition of many Task
               on tasks.botInstance = $self; // Sub-tasks
  messages : Composition of many BotMessage
               on messages.botInstance = $self;
}

/** Bot message entity, records human-AI/system conversations */
entity BotMessage : cuid, managed {
  role        : String(20); // 'user' | 'assistant' | 'system'
  message     : LargeString;
  ragData     : LargeString; // RAG result data (optional)
  botInstance : Association to BotInstance;
}
