using {
  cuid,
  managed,
  sap.common.CodeList
} from '@sap/cds/common';

namespace ai.orchestration.config;

/** Task types, such as field design, API mapping, etc. */
entity TaskType : cuid, managed {
  name        : String(100);
  description : String;
  autoRun     : Boolean default false;
  isMain      : Boolean default true;
  botTypes    : Composition of many BotType
                  on botTypes.taskType = $self;
}

/** BotType: bot type, with contextType field (enum reference) */
entity BotType : cuid, managed {
  taskType            : Association to TaskType;
  sequence            : Integer;
  name                : String(50);
  description         : String;
  functionType        : Association to BotFunctionType default 'AI_CHAT';
  autoRun             : Boolean default false;
  executionCondition  : String(1000);
  model               : Association to ModelConfig;
  prompts             : Composition of many PromptText
                          on prompts.botType = $self;
  outputContextPath   : String(1000); // Output path, can be array[-1]; relative in subTask, absolute in main task
  contextType         : Association to ContextType; // New: Output context data type (enum)
  isRAGEnabled        : Boolean default false;
  //ragFunction       : Association to RagFunction;
  ragClass            : String(100); // Replaces ragFunction
  ragSource           : String(100);
  ragTopK             : Integer;
  ragThreshold        : String;
  implementationClass : String(100); // For C and F types
  //subTaskContextPath: String(1000); // Must include array, e.g., datasource.children[-1].content
  subTaskType         : Association to TaskType;
}

/** AI model configuration */
entity ModelConfig : cuid, managed {
  name       : String(100);
  provider   : String(50);
  modelName  : String(100);
  parameters : LargeString;
}

/** Bot prompt templates, support multilingual and multiple templates */
entity PromptText : cuid, managed {
  botType : Association to BotType;
  lang    : Association to Languages; // Association to enable value help
  name    : String(100);
  content : LargeString;
}

/** Bot execution status enumeration */
entity BotInstanceStatus : CodeList {
  key code : String enum {
        CREATED = 'CREATED';
        RUNNING = 'RUNNING';
        SUCCESS = 'SUCCESS';
        FAILED = 'FAILED';
        SKIPPED = 'SKIPPED';
        CANCELLED = 'CANCELLED';
      };
}

/** Bot function type enumeration */
entity BotFunctionType : CodeList {
  key code : String enum {
        AI_CHAT = 'AI CHAT';
        FUNCTION_CALL = 'FUNCTION CALL';
        CODE = 'CODE';
      //SUBTASK_GENERATOR = 'SUBTASK_GENERATOR'; replaced by FUNCTION_CALL
      };
}

entity Languages : CodeList {
  key code : String enum {
        EN = 'English';
        ZH = 'Chinese';
        DE = 'German';
        JA = 'Japanese';
        ID = 'Indonesian';
      };
}

/** ContextType: type of content in the context node (strongly typed) */
entity ContextType : CodeList {
  key code : String enum {
        string = 'STRING'; // Plain text
        markdown = 'MARKDOWN'; // Markdown document
        code = 'CODE'; // Code snippet
        json = 'JSON'; // JSON structure
      //object  = 'OBJECT';    // Object
      //array   = 'ARRAY';     // Array
      //table   = 'TABLE';     // Table
      //image   = 'IMAGE';     // Image (base64 or URL)
      };
}

/** Bot Execution interface impl class */
@cds.persistence.skip
entity BotExecutionClass {
  key Name        : String;
      Description : String;
}

/** RAG Extractor interface impl class */
@cds.persistence.skip
entity RAGExtractorClass {
  key Name        : String;
      Description : String;
}
