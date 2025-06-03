using from '../../srv/index';

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


annotate MainService.Tasks with @title: '{i18n>TaskRuntime}' {
    name        @Common.Label         : '{i18n>TaskName}';
    description @Common.Label         : '{i18n>Description}';
    sequence    @Common.Label         : '{i18n>Sequence}';
    contextPath @Common.Label         : '{i18n>ContextPath}';
};

annotate MainService.BotInstances with {
    sequence @Common.Label: '{i18n>Sequence}';
    result   @Common.Label: '{i18n>Result}';
    status   @Common.Label: '{i18n>Status}';
};

annotate MainService.ContextNodes with {
    path  @Common.Label: '{i18n>Path}';
    type  @Common.Label: '{i18n>Type}';
    label @Common.Label: '{i18n>Label}';
    value @Common.Label: '{i18n>Value}';
};

annotate MainService.createTaskWithBots with  @title       : '{i18n>CreateTaskWithBots}'  (name  @Common.Label: '{i18n>TaskName}',
description                                   @Common.Label: '{i18n>Description}',
typeId                                        @Common.Label: '{i18n>TypeID}' );
