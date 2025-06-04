using from '../../../../srv/index';
using from '../../../index';


annotate ConfigService.PromptTexts with @(UI: {
    SelectionFields: [
        name,
        lang_code
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>PromptName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : lang_code,
            Label         : '{i18n>Language}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : content,
            Label         : '{i18n>Content}',
            @UI.Importance: #High
        },
    ],

});
