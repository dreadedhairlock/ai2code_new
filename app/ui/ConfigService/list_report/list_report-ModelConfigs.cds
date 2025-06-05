using from '../../../../srv/index';
using from '../../../index';

annotate ConfigService.ModelConfigs with @(UI: {
    SelectionFields: [
        name,
        parameters,
        modelName,
        provider
    ],
    LineItem       : [
        {
            $Type         : 'UI.DataField',
            Value         : name,
            Label         : '{i18n>ModelConfigName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : modelName,
            Label         : '{i18n>ModelName}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : parameters,
            Label         : '{i18n>Parameters}',
            @UI.Importance: #High
        },
        {
            $Type         : 'UI.DataField',
            Value         : provider,
            Label         : '{i18n>Provider}',
            @UI.Importance: #High
        },
    ],
});
