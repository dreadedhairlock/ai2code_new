using from '../../../../srv/index';
using from '../../../index';


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
