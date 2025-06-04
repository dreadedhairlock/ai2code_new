using from '../../../srv/index';


annotate ConfigService.PromptTexts with {
    lang @(
        Common.ValueList               : {
            $Type                       : 'Common.ValueListType',
            CollectionPath              : 'Languages',
            Label                       : 'Language',
            PresentationVariantQualifier: 'vh_PromptText_language',
            Parameters                  : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: lang_code,
                    ValueListProperty: 'code',
                },
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: lang_code,
                    ValueListProperty: 'descr',
                }
            ],
        },
        Common.ValueListWithFixedValues: true
    )
};

annotate ConfigService.PromptTexts with @(UI.PresentationVariant #vh_PromptText_language: {
    $Type    : 'UI.PresentationVariantType',
    SortOrder: [{
        $Type     : 'Common.SortOrderType',
        Property  : lang_code,
        Descending: true,
    }, ],
}, );
