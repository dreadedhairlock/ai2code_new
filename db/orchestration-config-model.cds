using {
  cuid,
  managed,
  sap.common.Languages,
  sap.common.CodeList
} from '@sap/cds/common';

namespace ai.orchestration.config;

/* 任务类型，如字段设计、API映射等。 */
entity TaskType : cuid, managed {
  name        : String(100);
  description : String;
  autoRun     : Boolean default false;
  isMain      : Boolean default true;
  botTypes    : Composition of many BotType
                  on botTypes.taskType = $self;
}

/* BotType: bot类型，增加contextType字段（枚举引用） */
entity BotType : cuid, managed {
  taskType            : Association to TaskType;
  sequence            : Integer;
  name                : String(50);
  description         : String;
  functionType        : Association to BotFunctionType default 'A'; //A F C S
  autoRun             : Boolean default false;
  executionCondition  : String(1000);
  model               : Association to ModelConfig;
  prompts             : Composition of many PromptText
                          on prompts.botType = $self;
  outputContextPath   : String(1000); // 输出内容写回路径, 可以是数组[-1], 在subTask中是相对路径；在主task中是绝对路径
  contextType         : Association to ContextType; // 新增：输出内容的数据类型（枚举）
  isRAGEnabled        : Boolean default false;
  //ragFunction         : Association to RagFunction;
  ragClass            : String(100); //代替ragFunction
  ragSource           : String(100);
  ragTopK             : Integer;
  implementationClass : String(100); //C和F适用
//subTaskContextPath  : String(1000);   // 约定必须包含数组，例如datasource.children[-1].content，数组实例会写入subTask的contextPath, 例如datasource.children[3]
//subTaskType         : Association to TaskType;
}

/* AI模型配置 */
entity ModelConfig : cuid, managed {
  name       : String(100);
  provider   : String(50);
  modelName  : String(100);
  parameters : LargeString;
}

/* Bot提示词模板，支持多语言多模板。 */
entity PromptText : cuid, managed {
  botType : Association to BotType;
  lang    : Association to Languages; // Association to enable value help
  name    : String(100);
  content : LargeString;
}

/* Bot执行状态枚举 */
entity BotInstanceStatus : CodeList {
  key code : String enum {
        C = 'CREATED';
        R = 'RUNNING';
        S = 'SUCCESS';
        F = 'FAILED';
        K = 'SKIPPED';
        X = 'CANCELLED';
      };
}

/* Bot功能类型枚举 */
entity BotFunctionType : CodeList {
  key code : String enum {
        A = 'AI';
        F = 'FUNCTION_CALL';
        C = 'CODE';
      //S = 'SUBTASK_GENERATOR';  用Function_call代替
      };
}

/* ContextType: 上下文节点内容类型（强类型约束） */
entity ContextType : CodeList {
  key code : String enum {
        string = 'STRING'; // 普通文本
        markdown = 'MARKDOWN'; // Markdown文档
        code = 'CODE'; // 代码片段
        json = 'JSON'; // JSON结构
      //object   = 'OBJECT';    // 对象
      //array    = 'ARRAY';     // 数组
      //table    = 'TABLE';     // 表格
      //image    = 'IMAGE';     // 图片(base64或URL)
      };
}
