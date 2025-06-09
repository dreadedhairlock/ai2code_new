sap.ui.define([
    "sap/ui/core/mvc/Controller",

], (Controller) => {
    "use strict";

    return Controller.extend("runtime.controller.runtime", {
        
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
                const sPath = "/ContextNodes(" + sContextNodeId + ")";

                oForm.bindElement({
                    path: sPath,
                });
            }
        },
        // ---------------------------------------Context Tree -------------------------------------



        
        // -----------------------------------------Task Tree --------------------------------------
        // This is Detail page
        onTaskSelect: function () {
            // Get the reference to the author list control by its ID
            const oList = this.byId("TasksList");
    
        },


        // -----------------------------------------Task Tree --------------------------------------
    });
});