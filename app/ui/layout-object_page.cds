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
                Value: isMain,
                Label: '{i18n>TaskIsMain}'
            },
            {
                $Type: 'UI.DataField',
                Value: autoRun,
                Label: '{i18n>TaskAutoRun}'
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
                Value: ragSource,
                Label: '{i18n>BotIsMain}'
            },
            {
                $Type: 'UI.DataField',
                Value: autoRun,
                Label: '{i18n>BotAutoRun}'
            }

        ]
    },
});

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
    HeaderInfo                : {
        TypeName      : '{i18n>TaskName}',
        TypeNamePlural: '{i18n>TaskPlural}',
        Title         : {Value: name},
        TypeImageUrl  : 'sap-icon://activities'
    },
    Facets                    : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>GeneralInfo}',
            ID    : 'General',
            Target: '@UI.FieldGroup#General'
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>Administration}',
            ID    : 'Administration',
            Target: '@UI.FieldGroup#Administration'
        },
    ],
    FieldGroup #General       : {
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
        ]
    },
    FieldGroup #Administration: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: createdAt,
            },
            {
                $Type: 'UI.DataField',
                Value: createdBy,
            },
            {
                $Type: 'UI.DataField',
                Value: modifiedAt,
            },
            {
                $Type: 'UI.DataField',
                Value: modifiedBy,
            },
        ]
    }
});
