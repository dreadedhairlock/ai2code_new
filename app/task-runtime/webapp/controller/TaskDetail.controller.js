sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/ui/model/json/JSONModel"],
  (Controller, JSONModel) => {
    "use strict";

    return Controller.extend("task-runtime.controller.TaskDetail", {
      onInit: function () {
        const oModel = this.getOwnerComponent().getModel();
        oModel
          .bindList("/ContextNodes")
          .requestContexts()
          .then(
            function (aContexts) {
              var aData = aContexts.map(function (oContext) {
                return oContext.getObject(); // Returns JS object
              });

              //   Now aData is a plain JavaScript array -> can be used to create a JSONModel
              const oJSONModel = new JSONModel();
              oJSONModel.setData({ results: aData });

              // Use the JSON model as needed
              this.getOwnerComponent().setModel(oJSONModel, "myJSON");
              const data = this.getOwnerComponent()
                .getModel("myJSON")
                .getData().results;
              this.buildContextTree(data);

              //   const aTree = this._groupByPath(aData);
              //   const oTreeModel = new JSONModel({ nodes: aTree });
              //   this.getOwnerComponent().setModel(oTreeModel, "tree");
            }.bind(this)
          );
      },

      buildContextTree: function (flatData) {
        // Result tree
        const treeData = {};

        flatData.forEach((item) => {
          const pathSegments = item.path.split("/").filter(Boolean); // e.g. ["documents", "section1"]
          let current = treeData;

          // Build hierarchy
          pathSegments.forEach((segment) => {
            if (!current[segment]) {
              current[segment] = {};
            }
            current = current[segment];
          });

          // Assign label-value pair
          current[item.label] = item.value;
        });

        console.log(treeData);

        const aTree = this.prepareTreeArray(treeData);
        const oTreeModel = new JSONModel({ nodes: aTree });
        this.getOwnerComponent().setModel(oTreeModel, "tree");
      },

      prepareTreeArray: function (oObj) {
        return Object.keys(oObj).map((key) => {
          const node = { key: key, children: [] };
          const val = oObj[key];
          if (val !== null && typeof val === "object") {
            // object → recurse
            node.children = this.prepareTreeArray(val);
          } else {
            // primitive → treat as leaf with a value
            node.value = val;
          }
          return node;
        });
      },

      _groupByPath: function (flatData) {
        const map = {};
        flatData.forEach((item) => {
          // strip leading slash
          const pathKey = item.path.replace(/^\/+/, "");
          if (!map[pathKey]) {
            map[pathKey] = { key: pathKey, children: [] };
          }
          // push each label/value as a leaf node
          map[pathKey].children.push({
            key: item.label,
            value: item.value,
            children: [],
          });
        });
        // return array of all grouped nodes
        return Object.values(map);
      },

      _onLoadContextNodes: function (oData) {
        // oData.results is a flat array of ContextNode objects
        var aFlat = oData.results;
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
    
      // ---------------------------------------Chat Bot -------------------------------------
        onSubmitQuery: function() {
            var oInput = this.byId("chatInput");
            var sMessage = oInput.getValue().trim();
            if (sMessage) {
                // Add user message
                this.addChatMessage(sMessage, "user");
                // Clear input
                oInput.setValue("");
                // Simulate AI response (replace with your actual AI call)
                setTimeout(() => {
                    this.addChatMessage(sMessage + " hehehe", "ai");
                }, 1000);
            }
        },
        addChatMessage: function(sMessage, sType) {
            var oChatBox = this.byId("chatMessagesBox");
            var sTimestamp = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
            var oHTML = new sap.ui.core.HTML({
                content: `
                    <div class="chatBubbleContainer ${sType}">
                        <div class="chatBubble ${sType}">
                            <div>${sMessage}</div>
                            <div class="chatTimestamp">${sTimestamp}</div>
                        </div>
                    </div>
                `
            });
            oChatBox.addItem(oHTML);
            // Scroll to bottom
            setTimeout(() => {
                var oScrollContainer = this.byId("chatMessagesContainer");
                if (oScrollContainer && oScrollContainer.getDomRef("scroll")) {
                    oScrollContainer.scrollTo(0, oScrollContainer.getDomRef("scroll").scrollHeight);
                }
            }, 100);
        }
      // ---------------------------------------Chat Bot -------------------------------------
    }
  );
  }
);
