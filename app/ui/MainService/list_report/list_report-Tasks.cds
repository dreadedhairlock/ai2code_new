using from '../../../../srv/index';
using from '../../../index';

annotate MainService.Tasks with @(UI: {
    SelectionFields: [
        name,
        type.name,
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : description,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : contextPath,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : sequence,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : isMain,
            @UI.Importance: #High
        },
        {
            $Type : 'UI.DataFieldForAction',
            Label : '{i18n>Create}',
            Action: 'MainService.EntityContainer/createTaskWithBots'
        },
    ],

});
