using from '../../../srv/index';

// annotate ConfigService.PromptTexts with {   
//     lang @(
//         Common.ValueList               : {
//             $Type                       : 'Common.ValueListType',
//             CollectionPath              : 'Language',
//             Label                       : 'Languages',
//             PresentationVariantQualifier: 'vh_lang',
//             Parameters                  : [
//                 {
//                     $Type            : 'Common.ValueListParameterInOut',
//                     LocalDataProperty: 'lang_code',
//                     ValueListProperty: 'code'
//                 },
//                 {
//                     $Type            : 'Common.ValueListParameterDisplayOnly',
//                     LocalDataProperty: 'lang_code',
//                     ValueListProperty: 'name'
//                 },
//                 {
//                     $Type            : 'Common.ValueListParameterDisplayOnly',
//                     LocalDataProperty: 'lang_code',
//                     ValueListProperty: 'descr'
//                 }
//             ]
//         }
//     );
// };

// annotate ConfigService.PromptTexts with @(
//     UI.PresentationVariant #vh_lang : {
//         $Type    : 'UI.PresentationVariantType',
//         SortOrder: [{
//             $Type     : 'Common.SortOrderType',
//             Property  : lang_code,
//             Descending: false
//         } ],
//     }
// );