sap.ui.define(
  [
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/Dialog",
    "sap/m/DialogType",
    "sap/ui/layout/form/SimpleForm",
    "sap/m/Label",
    "sap/m/Input",
    "sap/m/Button",
    "sap/m/ButtonType",
    "sap/ui/core/Element",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
  ],
  (
    Controller,
    JSONModel,
    Dialog,
    DialogType,
    SimpleForm,
    Label,
    Input,
    Button,
    ButtonType,
    Element,
    MessageToast,
    MessageBox
  ) => {
    "use strict";

    return Controller.extend("task-runtime.controller.TaskDetail", {
      onInit: function () {
        const oRouter = this.getOwnerComponent().getRouter();
        const oModel = this.getOwnerComponent().getModel();
        oRouter.attachRouteMatched(
          function (oEvent) {
            this._sTaskId = oEvent.getParameter("arguments").taskId;
            oModel.refresh();
          }.bind(this)
        );
      },

      onCNItemPress: function (oEvent) {
        // 1) get the pressed item context
        const oItem = oEvent.getParameter("listItem");
        const oTreeCtx = oItem.getBindingContext("contextNodes");
        const sUuid = oTreeCtx.getProperty("ID");

        if (!sUuid) {
          return;
        }

        // 2) assemble your read path and fetch full entity
        const sReadPath = `/ContextNodes('${sUuid}')`;
        const oOData = this.getOwnerComponent().getModel();
        oOData
          .bindContext(sReadPath)
          .requestObject()
          .then(
            function () {
              const oCNForm = this.byId("ContextNodeForm");

              const sPath = "/ContextNodes('" + sUuid + "')";

              oCNForm.bindElement({ path: sPath });
            }.bind(this)
          );
        this.getOwnerComponent()._loadContextNodes(this._sTaskId);
      },

      // ---------------------------------------Context Tree -------------------------------------
      onCreateCNData: function () {
        const oCNForm = this._createCNFormForCreate();

        this.oCreateDialog = new Dialog({
          title: "Add New Context Node",
          content: [oCNForm],
          beginButton: new Button({
            text: "Create",
            enabled: false,
            press: this._createContextNodes.bind(this),
          }),
          endButton: new Button({
            text: "Cancel",
            press: function () {
              this.oCreateDialog.close();
            }.bind(this),
          }),
          afterClose: function () {
            this.oCreateDialog.destroy();
            this.oCreateDialog = null;
          }.bind(this),
        });

        this.getView().addDependent(this.oCreateDialog);
        this.oCreateDialog.open();
      },

      _clearCNInputs: function () {
        const aInputIds = ["CNPath", "CNLabel", "CNType", "CNValue"];
        aInputIds.forEach((id) => {
          const oInput = sap.ui.getCore().byId(id);
          if (oInput) {
            oInput.setValue("");
          }
        });
      },

      _createContextNodes: async function () {
        const sPath = Element.getElementById("CNPathCreate").getValue();
        const sLabel = Element.getElementById("CNLabelCreate").getValue();
        const sType = Element.getElementById("CNTypeCreate").getValue();
        const sValue = Element.getElementById("CNValueCreate").getValue();

        const oNewContextNodes = {
          task_ID: this._sTaskId,
          path: sPath,
          label: sLabel,
          type: sType,
          value: sValue,
        };

        const oModel = this.getOwnerComponent().getModel();
        const oBinding = oModel.bindList("/ContextNodes");
        await oBinding.create(oNewContextNodes).created();
        MessageToast.show("Context Nodes created");

        await this.getOwnerComponent()._loadContextNodes(this._sTaskId);
        this.oCreateDialog.close();
      },

      _updateContextNodes: async function (sContextNodeId) {
        const sPath = Element.getElementById("CNPathEdit").getValue();
        const sLabel = Element.getElementById("CNLabelEdit").getValue();
        const sType = Element.getElementById("CNTypeEdit").getValue();
        const sValue = Element.getElementById("CNValueEdit").getValue();

        try {
          const oModel = this.getOwnerComponent().getModel();
          const sEntityPath = "/ContextNodes('" + sContextNodeId + "')";

          // Bind ke specific context untuk update
          const oContext = oModel.bindContext(sEntityPath);
          await oContext.requestObject(); // Load existing data
          const oBoundContext = oContext.getBoundContext();

          if (!oBoundContext) {
            MessageToast.show("Context node not found!");
            return;
          }

          // Update properties
          oBoundContext.setProperty("path", sPath);
          oBoundContext.setProperty("label", sLabel);
          oBoundContext.setProperty("type", sType);
          oBoundContext.setProperty("value", sValue);

          MessageToast.show("Context Node updated successfully!");

          // Close dialog dan refresh data
          this.oEditDialog.close();

          await this.getOwnerComponent()._loadContextNodes(this._sTaskId);

          this._refreshCNContent();
        } catch (oError) {
          console.error("Update error:", oError);
          MessageToast.show(
            "Error updating context node: " + (oError.message || oError)
          );
        }
      },

      _createCNFormForCreate: function () {
        return new SimpleForm({
          content: [
            new Label({ text: "Task ID" }),
            new Input("CNTaskIdCreate", {
              value: this._sTaskId,
              editable: false,
            }),

            new Label({ text: "Path" }),
            new Input("CNPathCreate", {
              placeholder: "Enter path",
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),

            new Label({ text: "Label" }),
            new Input("CNLabelCreate", {
              placeholder: "Enter label",
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),

            new Label({ text: "Type" }),
            new Input("CNTypeCreate", {
              placeholder: "Enter type",
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),

            new Label({ text: "Value" }),
            new Input("CNValueCreate", {
              placeholder: "Enter value",
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),
          ],
        });
      },

      _createCNFormForEdit: function (oEditData) {
        return new SimpleForm({
          content: [
            new Label({ text: "Task ID" }),
            new Input("CNTaskIdEdit", {
              value: this._sTaskId,
              editable: false,
            }),

            new Label({ text: "Path" }),
            new Input("CNPathEdit", {
              value: oEditData.path,
              placeholder: "Enter path",
            }),

            new Label({ text: "Label" }),
            new Input("CNLabelEdit", {
              value: oEditData.label,
              placeholder: "Enter label",
            }),

            new Label({ text: "Type" }),
            new Input("CNTypeEdit", {
              value: oEditData.type,
              placeholder: "Enter type",
            }),

            new Label({ text: "Value" }),
            new Input("CNValueEdit", {
              value: oEditData.value,
              placeholder: "Enter value",
            }),
          ],
        });
      },

      _validateCNCreateForm: function () {
        const aFieldIds = [
          "CNPathCreate",
          "CNLabelCreate",
          "CNTypeCreate",
          "CNValueCreate",
        ];
        const bAllFilled = aFieldIds.every((sId) => {
          const oInput = sap.ui.getCore().byId(sId);
          return oInput && oInput.getValue().trim().length > 0;
        });
        this.oCreateDialog.getBeginButton().setEnabled(bAllFilled);
      },

      onDeleteCNData: async function () {
        const oTree = this.byId("docTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected) {
          MessageToast.show("Please select a context node to delete!");
          return;
        }

        const oJsonCtx = oSelected.getBindingContext("contextNodes");
        const sID = oJsonCtx.getProperty("ID");

        // Langsung gunakan path, seperti program awal tapi dengan await requestObject
        const oODataModel = this.getOwnerComponent().getModel();
        const sPath = "/ContextNodes('" + sID + "')";

        try {
          const oContext = oODataModel.bindContext(sPath);

          // KUNCI: Request object dulu untuk memastikan context valid
          await oContext.requestObject();
          const oBoundContext = oContext.getBoundContext();

          if (oBoundContext) {
            await oBoundContext.delete();
            MessageToast.show("Context node deleted successfully.");
            await this.getOwnerComponent()._loadContextNodes(this._sTaskId);
          } else {
            MessageToast.show("Could not find context with ID: " + sID);
          }
        } catch (oError) {
          console.error("Delete error:", oError);
          MessageToast.show("Error: " + (oError.message || oError));
        }
      },

      onEditCNData: function () {
        const oTree = this.byId("docTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected) {
          MessageToast.show("Please select a context node to edit!");
          return;
        }

        const oJsonCtx = oSelected.getBindingContext("contextNodes");
        const oEditData = {
          ID: oJsonCtx.getProperty("ID"),
          path: oJsonCtx.getProperty("path"),
          label: oJsonCtx.getProperty("label"),
          type: oJsonCtx.getProperty("type"),
          value: oJsonCtx.getProperty("value"),
        };

        const oCNForm = this._createCNFormForEdit(oEditData);

        this.oEditDialog = new Dialog({
          title: "Edit Context Node",
          content: [oCNForm],
          beginButton: new Button({
            text: "Update",
            enabled: true, // Sudah terisi
            press: this._updateContextNodes.bind(this, oEditData.ID),
          }),
          endButton: new Button({
            text: "Cancel",
            press: function () {
              this.oEditDialog.close();
            }.bind(this),
          }),
          afterClose: function () {
            this.oEditDialog.destroy();
            this.oEditDialog = null;
          }.bind(this),
        });

        this.getView().addDependent(this.oEditDialog);
        this.oEditDialog.open();
      },

      _refreshCNContent: function () {
        const oForm = this.byId("ContextNodeForm");
        const oElementBinding = oForm.getElementBinding();
        if (oElementBinding) {
          oElementBinding.refresh();
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
        // Check if the selected node item is a botIsntances
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
          // If it's not a bot, then it must be a task
          // If it is a task, only display the bot instances of the task when isMain is false
        } else if (oContext.getProperty("isMain") == false) {
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
