using from '../../../../srv/index';
using from '../../../index';

annotate MainService.TaskTypes with @(UI: {
    SelectionFields: [
        name,
        isMain,
        autoRun
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>TaskName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : description,
            Label         : '{i18n>TaskDescription}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : autoRun,
            Label         : '{i18n>AutoRun}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : isMain,
            Label         : '{i18n>IsMain}',
            @UI.Importance: #High
        },
    ],

});
