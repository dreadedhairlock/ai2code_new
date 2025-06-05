sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/Filter",
    "sap/ui/model/FilterOperator",
], (Controller, Filter, FilterOperator) => {
    "use strict";

    return Controller.extend("runtime.controller.runtime", {
        onInit() {
        },

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
            const oTable = this.byId("ContextNodeTable");

            // If no author ID is provided, unbind the table and exit
            if (!sContextNodeId) {
                oTable.unbindItems();
                return;
            }

            // Bind the table items to the /Books entity set, filtered by the selected author's ID
            oTable.bindItems({
                path: "/ContextNodes", // OData entity set
                filters: [new Filter("ID", FilterOperator.EQ, sContextNodeId)], // Show only books matching the selected author
                template: new sap.m.ColumnListItem({
                    cells: [
                        // Display the book title
                        new sap.m.Text({ text: "{label}" }),
                        // Display the book description
                        new sap.m.Text({ text: "{type}" }),

                    ],
                }),
            });
        },
    });
});