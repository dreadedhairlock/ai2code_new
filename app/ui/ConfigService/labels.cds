using from '../../../srv/index';

annotate ConfigService.TaskTypes with @title: '{i18n>TaskTypes}' {
    name        @Common.Label               : '{i18n>TaskName}';
    description @Common.Label               : '{i18n>Description}';
    autoRun     @Common.Label               : '{i18n>TaskAutoRun}';
    isMain      @Common.Label               : '{i18n>TaskIsMain}';
};

annotate ConfigService.BotTypes with @title: '{i18n>BotTypes}' {
    sequence           @Common.Label       : '{i18n>Sequence}';
    name               @Common.Label       : '{i18n>BotTypeName}';
    description        @Common.Label       : '{i18n>Description}';
    functionType       @Common.Label       : '{i18n>FunctionType}';
    autoRun            @Common.Label       : '{i18n>BotAutoRun}';
    executionCondition @Common.Label       : '{i18n>ExecutionCondition}';
    model              @Common.Label       : '{i18n>Model}';
};
