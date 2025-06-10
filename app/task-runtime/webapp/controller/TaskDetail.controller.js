sap.ui.define(
  [
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageBox",
  ],
  (Controller, JSONModel, MessageBox) => {
    "use strict";

    return Controller.extend("task-runtime.controller.TaskDetail", {
      onInit: function () {
        const oModel = this.getOwnerComponent().getModel();

        oModel
          .bindList("/ContextNodes")
          .requestContexts()
          .then(
            function (aContexts) {
              const flatData = aContexts.map((ctx) => ctx.getObject());
              this.buildContextTree(flatData);
            }.bind(this)
          );
      },

      buildContextTree: function (flatList) {
        // group items by their section path
        const sectionMap = {};
        flatList.forEach((item) => {
          if (!sectionMap[item.path]) {
            sectionMap[item.path] = {
              path: item.path,
              label: item.path.split("/").pop(), // "section1"
              children: [],
            };
          }
          sectionMap[item.path].children.push({
            ID: item.ID,
            label: item.label,
            value: item.value,
            children: [],
          });
        });

        const roots = Object.values(sectionMap);
        const oTreeModel = new JSONModel({ nodes: roots });
        this.getOwnerComponent().setModel(oTreeModel, "tree");
      },

      getParentPath: function (path) {
        const lastDot = path.lastIndexOf("/");
        if (lastDot > 0) return path.substring(0, lastDot);
        return null;
      },

      onTreeItemPress: function (oEvent) {
        // 1) get the pressed item context
        const oItem = oEvent.getSource();
        const oTreeCtx = oItem.getBindingContext("tree");
        const sUuid = oTreeCtx.getProperty("ID");
        if (!sUuid) {
          MessageBox.warning("No ID on this node");
          return;
        }

        // 2) assemble your read path and fetch full entity
        const sReadPath = `/ContextNodes('${sUuid}')`;
        const oOData = this.getOwnerComponent().getModel(); // your V4 model
        oOData
          .bindContext(sReadPath)
          .requestObject()
          .then(
            function () {
              // this.byId("ContextNodeForm").setVisible(true);

              const oCNForm = this.byId("ContextNodeForm");

              const sPath = "/ContextNodes('" + sUuid + "')";

              oCNForm.setVisible(true).bindElement({ path: sPath });
            }.bind(this)
          )
          .catch(function (oErr) {
            MessageBox.error("Failed to load details: " + oErr.message);
          });
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
        const sContextNodeId = oContextNodeSelected
          .getBindingContext()
          .getProperty("ID");
        console.log(sContextNodeId);
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
      onTaskSelect: function () {
        // Get the reference to the author list control by its ID
        const oList = this.byId("TasksList");
      },

      // -----------------------------------------Task Tree --------------------------------------
    });
  }
);
