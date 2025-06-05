using from '../../../../srv/index';
using from '../../../index';

annotate ConfigService.ModelConfigs with @(UI: {
    HeaderInfo             : {
        TypeName      : '{i18n>ModelConfigName}}',
        TypeNamePlural: '{i18n>ModelConfigPlural}}',
        Title         : {Value: name},
        Description   : {Value: provider},
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
                Value: modelName,
                Label: '{i18n>ModelName}'
            },
            {
                $Type: 'UI.DataField',
                Value: provider,
                Label: '{i18n>Provider}'
            },
            {
                $Type: 'UI.DataField',
                Value: parameters,
                Label: '{i18n>Parameters}'
            },
        ]
    },

});
