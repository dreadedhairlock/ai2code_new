using ai.orchestration.config as cfg from '../db/orchestration-config-model';

service ConfigService {
  entity TaskTypes           as projection on cfg.TaskType;
  //entity TaskBotSequences     as projection on cfg.TaskBotSequence;
  entity BotTypes            as projection on cfg.BotType;
  entity ModelConfigs        as projection on cfg.ModelConfig;
  entity PromptTexts         as projection on cfg.PromptText;
  //entity FunctionCalls        as projection on cfg.FunctionCall;
  entity BotInstanceStatuses as projection on cfg.BotInstanceStatus;
  entity BotFunctionTypes    as projection on cfg.BotFunctionType;
  //entity RagFunctions         as projection on cfg.RagFunction;
  entity ContextTypes        as projection on cfg.ContextType;
  entity Languages           as projection on cfg.Languages;
  entity BotExecutionClass   as projection on cfg.BotExecutionClass;
  entity RAGExtractorClass   as projection on cfg.RAGExtractorClass;
}

annotate ConfigService.TaskTypes with @odata.draft.enabled;
