using from '../../srv/index';

annotate ConfigService.TaskTypes with @(UI: {
    HeaderInfo: {
        TypeName      : '{i18n>TaskSingle}',
        TypeNamePlural: '{i18n>TaskPlural}',
    },
    SelectionFields     : [
        name,
        isMain
    ],
    LineItem  : [
        {
            $Type   : 'UI.DataField',
            Value   : name,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : description,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : isMain,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : autoRun,
            @UI.Importance : #High
        },
    ],

});

annotate MainService.Tasks with @(UI:{
    HeaderInfo: {
        TypeName      : '{i18n>TaskRuntime}',
        TypeNamePlural: '{i18n>TaskRuntimePlural}',
    },
    SelectionFields     : [
        name,
        type.name,
    ],
    LineItem  : [
        {
            $Type   : 'UI.DataField',
            Value   : name,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : description,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : contextPath,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataFieldForAction',
            Label : '{i18n>Create}',
            Action : 'MainService.EntityContainer/createTaskWithBots'
        },
    ],

});

annotate MainService.BotInstances with @(UI: {
    HeaderInfo: {
        TypeName      : '{i18n>BotInstancesSingular}',
        TypeNamePlural: '{i18n>BotInstancesPlural}',
    },
    LineItem  : [
        {
            $Type   : 'UI.DataField',
            Value   : sequence,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : result,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : status_code,
            @UI.Importance : #High
        },
    ],
});

annotate MainService.ContextNodes with @(UI: {
    HeaderInfo: {
        TypeName      : '{i18n>ContextNodesSingular}',
        TypeNamePlural: '{i18n>ContextNodesPlural}',
    },
    LineItem  : [
        {
            $Type   : 'UI.DataField',
            Value   : path,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : type,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : label,
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : value,
            @UI.Importance : #High
        },
    ],
});
