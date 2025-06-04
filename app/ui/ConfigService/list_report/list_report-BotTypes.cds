using from '../../../../srv/index';
using from '../../../index';


annotate ConfigService.BotTypes with @UI: {

    SelectionFields: [
        sequence,
        name,
        functionType_code,
        autoRun,
        executionCondition,
        model.name

    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : sequence,
            Label         : '{i18n>Sequence}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>BotName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : description,
            Label         : '{i18n>BotDescription}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : functionType_code,
            Label         : '{i18n>FunctionType}',
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
            Value         : executionCondition,
            Label         : '{i18n>ExecutionCondition}',
            @UI.Importance: #High
        },
    ]
};
