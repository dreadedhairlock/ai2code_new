using from '../../srv/index';
using from '../ui/layout-list_report';

annotate ConfigService.TaskTypes with @(UI: {Identification: [
    {
        Value: name,
        $Type: 'UI.DataField',
        Label: '{i18n>TaskName}',
    },
    {
        Value: description,
        $Type: 'UI.DataField',
        Label: '{i18n>TaskDescr}',
    },
],

});

annotate MainService.Tasks with @(UI: {
    HeaderInfo: {
        TypeName       : '{i18n>TaskName}',
        TypeNamePlural : '{i18n>TaskPlural}',
        Title          : {Value: name},
        TypeImageUrl   : 'sap-icon://activities'
    },
    Facets: [

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
    FieldGroup #TaskInfo: {
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
    HeaderInfo: {
        TypeName       : '{i18n>BotInstancesSingular}',
        TypeNamePlural : '{i18n>BotInstancesPlural}',
        Title          : {Value: result},
        TypeImageUrl   : 'sap-icon://activities'
    },
    Facets: [

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
    FieldGroup #Tasks: {
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