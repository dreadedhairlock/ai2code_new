using from '../../srv/index';
using from '../index';

annotate ConfigService.TaskTypes with @(UI: {
    HeaderInfo             : {
        TypeName      : '{i18n>TaskName}',
        TypeNamePlural: '{i18n>TaskPlural}',
        Title         : {Value: name},
        Description   : {Value: description},
        TypeImageUrl  : 'sap-icon://activity-items'
    },
    Facets                 : [
        {
            $Type : 'UI.CollectionFacet',
            Label : '{i18n>GeneralInfo}',
            ID    : 'GeneralInfo',
            Facets: [{
                $Type : 'UI.ReferenceFacet',
                Label : '{i18n>GeneralInfo}',
                ID    : 'GeneralFields',
                Target: '@UI.FieldGroup#GeneralInfo'
            }]
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotTypes}',
            ID    : 'BotTypes',
            Target: 'botTypes/@UI.LineItem'
        },
    ],
    FieldGroup #GeneralInfo: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: name,
                Label: '{i18n>TaskName}'
            },
            {
                $Type: 'UI.DataField',
                Value: description,
                Label: '{i18n>TaskDescription}'
            },
            {
                $Type: 'UI.DataField',
                Value: autoRun,
                Label: '{i18n>TaskAutoRun}'
            },
            {
                $Type: 'UI.DataField',
                Value: isMain,
                Label: '{i18n>TaskIsMain}'
            }


        ]
    },

});

annotate ConfigService.BotTypes with @(UI: {
    HeaderInfo             : {
        TypeName      : '{i18n>BotTypes}',
        TypeNamePlural: '{i18n>BotTypePlural}',
        Title         : {Value: name},
        Description   : {Value: description},
        TypeImageUrl  : 'sap-icon://factory'
    },
    Facets                 : [
        {
            $Type : 'UI.CollectionFacet',
            Label : '{i18n>BotType}',
            ID    : 'BotType',
            Facets: [{
                $Type : 'UI.ReferenceFacet',
                Label : '{i18n>BotType}',
                ID    : 'GeneralFields',
                Target: '@UI.FieldGroup#GeneralInfo'
            }]
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>PromptText}',
            ID    : 'PromptText',
            Target: 'prompts/@UI.LineItem'
        },
    ],
    FieldGroup #GeneralInfo: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: name,
                Label: '{i18n>BotName}'
            },
            {
                $Type: 'UI.DataField',
                Value: description,
                Label: '{i18n>BotDescription}'
            },
            {
                $Type: 'UI.DataField',
                Value: functionType_code,
                Label: '{i18n>FunctionType}'
            },
            {
                $Type: 'UI.DataField',
                Value: sequence,
                Label: '{i18n>Sequence}'
            },
            {
                $Type: 'UI.DataField',
                Value: autoRun,
                Label: '{i18n>BotAutoRun}'
            },
            {
                $Type: 'UI.DataField',
                Value: executionCondition,
                Label: '{i18n>executionCondition}'
            },
            {
                $Type: 'UI.DataField',
                Value: ragSource,
                Label: '{i18n>RAGSource}'
            },
            {
                $Type: 'UI.DataField',
                Value: sequence,
                Label: '{i18n>Sequence}'
            }

        ]
    },
});

annotate ConfigService.BotTypes with {
    contextType @(
        Common.Text                    : contextType.descr,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'ContextTypes',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: contextType_code,
                    ValueListProperty: 'code',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'name',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};

annotate ConfigService.PromptTexts with @(UI: {
    HeaderInfo             : {
        TypeName      : '{i18n>PromptSingle}',
        TypeNamePlural: '{i18n>PromptPlural}',
        Title         : {Value: name},
        Description   : {Value: botType.name},
        TypeImageUrl  : 'sap-icon://activity-items'
    },
    Facets                 : [{
        $Type : 'UI.CollectionFacet',
        Label : '{i18n>GeneralInfo}',
        ID    : 'GeneralInfo',
        Facets: [{
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>GeneralInfo}',
            ID    : 'GeneralFields',
            Target: '@UI.FieldGroup#GeneralInfo'
        }]
    }, ],
    FieldGroup #GeneralInfo: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: name,
                Label: '{i18n>PromptName}'
            },
            {
                $Type: 'UI.DataField',
                Value: lang,
                Label: '{i18n>Language}'
            },
            {
                $Type: 'UI.DataField',
                Value: content,
                Label: '{i18n>Content}'
            },
        ]
    },

});

annotate MainService.Tasks with @(UI: {
    HeaderInfo              : {
        TypeName      : '{i18n>TaskName}',
        TypeNamePlural: '{i18n>TaskPlural}',
        Title         : {Value: name},
        TypeImageUrl  : 'sap-icon://activities'
    },
    Facets                  : [

        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>TaskInfo}',
            ID    : 'TaskInfo',
            Target: '@UI.FieldGroup#TaskInfo'
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotInstancesPlural}',
            ID    : 'BotInstances',
            Target: 'botInstances/@UI.LineItem'
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>ContextNodes}',
            ID    : 'ContextNodes',
            Target: 'contextNodes/@UI.LineItem'
        },
    ],
    FieldGroup #TaskInfo    : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: name,
            },
            {
                $Type: 'UI.DataField',
                Value: description,
            },
            {
                $Type: 'UI.DataField',
                Value: contextPath,
            },
            {
                $Type: 'UI.DataField',
                Value: sequence,
            },
            {
                $Type: 'UI.DataField',
                Value: isMain,
            },
        ]
    },
    FieldGroup #BotInstances: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: sequence,
            },
            {
                $Type: 'UI.DataField',
                Value: botInstances.result,
            },
            {
                $Type: 'UI.DataField',
                Value: botInstances.status_code,
            },
        ]
    },
    FieldGroup #ContextNodes: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: contextNodes.path,
            },
            {
                $Type: 'UI.DataField',
                Value: contextNodes.type,
            },
            {
                $Type: 'UI.DataField',
                Value: contextNodes.label,
            },
            {
                $Type: 'UI.DataField',
                Value: contextNodes.value,
            },
        ]
    },
});

annotate MainService.BotInstances with @(UI: {
    HeaderInfo              : {
        TypeName      : '{i18n>BotInstancesSingular}',
        TypeNamePlural: '{i18n>BotInstancesPlural}',
        Title         : {Value: result},
        TypeImageUrl  : 'sap-icon://activities'
    },
    Facets                  : [

        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>TaskRuntime}',
            ID    : 'Tasks',
            Target: 'tasks/@UI.LineItem'
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotInstancesPlural}',
            ID    : 'BotInstances',
            Target: '@UI.FieldGroup#BotInstances'
        },

    ],
    FieldGroup #Tasks       : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: task.description,
            },
            {
                $Type: 'UI.DataField',
                Value: task.contextPath,
            },
            {
                $Type: 'UI.DataField',
                Value: task.sequence,
            },
            {
                $Type: 'UI.DataField',
                Value: task.isMain,
            },
            {
                $Type: 'UI.DataField',
                Value: task.name,
            },
            {
                $Type: 'UI.DataField',
                Value: task.description,
            },
            {
                $Type: 'UI.DataField',
                Value: task.contextPath,
            },
            {
                $Type: 'UI.DataField',
                Value: task.sequence,
            },
            {
                $Type: 'UI.DataField',
                Value: task.isMain,
            },
        ]
    },
    FieldGroup #BotInstances: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: sequence,
            },
            {
                $Type: 'UI.DataField',
                Value: result,
            },
            {
                $Type: 'UI.DataField',
                Value: status_code,
            },
        ]
    },

});
