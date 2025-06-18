using from '../../../../srv/index';
using from '../../../index';

annotate MainService.TaskTypes with @(UI: {
    HeaderInfo             : {
        TypeName      : '{i18n>TaskName}',
        TypeNamePlural: '{i18n>TaskPlural}',
        Title         : {
            $Type : 'UI.DataField',
            Value : name
        },
        Description   : {
            $Type : 'UI.DataField',
            Value: description
        },
        TypeImageUrl  : 'sap-icon://activity-items'
    },
    Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>GeneralInfo}',
            ID    : 'GeneralInfo',
            Target: '@UI.FieldGroup#GeneralInfo',
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotTypes}',
            ID    : 'BotTypes',
            Target: 'botTypes/@UI.LineItem',
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
