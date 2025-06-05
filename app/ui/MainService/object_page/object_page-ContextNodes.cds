using from '../../../../srv/index';
using from '../../../index';

annotate MainService.ContextNodes with @(UI: {
    HeaderInfo              : {
        TypeName      : '{i18n>ContextNodesSingular}',
        TypeNamePlural: '{i18n>ContextNodesPlural}',
        Title         : {Value: path},
        TypeImageUrl  : 'sap-icon://activities'
    },
    Facets                  : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>ContextNodesPlural}',
            ID    : 'ContextNodes',
            Target: '@UI.FieldGroup#ContextNodes'
        },

    ],
    FieldGroup #ContextNodes: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type         : 'UI.DataField',
                Value         : path,
            },
            {
                $Type         : 'UI.DataField',
                Value         : type,
            },
            {
                $Type         : 'UI.DataField',
                Value         : label,
            },
            {
                $Type         : 'UI.DataField',
                Value         : value,
            },
        ]
    },

});