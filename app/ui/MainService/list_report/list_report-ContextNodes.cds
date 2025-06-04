using from '../../../../srv/index';
using from '../../../index';

annotate MainService.ContextNodes with @(UI: {
    HeaderInfo: {
        TypeName      : '{i18n>ContextNodesSingular}',
        TypeNamePlural: '{i18n>ContextNodesPlural}',
    },
    LineItem  : [
        {
            $Type         : 'UI.DataField',
            Value         : path,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : type,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : label,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : value,
            @UI.Importance: #High
        },
    ],
});
