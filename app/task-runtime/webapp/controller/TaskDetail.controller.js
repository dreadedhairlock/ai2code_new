sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/ui/model/json/JSONModel"],
  (Controller, JSONModel) => {
    "use strict";

    return Controller.extend("task-runtime.controller.TaskDetail", {
      onInit: function () {
        const oRouter = this.getOwnerComponent().getRouter();
        const oModel = this.getOwnerComponent().getModel();

        oRouter.attachRouteMatched(
          async function (oEvent) {
            const oArgs = oEvent.getParameter("arguments");
            if (oArgs && oArgs.taskId) {
              // Bind the main element first
              this.getView().bindElement({
                path: "/Tasks('" + oArgs.taskId + "')",
              });

              try {
                // Wait for all data to load before proceeding
                await Promise.all([
                  this.loadBotInstances(oArgs.taskId),
                  this.loadContextNodes(oArgs.taskId),
                ]);

                // Data is now loaded, you could trigger a refresh here if needed
              } catch (error) {
                console.error("Failed to load data:", error);
              }
            }
          }.bind(this)
        );
      },

      loadBotInstances: function (taskId) {
        const oModel = this.getOwnerComponent().getModel();
        return oModel
          .bindList("/Tasks('" + taskId + "')/botInstances")
          .requestContexts()
          .then(
            function (aContexts) {
              var aData = aContexts.map(function (oContext) {
                var oObj = oContext.getObject();
                oObj.type = "bot";
                return oObj;
              });
              const oJSONModel = new JSONModel();
              oJSONModel.setData({ results: aData });
              this.getOwnerComponent().setModel(oJSONModel, "botInstances");
            }.bind(this)
          );
      },

      loadContextNodes: function (taskId) {
        const oModel = this.getOwnerComponent().getModel();
        return oModel
          .bindList("/Tasks('" + taskId + "')/contextNodes")
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
        const oItem = oEvent.getParameter("listItem");
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

              oCNForm.bindElement({ path: sPath });
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
          const sPath = "/contextNodes('" + sContextNodeId + "')";

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
        var oSelectedItem = oEvent.getParameter("listItem"); // atau "item"
        var oContext = oSelectedItem.getBindingContext("botInstances");
        var sID = oContext.getProperty("ID");
        var sType = oContext.getProperty("type");

        var oTree = this.byId("tree"),
          aSelectedItems = oTree.getSelectedItems(),
          aSelectedIndices = [];

        for (var i = 0; i < aSelectedItems.length; i++) {
          aSelectedIndices.push(oTree.indexOfItem(aSelectedItems[i]));
        }

        var oTree = this.byId("tree");
        var oBinding = oTree.getBinding("items");
        var iItemIndex = oTree.indexOfItem(aSelectedItems[0]);
        var oNewParentContext = oBinding.getContextByIndex(iItemIndex);

        if (!oNewParentContext) {
          return;
        }

        var oNewParent = oNewParentContext.getProperty();

        // Gunakan "nodes" sesuai struktur JSON Anda
        if (!oNewParent.nodes) {
          oNewParent.nodes = [];
        }

        if (sType == "bot") {
          const oModel = this.getOwnerComponent().getModel();
          oModel
            .bindList("/BotInstances('" + sID + "')/tasks")
            .requestContexts()
            .then(
              function (aContexts) {
                var aData = aContexts.map(function (oContext) {
                  var oObj = oContext.getObject();
                  oObj.type = "task"; // add type tree 'bot'
                  return oObj;
                });
                //   Now aData is a plain JavaScript array -> can be used to create a JSONModel
                const oJSONModel = new JSONModel();
                oJSONModel.setData({ results: aData });
                oNewParent.nodes.push(...aData);
                // Refresh tree
                oTree.getBinding("items").refresh();
                oTree.expand(aSelectedIndices);
              }.bind(this)
            );
        } else {
          const oModel = this.getOwnerComponent().getModel();
          oModel
            .bindList("/Tasks('" + sID + "')/botInstances")
            .requestContexts()
            .then(
              function (aContexts) {
                var aData = aContexts.map(function (oContext) {
                  var oObj = oContext.getObject();
                  oObj.type = "bot"; // Tambahkan properti 'type' dengan nilai 'bot'
                  return oObj;
                });
                //   Now aData is a plain JavaScript array -> can be used to create a JSONModel
                const oJSONModel = new JSONModel();
                oJSONModel.setData({ results: aData });
                var isDuplicate = oNewParent.nodes.some(function (
                  existingItem
                ) {
                  return existingItem.ID === newItem.ID;
                });

                aData.forEach(function (newItem) {
                  var isDuplicate = oNewParent.nodes.some(function (
                    existingItem
                  ) {
                    return existingItem.ID === newItem.ID;
                  });

                  if (!isDuplicate) {
                    oNewParent.nodes.push(newItem);
                  }
                });
                // Refresh tree

                oTree.getBinding("items").refresh();
                oTree.expand(aSelectedIndices);
              }.bind(this)
            );
        }

        // Refresh untuk update tampilan
      },

      onEditContextPress: function () {},

      // -----------------------------------------Task Tree --------------------------------------

      // ---------------------------------------Chat Bot -------------------------------------
      onSubmitQuery: function () {
        var oInput = this.byId("chatInput");
        var sMessage = oInput.getValue().trim();
        if (sMessage) {
          // Add user message
          this.addChatMessage(sMessage, "user");
          // Clear input
          oInput.setValue("");
          // Simulate AI response (replace with your actual AI call)
          setTimeout(() => {
            this.addChatMessage("AI received: " + sMessage, "ai");
          }, 1000);
        }
      },
      addChatMessage: function (sMessage, sType) {
        var oChatBox = this.byId("chatMessagesBox");
        var sTimestamp = new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        });
        var oHTML = new sap.ui.core.HTML({
          content: `
                    <div class="chatBubbleContainer ${sType}">
                        <div class="chatBubble ${sType}">
                            <div>${sMessage}</div>
                            <div class="chatTimestamp">${sTimestamp}</div>
                        </div>
                    </div>
                `,
        });
        oChatBox.addItem(oHTML);
        // Scroll to bottom
        setTimeout(() => {
          var oScrollContainer = this.byId("chatMessagesContainer");
          if (oScrollContainer && oScrollContainer.getDomRef("scroll")) {
            oScrollContainer.scrollTo(
              0,
              oScrollContainer.getDomRef("scroll").scrollHeight
            );
          }
        }, 100);
      },
      // ---------------------------------------Chat Bot -------------------------------------
    });
  }
);
