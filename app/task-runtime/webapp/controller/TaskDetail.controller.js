sap.ui.define(
  [
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/Dialog",
    "sap/m/DialogType",
    "sap/ui/layout/form/SimpleForm",
    "sap/m/Label",
    "sap/m/TextArea",
    "sap/m/Input",
    "sap/m/Button",
    "sap/m/ButtonType",
    "sap/ui/core/Element",
    "sap/m/MessageToast",
  ],
  (
    Controller,
    JSONModel,
    Dialog,
    DialogType,
    SimpleForm,
    Label,
    TextArea,
    Input,
    Button,
    ButtonType,
    Element,
    MessageToast
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
      },

      // ---------------------------------------Context Tree -------------------------------------
      onCreate: function () {
        if (!this.oSubmitDialogContextTree) {
          this.oSubmitDialogContextTree = new Dialog({
            type: DialogType.Message,
            title: "Create Context Node",
            content: [this._createCNForm()],
            beginButton: new Button({
              type: ButtonType.Emphasized,
              text: "Create",
              enabled: false,
              press: function () {
                this._createContextNodes();
                this.oSubmitDialogContextTree.close();
              }.bind(this),
            }),
            endButton: new Button({
              text: "Cancel",
              press: function () {
                this.oSubmitDialogContextTree.close();
              }.bind(this),
            }),
          });

          this.oSubmitDialogContextTree.attachAfterClose(
            function () {
              this._clearCNInputs();
            }.bind(this)
          );
        }

        this.oSubmitDialogContextTree.open();
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
        const sPath = Element.getElementById("CNPath").getValue();
        const sLabel = Element.getElementById("CNLabel").getValue();
        const sType = Element.getElementById("CNType").getValue();
        const sValue = Element.getElementById("CNValue").getValue();

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
      },

      _createCNForm: function () {
        return new SimpleForm({
          content: [
            new Label({ text: "Task ID" }),
            new Input("CNTaskId", {
              value: this._sTaskId,
              editable: false,
            }),

            new Label({ text: "Path" }),
            new Input("CNPath", {
              placeholder: "Enter path",
              required: true,
              liveChange: this._validateCNForm.bind(this),
            }),
            new Label({ text: "Label" }),
            new Input("CNLabel", {
              placeholder: "Enter label",
              required: true,
              liveChange: this._validateCNForm.bind(this),
            }),
            new Label({ text: "Type" }),
            new Input("CNType", {
              placeholder: "Enter type",
              required: true,
              liveChange: this._validateCNForm.bind(this),
            }),
            new Label({ text: "Value" }),
            new Input("CNValue", {
              placeholder: "Enter value",
              required: true,
              liveChange: this._validateCNForm.bind(this),
            }),
          ],
        });
      },

      _validateCNForm: function () {
        // List of your Input IDs
        const aFieldIds = ["CNPath", "CNLabel", "CNType", "CNValue"];
        // Check that each one has a non‐empty, trimmed value
        const bAllFilled = aFieldIds.every((sId) => {
          const oInput = sap.ui.getCore().byId(sId);
          return oInput && oInput.getValue().trim().length > 0;
        });
        // Enable/disable the dialog’s Begin button
        this.oSubmitDialogContextTree.getBeginButton().setEnabled(bAllFilled);
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

      onCreateSubTask: function () {
        if (!this.oSubmitDialogTaskTree) {
          this.oSubmitDialogTaskTree = new Dialog({
            type: DialogType.Message,
            title: "Create Subtask",
            content: [this._createTaskForm()],
            beginButton: new Button({
              type: ButtonType.Emphasized,
              text: "Create",
              enabled: false,
              press: function () {
                this._createTask();
                this.oSubmitDialogTaskTree.close();
              }.bind(this),
            }),
            endButton: new Button({
              text: "Cancel",
              press: function () {
                this.oSubmitDialogTaskTree.close();
              }.bind(this),
            }),
          });
        }

        this.oSubmitDialogTaskTree.open();
      },

      onItemPress: function (oEvent) {
        // Handle item press event
        const oItem = oEvent.getSource();
        const oContext = oItem.getBindingContext();
        if (oContext) {
          this._navToTaskRunDetail(oContext.getProperty("ID"));
        } else {
          MessageToast.show("No context available for the selected item.");
        }
      },

      _navToTaskRunDetail: function (sTaskId) {
        this.getOwnerComponent().getRouter().navTo("RouteTaskDetail", {
          taskId: sTaskId,
        });
      },

      _createSelectTaskTypeDialog: function () {
        return this.oSelectTypeDialog
          ? this.oSelectTypeDialog
          : new SelectDialog({
              noDataText: "No task types found",
              title: "Select Task Type",
              items: {
                path: "/TaskTypes",
                template: new sap.m.StandardListItem({
                  title: "{name}",
                  description: "{description}",
                  highlightText: "{ID}", // ID placeholder
                }),
              },
              confirm: function (oEvent) {
                const oSelectedItem = oEvent.getParameter("selectedItem");
                if (oSelectedItem) {
                  Element.getElementById("taskTypeId").setValue(
                    oSelectedItem.getHighlightText()
                  );
                }
              }.bind(this),
            });
      },

      _createTaskForm: function () {
        return new SimpleForm({
          content: [
            new Label({ text: "Task name" }),
            new Input("taskName", {
              placeholder: "Enter task name",
              required: true,
              liveChange: function (oEvent) {
                var sText = oEvent.getParameter("value");
                this.oSubmitDialogTaskTree
                  .getBeginButton()
                  .setEnabled(sText.length > 0);
              }.bind(this),
            }),
            
            new Label({ text: "Description" }),
            new TextArea("taskDescription", {
              placeholder: "Enter task description",
              rows: 3,
            }),

            new Label({ text: "Type id" }),
            new Input("taskTypeId", {
              showValueHelp: true,
              valueHelpOnly: true,
              valueHelpRequest: function () {
                this.oSelectTypeDialog = this._createSelectTaskTypeDialog();
                this.oSelectTypeDialog.setModel(
                  this.getOwnerComponent().getModel()
                );
                this.oSelectTypeDialog.open();
              }.bind(this),
            }),
          ],
        });
      },

      _createTask: function () {
        const sTaskName = Element.getElementById("taskName").getValue();
        const sTaskDescription =
          Element.getElementById("taskDescription").getValue();
        const sTaskTypeId = Element.getElementById("taskTypeId").getValue();
        const oNewTask = {
          name: sTaskName,
          description: sTaskDescription,
          type_ID: sTaskTypeId == "" ? null : sTaskTypeId,
        };
        const oModel = this.getOwnerComponent().getModel();
        const sPath = "/createTaskWithBots(...)";
        const oContextBinding = oModel.bindContext(sPath);
        oContextBinding.setParameter("name", oNewTask.name);
        oContextBinding.setParameter("description", oNewTask.description);
        oContextBinding.setParameter("typeId", oNewTask.type_ID);
        oContextBinding
          .invoke()
          .then(
            function (oContext) {
              MessageToast.show("Task created successfully");
              this._navToTaskRunDetail(
                oContextBinding.getBoundContext().getProperty("ID")
              );
              oModel.refresh();
            }.bind(this)
          )
          .catch(
            function (oError) {
              MessageToast.show("Error creating task: " + oError.message);
            }.bind(this)
          );
          
      },

      onEditSubTask: function () {
        const oTree = this.byId("tree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected || oSelected.getProperty("type") == "bot") {
          MessageToast.show("Please select a task to edit!");
          return;
        }
      },

      onDeleteSubTask: function () {

      },

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
