using from '../../../../srv/index';
using from '../../../index';

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
