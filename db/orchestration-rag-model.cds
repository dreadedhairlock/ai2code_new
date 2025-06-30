using {
    cuid,
    managed
} from '@sap/cds/common';

namespace ai.orchestration.rag;

entity BusinessScenarios : cuid, managed {
    scenario      : String @title: 'Business Scenario';
    description   : String @title: 'Scenario Description';
    viewCategory  : String @title: 'View Category';
    embeddings    : String(768); //HANA生成的向量
    embeddings_ai : String(1536); //AI Core生成的向量
}


entity CDSViews : managed {
        viewCategory : String  @title: 'View Category';
    key viewName     : String  @title: 'View Name';
        viewDesc     : String  @title: 'View Description';
        isActive     : Boolean @title: 'is Active'
}

entity CDSViewFiles : cuid, managed {
    fileName    : String;
    size        : String;

    @Core.IsMediaType: true
    mediaType   : String;
    isGenerated : Boolean;

    @Core.MediaType: mediaType  @Core.ContentDisposition.Filename: fileName
    fileContent : LargeBinary;
    viewFields  : Composition of many Viewfields
                      on viewFields.file = $self;
}

entity Viewfields : cuid, managed {
    file                 : Association to CDSViewFiles;
    category             : String;
    content              : LargeString; //view fields
    isGeneratedEmbedding : Boolean;
    tableName            : String;
    tableDesc            : String;
    langu                : String;
    embeddings           : String(768); //可选生成向量，可以直接根据View查询fields
}

entity RagJoinCond : cuid, managed {
    tableFirst  : String;
    tableSecond : String;
    tableJoin   : LargeString //join关系
}
