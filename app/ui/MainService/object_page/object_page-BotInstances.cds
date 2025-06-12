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
            Label : '{i18n>BotMessages}',
            ID    : 'BotMessages',
            Target: 'messages/@UI.LineItem'
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotInstancesPlural}',
            ID    : 'BotInstances',
            Target: '@UI.FieldGroup#BotInstances'
        },

    ],

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
