sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel"

], (Controller, JSONModel) => {
    "use strict";

    return Controller.extend("task-runtime.controller.runtime", {
        onInit: function (evt) {
            // set explored app's demo model on this sample
            var oModel = new JSONModel("./Tree.json");
            //             this.getView().setModel(oModel);
            // // 
            //             var oTree = this.byId("Tree");

            // oTree.setMode("MultiSelect");
            var uri = "/odata/v4/MainService/Tasks?$format=json";
            var oModel = new sap.ui.model.json.JSONModel();
            oModel.loadData(uri);
            console.log(oModel);
            this.getView().setModel(oModel, "tree");

        },


        // ---------------------------------------Context Tree -------------------------------------
        // This is Detail page
        onContextNodesSelect: function () {
            // Get the reference to the author list control by its ID
            const oList = this.byId("ContextNodesList");

            // Get the currently selected item (author) from the list
            const oContextNodeSelected = oList.getSelectedItem();

            // If no author is selected, exit the function
            if (!oContextNodeSelected) {
                return;
            }

            // Retrieve the ID of the selected author from its binding context
            const sContextNodeId = oContextNodeSelected.getBindingContext().getProperty("ID");
            console.log(sContextNodeId)
            // Call a private function to bind and display books related to the selected author
            this._bindContextNode(sContextNodeId);
        },

        _bindContextNode: function (sContextNodeId) {
            // Get a reference to the books table control by its ID
            const oForm = this.byId("ContextNodeForm");
            const oOtherForm = this.byId("BotInstanceForm");

            // If no author ID is provided, unbind the table and exit
            if (!sContextNodeId) {
                oForm.setVisible(false);
                oForm.unbindItems();
                return;
            } else {
                oForm.setVisible(true);
                oOtherForm.setVisible(false);
                // Bind the table items to the /Books entity set, filtered by the selected author's ID
                const sPath = "/ContextNodes('" + sContextNodeId + "')";

                oForm.bindElement({
                    path: sPath,
                });
            }
        },
        // ---------------------------------------Context Tree -------------------------------------




        // -----------------------------------------Task Tree --------------------------------------
        // This is Detail page
        onTaskSelect: function (oEvent) {
            // Get the reference to the author list control by its ID
            console.log("clicked")
            var oTree = this.byId("Tree"),
                aSelectedItems = oTree.getSelectedItems(),
                aSelectedIndices = [];

            for (var i = 0; i < aSelectedItems.length; i++) {
                aSelectedIndices.push(oTree.indexOfItem(aSelectedItems[i]));
            }

            oTree.expand(aSelectedIndices);

            var oTree = this.byId("Tree");
            var oBinding = oTree.getBinding("items");
            var iDroppedIndex = oTree.indexOfItem(aSelectedItems[0]);
            var oNewParentContext = oBinding.getContextByIndex(iDroppedIndex);

            if (!oNewParentContext) {
                return;
            }

            var oModel = oTree.getBinding("items").getModel();
            var oNewParent = oNewParentContext.getProperty();

            // Gunakan "nodes" sesuai struktur JSON Anda
            if (!oNewParent.nodes) {
                oNewParent.nodes = [];
            }


            // Buat node baru (bebas sesuai kebutuhan Anda)
            var oNewNode = {
                text: "New Node " + (i + 1) // Anda bisa custom di sini
                // Tambahkan properti lain jika perlu
            };

            // Tambahkan ke parent yang di-drop
            oNewParent.nodes.push(oNewNode);


            oModel.refresh(true); // Refresh untuk update tampilan
        },

        // onDragStart: function (oEvent) {
        //     var oTree = this.byId("Tree");
        //     var oBinding = oTree.getBinding("items");
        //     var oDragSession = oEvent.getParameter("dragSession");
        //     var oDraggedItem = oEvent.getParameter("target");
        //     console.log(oDraggedItem)
        //     var iDraggedItemIndex = oTree.indexOfItem(oDraggedItem);
        //     console.log(iDraggedItemIndex)
        //     var aSelectedIndices = oTree.getBinding("items").getSelectedIndices();
        //     console.log(aSelectedIndices)
        //     var aSelectedItems = oTree.getSelectedItems();
        //     console.log(aSelectedItems)

        //     var aDraggedItemContexts = [];

        //     if (aSelectedItems.length > 0) {
        //         // If items are selected, do not allow to start dragging from a item which is not selected.
        //         if (aSelectedIndices.indexOf(iDraggedItemIndex) === -1) {
        //             oEvent.preventDefault();
        //         } else {
        //             for (var i = 0; i < aSelectedItems.length; i++) {
        //                 aDraggedItemContexts.push(oBinding.getContextByIndex(aSelectedIndices[i]));
        //             }
        //         }
        //     } else {
        //         aDraggedItemContexts.push(oBinding.getContextByIndex(iDraggedItemIndex));
        //     }

        //     oDragSession.setComplexData("hierarchymaintenance", {
        //         draggedItemContexts: aDraggedItemContexts
        //     });
        // },

        onDrop: function (oEvent) {
            // var oTree = this.byId("Tree");
            // var oBinding = oTree.getBinding("items");
            // var oDragSession = oEvent.getParameter("dragSession");
            // var oDroppedItem = oEvent.getParameter("droppedControl");
            // var aDraggedItemContexts = oDragSession.getComplexData("hierarchymaintenance").draggedItemContexts;
            // var iDroppedIndex = oTree.indexOfItem(oDroppedItem);
            // var oNewParentContext = oBinding.getContextByIndex(iDroppedIndex);

            // if (aDraggedItemContexts.length === 0 || !oNewParentContext) {
            //     return;
            // }

            // var oModel = oTree.getBinding("items").getModel();
            // var oNewParent = oNewParentContext.getProperty();

            // // In the JSON data of this example the children of a node are inside an array with the name "categories".
            // if (!oNewParent.categories) {
            //     oNewParent.categories = []; // Initialize the children array.
            // }

            // for (var i = 0; i < aDraggedItemContexts.length; i++) {
            //     if (oNewParentContext.getPath().indexOf(aDraggedItemContexts[i].getPath()) === 0) {
            //         // Avoid moving a node into one of its child nodes.
            //         continue;
            //     }

            //     // Copy the data to the new parent.
            //     oNewParent.categories.push(aDraggedItemContexts[i].getProperty());

            //     // Remove the data. The property is simply set to undefined to preserve the tree state (expand/collapse states of nodes).
            //     // oModel.setProperty(aDraggedItemContexts[i].getPath(), undefined, aDraggedItemContexts[i], true);
            // }
            // // Refresh model to update bindings
            // oModel.refresh(true);

            // var oTree = this.byId("Tree");
            // var oBinding = oTree.getBinding("items");
            // var oDragSession = oEvent.getParameter("dragSession");
            // var oDroppedItem = oEvent.getParameter("droppedControl");
            // var aDraggedItemContexts = oDragSession.getComplexData("hierarchymaintenance").draggedItemContexts;
            // var iDroppedIndex = oTree.indexOfItem(oDroppedItem);
            // var oNewParentContext = oBinding.getContextByIndex(iDroppedIndex);

            // if (aDraggedItemContexts.length === 0 || !oNewParentContext) {
            //     return;
            // }

            // var oModel = oTree.getBinding("items").getModel();
            // var oNewParent = oNewParentContext.getProperty();

            // // Gunakan "nodes" sesuai struktur JSON Anda
            // if (!oNewParent.nodes) {
            //     oNewParent.nodes = [];
            // }

            // for (var i = 0; i < aDraggedItemContexts.length; i++) {
            //     // Cegah drop ke descendant-nya sendiri
            //     if (oNewParentContext.getPath().indexOf(aDraggedItemContexts[i].getPath()) === 0) {
            //         continue;
            //     }

            //     // Buat node baru (bebas sesuai kebutuhan Anda)
            //     var oNewNode = {
            //         text: "New Node " + (i + 1) // Anda bisa custom di sini
            //         // Tambahkan properti lain jika perlu
            //     };

            //     // Tambahkan ke parent yang di-drop
            //     oNewParent.nodes.push(oNewNode);
            // }

            // oModel.refresh(true); // Refresh untuk update tampilan

        }



        // -----------------------------------------Task Tree --------------------------------------
    });
});