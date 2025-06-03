using from '../../srv/index';

annotate ConfigService.TaskTypes with @(UI: {Identification: [
    {
        Value: name,
        $Type: 'UI.DataField',
        Label: '{i18n>TaskName}',
    },
    {
        Value: description,
        $Type: 'UI.DataField',
        Label: '{i18n>TaskDescr}',
    },
],

});

annotate MainService.Tasks with @(UI: {
    HeaderInfo: {
        TypeName: '{i18n>TaskName}',
        TypeNamePlural : '{i18n>TaskPlural}',
        Title: {Value: name},
        TypeImageUrl : 'sap-icon://activities'
    },
    Facets: [
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>General}',
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
    FieldGroup #General: {
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