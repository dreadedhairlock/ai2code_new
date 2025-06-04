using from '../../../../srv/index';
using from '../../../index';

annotate MainService.BotInstances with @(UI: {
    LineItem  : [
        {
            $Type         : 'UI.DataField',
            Value         : sequence,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : result,
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : status_code,
            @UI.Importance: #High
        },
    ],
});
