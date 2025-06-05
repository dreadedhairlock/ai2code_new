using from '../../../../srv/index';
using from '../../../index';

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


});
