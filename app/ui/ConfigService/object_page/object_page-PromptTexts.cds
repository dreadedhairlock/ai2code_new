using from '../../../../srv/index';
using from '../../../index';

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
                Value: lang_code,
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
