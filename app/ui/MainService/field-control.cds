using from '../../../srv/index';

annotate MainService.Tasks with {
    @Common.SemanticObject : 'TaskRuntime'
    ID
};

annotate MainService.BotInstances with {
    @Common.SemanticObject : 'BotInstances'
    ID
};
