using from '../../srv/index';

annotate ConfigService.TaskTypes with @(UI: {
    SelectionFields: [
        name,
        isMain,
        autoRun
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>TaskName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : description,
            Label         : '{i18n>TaskDescription}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : autoRun,
            Label         : '{i18n>AutoRun}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : isMain,
            Label         : '{i18n>IsMain}',
            @UI.Importance: #High
        },
    ],

});

annotate ConfigService.PromptTexts with @(UI: {
    SelectionFields     : [
        name,
        lang
    ],
    LineItem  : [
        {
            $Type   : 'UI.DataField',
            Value   : name,
            Label   : '{i18n>PromptName}',
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : lang,
            Label   : '{i18n>Language}',
            @UI.Importance : #High
        },
        {
            $Type   : 'UI.DataField',
            Value   : content,
            Label   : '{i18n>Content}',
            @UI.Importance : #High
        },
    ],

});


annotate ConfigService.BotTypes with @UI: {
 
    SelectionFields: [
        sequence,
        name,
        functionType_code,
        autoRun,
        executionCondition,
        model.name

    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : sequence,
            Label         : '{i18n>Sequence}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>BotName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : description,
            Label         : '{i18n>BotDescription}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : functionType_code,
            Label         : '{i18n>FunctionType}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : autoRun,
            Label         : '{i18n>AutoRun}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : executionCondition,
            Label         : '{i18n>ExecutionCondition}',
            @UI.Importance: #High
        },
    ]
};

annotate ConfigService.ModelConfigs with @(UI: {
    HeaderInfo     : {
        TypeName      : '{i18n>ModelConfigSingular}',
        TypeNamePlural: '{i18n>ModelConfigPlural}',
    },
    SelectionFields: [
        name,
        parameters,
        modelName,
        provider
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>ModelConfigName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : modelName,
            Label         : '{i18n>ModelName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : parameters,
            Label         : '{i18n>Parameters}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : provider,
            Label         : '{i18n>Provider}',
            @UI.Importance: #High
        },
    ],
});


annotate MainService.Tasks with @(UI: {
    SelectionFields: [
        name,
        type.name,
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>TaskName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : description,
            Label         : '{i18n>TaskDescription}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : contextPath,
            Label         : '{i18n>ContextPath}',
            @UI.Importance: #High
        },
        {
            $Type : 'UI.DataFieldForAction',
            Label : '{i18n>Create}',
            Action: 'MainService.EntityContainer/createTaskWithBots'
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
            $Type         : 'UI.DataField',
            Value         : sequence,
            Label         : '{i18n>Sequence}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : result,
            Label         : '{i18n>Result}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : status_code,
            Label         : '{i18n>Status}',
            @UI.Importance: #High
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
            $Type         : 'UI.DataField',
            Value         : path,
            Label         : '{i18n>Path}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : type,
            Label         : '{i18n>Type}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : label,
            Label         : '{i18n>Label}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : value,
            Label         : '{i18n>Value}',
            @UI.Importance: #High
        },
    ],
});
