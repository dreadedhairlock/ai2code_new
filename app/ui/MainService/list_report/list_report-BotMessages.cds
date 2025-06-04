using from '../../../../srv/index';
using from '../../../index';

annotate MainService.BotMessages with @(UI: {
    LineItem  : [
        {
            $Type         : 'UI.DataField',
            Value         : role,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : message,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : ragData,
            @UI.Importance: #High
        },
    ],
});
