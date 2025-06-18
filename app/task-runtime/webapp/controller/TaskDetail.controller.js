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
    "sap/m/MessageBox",
    "sap/m/SelectDialog",
    "sap/ui/core/Fragment",
    "sap/m/ComboBox",
    "sap/m/FormattedText",
    "sap/ui/core/ListItem",
    "sap/m/StepInput",
    "sap/m/Select",
  ],
  function (
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
    MessageToast,
    MessageBox,
    SelectDialog,
    Fragment,
    ComboBox,
    FormattedText,
    ListItem,
    StepInput,
    Select
  ) {
    "use strict";

    return Controller.extend("task-runtime.controller.TaskDetail", {
      // Initialize router & chat history
      onInit: function () {
        this.oFlexibleColumnLayout = this.byId("flexibleColumnLayout");
        const oRouter = this.getOwnerComponent().getRouter();
        const oModel = this.getOwnerComponent().getModel();

        oRouter.attachRouteMatched(
          function (oEvent) {
            this._sTaskId = oEvent.getParameter("arguments").taskId;

            // load context tree
            this.loadContextNodesTree();
            oModel.refresh();
          }.bind(this)
        );

        // this._chatHistory = [];
        // this._currentMessages = [];
        // this._currentChatId = null;
        // this._loadHistoryFromStorage();
        // this.startNewChat();

        // Initialize view model for tree data
        var oViewModel = new JSONModel({
          rootNodes: [],
          selectedNode: null,
        });
        this.getView().setModel(oViewModel, "contextNodes");
      },

      loadContextNodesTree: function () {
        // Retrieve the main OData model from the component
        const oModel = this.getOwnerComponent().getModel();
        // Access the view's JSON model for context nodes
        const oViewModel = this.getView().getModel("contextNodes");

        // Create an unbound action binding for getContextNodesTree
        const oContextBinding = oModel.bindContext("/getContextNodesTree(...)");

        // Set the input parameter for the action
        oContextBinding.setParameter("taskId", this._sTaskId);

        // Execute the action and handle the result
        oContextBinding
          .execute()
          .then((oContext) => {
            // Try to extract the response object from the context
            let finalResult;

            if (oContext) {
              try {
                finalResult = oContext.getObject();
              } catch (e) {
                console.warn("Error retrieving result from context object:", e);
              }
            }

            // Fallback: try to get the result from the bound context if main context fails
            if (!finalResult) {
              const boundContext = oContextBinding.getBoundContext();
              if (boundContext) {
                finalResult = boundContext.getObject();
              }
            }

            return finalResult;
          })
          .then((result) => {
            // Normalize the returned data into a flat node array
            let nodes = [];

            if (result && result.nodes) {
              // Case 1: Expected format { nodes: [...] }
              nodes = result.nodes;
            } else if (result && Array.isArray(result)) {
              // Case 2: Direct array
              nodes = result;
            } else if (result && typeof result === "object") {
              // Case 3: Try to find an array within any property
              Object.keys(result).forEach((key) => {
                if (Array.isArray(result[key])) {
                  nodes = result[key];
                }
              });
            }

            // Update the tree model if nodes exist
            if (nodes && nodes.length > 0) {
              // Convert flat node data into hierarchical tree structure
              const hierarchicalData = this._convertFlatToHierarchical(nodes);

              // Update the JSON view model with structured nodes
              if (oViewModel) {
                oViewModel.setProperty("/rootNodes", hierarchicalData);
              }
            } else {
              // No valid node data found; fallback to empty structure
              console.warn("No nodes data found");
              if (oViewModel) {
                oViewModel.setProperty("/rootNodes", []);
              }
            }
          })
          .catch((oError) => {
            // Handle any errors that occur during action execution
            console.error("Action failed:", oError);
            MessageToast.show("Error loading tree data");
          });
      },

      /**
       * Converts a flat array of nodes into a hierarchical structure
       * suitable for display in a Tree control.
       *
       * @param {Array} flatNodes - The flat list of nodes with parent references
       * @returns {Array} Hierarchical array of root nodes with nested children
       */
      _convertFlatToHierarchical: function (flatNodes) {
        if (!flatNodes || flatNodes.length === 0) {
          return [];
        }

        // Create a map for quick node lookups by NodeID
        var nodesMap = {};
        flatNodes.forEach(function (node) {
          // Shallow copy to avoid mutating original data
          var nodeCopy = Object.assign({}, node);
          // Initialize an empty children array
          nodeCopy.nodes = [];
          // Store in map using NodeID as the key
          nodesMap[node.NodeID] = nodeCopy;
        });

        // Build the hierarchical tree structure
        var rootNodes = [];
        flatNodes.forEach(function (node) {
          var nodeWithChildren = nodesMap[node.NodeID];

          if (node.ParentNodeID === null) {
            // Node has no parent; it's a root node
            rootNodes.push(nodeWithChildren);
          } else {
            // Node has a parent; append to parent's children array
            var parent = nodesMap[node.ParentNodeID];
            if (parent) {
              parent.nodes.push(nodeWithChildren);
            } else {
              // Log warning if parent node is missing
              console.warn("Parent not found for node:", node);
            }
          }
        });
        return rootNodes;
      },

      onToggleBeginColumn: function () {
        var sCurrentLayout = this.oFlexibleColumnLayout.getLayout();

        if (sCurrentLayout === "ThreeColumnsEndExpanded") {
          this.oFlexibleColumnLayout.setLayout("TwoColumnsMidExpanded");
        } else if (sCurrentLayout === "TwoColumnsMidExpanded") {
          this.oFlexibleColumnLayout.setLayout("ThreeColumnsEndExpanded");
        } else if (sCurrentLayout === "OneColumn") {
          this.oFlexibleColumnLayout.setLayout("TwoColumnsMidExpanded");
        }
      },

      onNavToBeginColumn: function () {
        this.oFlexibleColumnLayout.setLayout("OneColumn");
      },

      onNavToMidColumn: function () {
        this.oFlexibleColumnLayout.setLayout("TwoColumnsMidExpanded");
      },

      onMidColumnFullScreen: function () {
        this.oFlexibleColumnLayout.setLayout("MidColumnFullScreen");
      },

      onExitMidColumnFullScreen: function () {
        this.oFlexibleColumnLayout.setLayout("ThreeColumnsEndExpanded");
      },

      onShowEndColumn: function () {
        this.oFlexibleColumnLayout.setLayout("ThreeColumnsEndExpanded");
      },

      onEndColumnFullScreen: function () {
        this.oFlexibleColumnLayout.setLayout("EndColumnFullScreen");
      },

      onExitEndColumnFullScreen: function () {
        this.oFlexibleColumnLayout.setLayout("ThreeColumnsEndExpanded");
      },

      onCloseEndColumn: function () {
        this.oFlexibleColumnLayout.setLayout("TwoColumnsMidExpanded");
      },

      // Handle selecting a context‐node in the list
      onCNItemPress: function (oEvent) {
        // Get the clicked item
        const oItem = oEvent.getParameter("listItem");
        if (!oItem) {
          return;
        }

        // Get the binding context and node data
        const oCtx = oItem.getBindingContext("contextNodes");
        if (!oCtx) {
          return;
        }

        // Get node data from the context
        const oNode = oCtx.getObject();
        console.log(oNode);

        // Store the selected node's attributes
        this._selectedNodePath = oNode.path;
        this._selectedNodeIsFolder = oNode.isFolder;
        this._selectedNodeLabel = oNode.label;

        // If it's a data node with ID (not a folder), bind to the form
        if (oNode.ID) {
          const sPath = `/ContextNodes('${oNode.ID}')`;
          const oOData = this.getOwnerComponent().getModel();

          oOData
            .bindContext(sPath)
            .requestObject()
            .then(
              function () {
                this.byId("ContextNodeForm").bindElement({ path: sPath });
              }.bind(this)
            );
        }
      },

      // Create new Context Node dialog
      onCreateCNData: function () {
        // Store current path in a temporary variable that won't be reset
        if (this._selectedNodePath && !this._lastSelectedNodePath) {
          this._lastSelectedNodePath = this._selectedNodePath;
        }

        // Use either the currently selected path or the last stored path
        const pathToUse =
          this._selectedNodePath || this._lastSelectedNodePath || "";

        // Create form with the correct path
        const oForm = this._createCNFormForCreate(pathToUse);

        this.oCreateDialog = new Dialog({
          title: "Add New Context Node",
          content: [oForm],
          beginButton: new Button({
            text: "Create",
            enabled: false,
            press: this._createContextNodes.bind(this),
          }),
          endButton: new Button({
            text: "Cancel",
            press: () => {
              this.oCreateDialog.close();
            },
          }),
          afterClose: () => {
            this.oCreateDialog.destroy();
            this.oCreateDialog = null;
          },
        });
        this.getView().addDependent(this.oCreateDialog);
        this.oCreateDialog.open();
      },

      // Build form for creating a Context Node
      _createCNFormForCreate: function (initialPath) {
        return new SimpleForm({
          content: [
            new Label({ text: "Task ID" }),
            new Input("CNTaskIdCreate", {
              value: this._sTaskId,
              editable: false,
            }),
            new Label({ text: "Path" }),
            new Input("CNPathCreate", {
              placeholder: "e.g., documents.section1",
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
              value: initialPath,
            }),

            new FormattedText("CNPathHelpCreate", {
              htmlText:
                "<em>Supported formats:</em><br/>• Simple: <code>user.name</code><br/>• Arrays: <code>user.addresses[0].street</code><br/>• Nested: <code>data.items[2].properties[0].value</code>",
              width: "100%",
            }),

            new Label({ text: "Label" }),
            new Input("CNLabelCreate", {
              placeholder: "Enter label",
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),
            new Label({ text: "Type" }),
            new ComboBox("CNTypeCreate", {
              placeholder: "Select type",
              required: true,
              items: [
                new ListItem({ key: "string", text: "String" }),
                new ListItem({ key: "number", text: "Number" }),
                new ListItem({ key: "boolean", text: "Boolean" }),
                new ListItem({ key: "array", text: "Array" }),
                new ListItem({ key: "object", text: "Object" }),
                new ListItem({ key: "date", text: "Date" }),
              ],
              selectionChange: this._validateCNCreateForm.bind(this),
            }),

            new Label({ text: "Value" }),
            new TextArea("CNValueCreate", {
              placeholder: "Enter value (JSON format for objects/arrays)",
              required: true,
              rows: 3,
              liveChange: this._validateCNCreateForm.bind(this),
            }),
          ],
        });
      },

      // Enable Create button when all fields have values
      _validateCNCreateForm: function () {
        const fields = [
          "CNPathCreate",
          "CNLabelCreate",
          "CNTypeCreate",
          "CNValueCreate",
        ];
        const allFilled = fields.every((id) => {
          const inp = sap.ui.getCore().byId(id);
          return inp && inp.getValue().trim().length > 0;
        });
        this.oCreateDialog.getBeginButton().setEnabled(allFilled);
      },

      // Send new Context Node to the backend
      _createContextNodes: async function () {
        const sPath = Element.getElementById("CNPathCreate").getValue();
        const sLabel = Element.getElementById("CNLabelCreate").getValue();
        const sType = Element.getElementById("CNTypeCreate").getValue();
        const sValue = Element.getElementById("CNValueCreate").getValue();

        const oNew = {
          task_ID: this._sTaskId,
          path: sPath,
          label: sLabel,
          type: sType,
          value: sValue,
        };

        const oModel = this.getOwnerComponent().getModel();
        const oBinding = oModel.bindList("/ContextNodes");
        await oBinding.create(oNew).created();
        MessageToast.show("Context Node created");

        // await this.getOwnerComponent()._loadContextNodes(this._sTaskId);
        this.loadContextNodesTree();
        this.oCreateDialog.close();
      },

      onDeleteCNData: function () {
        const oTree = this.byId("contextTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected) {
          MessageToast.show("Please select a context node to delete!");
          return;
        }

        const oJsonCtx = oSelected.getBindingContext("contextNodes");
        const sID = oJsonCtx.getProperty("ID");

        if (!sID) {
          MessageToast.show("Select one context node item first!");
          return;
        }

        const sPath = "/ContextNodes('" + sID + "')";

        MessageBox.confirm(
          "Are you sure you want to delete this context node?",
          {
            title: "Confirm Deletion",
            icon: MessageBox.Icon.WARNING,
            actions: [MessageBox.Action.OK, MessageBox.Action.CANCEL],
            emphasizedAction: MessageBox.Action.CANCEL,
            onClose: async (sAction) => {
              if (sAction === MessageBox.Action.OK) {
                try {
                  const oODataModel = this.getOwnerComponent().getModel();
                  const oContext = oODataModel.bindContext(sPath);

                  await oContext.requestObject();
                  const oBoundContext = oContext.getBoundContext();

                  if (oBoundContext) {
                    await oBoundContext.delete();
                    MessageToast.show("Context node deleted successfully.");

                    this.loadContextNodesTree();
                  } else {
                    MessageToast.show("Could not find context with ID: " + sID);
                  }
                } catch (oError) {
                  console.error("Delete error:", oError);
                  MessageToast.show("Error: " + (oError.message || oError));
                }
              }
            },
          }
        );
      },

      // Edit an existing Context Node
      onEditCNData: function () {
        const oTree = this.byId("contextTree");
        const oSel = oTree.getSelectedItem();
        const oCtx = oSel.getBindingContext("contextNodes");
        const sId = oCtx.getProperty("ID");

        if (!sId) {
          MessageToast.show("Please select a context node to edit!");
          return;
        }

        const data = {
          path: oCtx.getProperty("path"),
          label: oCtx.getProperty("label"),
          type: oCtx.getProperty("type"),
          value: oCtx.getProperty("value"),
        };

        const oForm = this._createCNFormForEdit(data);
        this.oEditDialog = new Dialog({
          title: "Edit Context Node",
          content: [oForm],
          beginButton: new Button({
            text: "Update",
            press: this._updateContextNodes.bind(this, sId),
          }),
          endButton: new Button({
            text: "Cancel",
            press: () => this.oEditDialog.close(),
          }),
          afterClose: () => {
            this.oEditDialog.destroy();
            this.oEditDialog = null;
          },
        });

        this.getView().addDependent(this.oEditDialog);
        this.oEditDialog.open();
      },

      // Build form for editing a Context Node
      _createCNFormForEdit: function (oData) {
        return new SimpleForm({
          content: [
            new Label({ text: "Task ID" }),
            new Input("CNTaskIdEdit", {
              value: this._sTaskId,
              editable: false,
            }),
            new Label({ text: "Path" }),
            new Input("CNPathEdit", {
              value: oData.path,
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),

            new FormattedText("CNPathHelpEdit", {
              htmlText:
                "<em>Supported formats:</em><br/>• Simple: <code>user.name</code><br/>• Arrays: <code>user.addresses[0].street</code><br/>• Nested: <code>data.items[2].properties[0].value</code>",
              width: "100%",
            }),

            new Label({ text: "Label" }),
            new Input("CNLabelEdit", {
              value: oData.label,
              required: true,
              liveChange: this._validateCNCreateForm.bind(this),
            }),
            new Label({ text: "Type" }),
            new ComboBox("CNTypeEdit", {
              value: oData.type,
              selectedKey: oData.type,
              required: true,
              items: [
                new ListItem({ key: "string", text: "String" }),
                new ListItem({ key: "number", text: "Number" }),
                new ListItem({ key: "boolean", text: "Boolean" }),
                new ListItem({ key: "array", text: "Array" }),
                new ListItem({ key: "object", text: "Object" }),
                new ListItem({ key: "date", text: "Date" }),
              ],
              selectionChange: this._validateCNCreateForm.bind(this),
            }),

            new Label({ text: "Value" }),
            new TextArea("CNValueEdit", {
              value: oData.value,
              required: true,
              rows: 3,
              liveChange: this._validateCNCreateForm.bind(this),
            }),
          ],
        });
      },

      // Submit changes for an existing Context Node
      _updateContextNodes: async function (sContextNodeId) {
        const sPath = Element.getElementById("CNPathEdit").getValue();
        const sLabel = Element.getElementById("CNLabelEdit").getValue();
        const sType = Element.getElementById("CNTypeEdit").getValue();
        const sValue = Element.getElementById("CNValueEdit").getValue();

        try {
          const oModel = this.getOwnerComponent().getModel();
          const sEntityPath = `/ContextNodes('${sContextNodeId}')`;
          const oContext = oModel.bindContext(sEntityPath);

          await oContext.requestObject();
          const oBound = oContext.getBoundContext();
          if (!oBound) {
            MessageToast.show("Context node not found");
            return;
          }

          oBound.setProperty("path", sPath);
          oBound.setProperty("label", sLabel);
          oBound.setProperty("type", sType);
          oBound.setProperty("value", sValue);

          MessageToast.show("Context Node updated");
          this.oEditDialog.close();
          this.loadContextNodesTree();
          this._refreshCNContent();
        } catch (err) {
          console.error("Update error:", err);
          MessageToast.show("Error updating context node: " + err.message);
        }
      },

      // Refresh the bound ContextNode form
      _refreshCNContent: function () {
        const oForm = this.byId("ContextNodeForm");
        const oBind = oForm.getElementBinding();
        if (oBind) {
          oBind.refresh();
        }
      },

      // ---------------------------------------Context Tree -------------------------------------

      // -----------------------------------------Task Tree --------------------------------------
      // This is Detail page
      onTaskSelect: function (oEvent) {
        var oSelectedItem = oEvent.getParameter("listItem");
        var oContext = oSelectedItem.getBindingContext("taskTree");

        if (!oContext) {
          console.error("No context found!");
          return;
        }

        var sID = oContext.getProperty("ID");
        var sType = oContext.getProperty("type");

        var oTree = this.byId("taskAndBotTree");
        var oBinding = oTree.getBinding("items");
        var iItemIndex = oTree.indexOfItem(oSelectedItem);
        var oSelectedContext = oBinding.getContextByIndex(iItemIndex);

        if (!oSelectedContext) {
          return;
        }

        var oSelectedNode = oSelectedContext.getProperty();

        if (!oSelectedNode.nodes) {
          oSelectedNode.nodes = [];
        }

        const oModel = this.getOwnerComponent().getModel();

        if (sType === "task") {
          oModel
            .bindList("/Tasks('" + sID + "')/botInstances")
            .requestContexts()
            .then(
              function (aContexts) {
                var aData = aContexts.map(function (oContext) {
                  var oObj = oContext.getObject();
                  oObj.type = "bot";
                  oObj.nodes = [];
                  return oObj;
                });

                aData.forEach(function (newItem) {
                  var isDuplicate = oSelectedNode.nodes.some(function (
                    existingItem
                  ) {
                    return existingItem.ID === newItem.ID;
                  });

                  if (!isDuplicate) {
                    oSelectedNode.nodes.push(newItem);
                  }
                });

                // Refresh tree
                this.getOwnerComponent().getModel("taskTree").updateBindings();
                oTree.expand(iItemIndex);
              }.bind(this)
            );
        } else if (sType === "bot") {
          oModel
            .bindList("/BotInstances('" + sID + "')/tasks")
            .requestContexts()
            .then(
              function (aContexts) {
                var aData = aContexts.map(function (oContext) {
                  var oObj = oContext.getObject();
                  oObj.type = "task";
                  oObj.nodes = [];
                  return oObj;
                });

                aData.forEach(function (newItem) {
                  var isDuplicate = oSelectedNode.nodes.some(function (
                    existingItem
                  ) {
                    return existingItem.ID === newItem.ID;
                  });

                  if (!isDuplicate) {
                    oSelectedNode.nodes.push(newItem);
                  }
                });

                // Refresh tree
                this.getOwnerComponent().getModel("taskTree").updateBindings();
                oTree.expand(iItemIndex);
              }.bind(this)
            );
        }
      },

      // -----------------------------------------Task Tree --------------------------------------

      onCreateSubTask: function () {
        var oTree = this.byId("taskAndBotTree");
        var oSelectedItem = oTree.getSelectedItem();

        if (!oSelectedItem) {
          MessageToast.show("Select the bot instance first");
          return;
        }

        var oContext = oSelectedItem.getBindingContext("taskTree");
        var sNodeType = oContext.getProperty("type");

        if (sNodeType !== "bot") {
          MessageToast.show(
            "New tasks can only be created under a Bot Instance"
          );
          return;
        }

        this._selectedBotInstanceId = oContext.getProperty("ID");

        this._openCreateTaskDialog();
      },

      _openCreateTaskDialog: function () {
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
                this._createSubTask();
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

        // Reset form values
        if (Element.getElementById("taskNameDetail")) {
          Element.getElementById("taskNameDetail").setValue("");
        }
        if (Element.getElementById("taskDescriptionDetail")) {
          Element.getElementById("taskDescriptionDetail").setValue("");
        }
        if (Element.getElementById("taskTypeIdDetail")) {
          Element.getElementById("taskTypeIdDetail").setValue("");
        }

        // Disable create button initially
        this.oSubmitDialogTaskTree.getBeginButton().setEnabled(false);

        this.oSubmitDialogTaskTree.open();
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
                  Element.getElementById("taskTypeIdDetail").setValue(
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
            new Input("taskNameDetail", {
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
            new TextArea("taskDescriptionDetail", {
              placeholder: "Enter task description",
              rows: 3,
            }),

            new Label({ text: "Type id" }),
            new Input("taskTypeIdDetail", {
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

      _createSubTask: function () {
        if (!this._selectedBotInstanceId) {
          MessageToast.show("Bot Instance not found");
          return;
        }

        const sTaskName = Element.getElementById("taskNameDetail").getValue();
        const sTaskDescription = Element.getElementById(
          "taskDescriptionDetail"
        ).getValue();
        const sTaskTypeId =
          Element.getElementById("taskTypeIdDetail").getValue();

        try {
          var oPayload = {
            name: sTaskName,
            description: sTaskDescription,
            isMain: false,
            sequence: 0,
            contextPath: "",
            botInstance_ID: this._selectedBotInstanceId,
          };

          if (sTaskTypeId && sTaskTypeId !== "") {
            oPayload.type_ID = sTaskTypeId;
          }

          var oContext = this.getOwnerComponent()
            .getModel()
            .bindList("/Tasks")
            .create(oPayload);

          oContext
            .created()
            .then(
              function () {
                MessageToast.show("Sub Task successfully created");

                var sBotId = this._selectedBotInstanceId;
                this._selectedBotInstanceId = null;

                this._refreshSelectedBotNode(sBotId);

                this.getView().setBusy(false);
              }.bind(this)
            )
            .catch(
              function (oError) {
                MessageBox.error("Error creating Sub Task: " + oError.message);
                this.getView().setBusy(false);
              }.bind(this)
            );
        } catch (oError) {
          MessageBox.error("Error: " + oError.message);
          this.getView().setBusy(false);
        }
      },

      _refreshSelectedBotNode: function (sBotId) {
        var oTree = this.byId("taskAndBotTree");
        if (!oTree) {
          MessageToast.show("Tree control not found");
          return;
        }

        var oTreeModel = this.getOwnerComponent().getModel("taskTree");

        this.getView().setBusy(true);

        try {
          var oModel = this.getOwnerComponent().getModel();

          oModel
            .bindList("/BotInstances('" + sBotId + "')/tasks")
            .requestContexts()
            .then(
              function (aContexts) {
                var aTasks = aContexts.map(function (oContext) {
                  var oTask = oContext.getObject();
                  oTask.type = "task";
                  oTask.nodes = [];
                  return oTask;
                });

                this._updateBotNodeChildren(
                  oTreeModel.getData(),
                  sBotId,
                  aTasks
                );

                oTreeModel.refresh(true);

                this._expandBotNode(sBotId);

                this.getView().setBusy(false);
              }.bind(this)
            )
            .catch(
              function (oError) {
                MessageToast.show(
                  "Error refreshing bot node: " + oError.message
                );
                this.getView().setBusy(false);
              }.bind(this)
            );
        } catch (oError) {
          MessageToast.show("Error: " + oError.message);
          this.getView().setBusy(false);
        }
      },

      _updateBotNodeChildren: function (oNodes, sBotId, aNewChildren) {
        var aNodes = Array.isArray(oNodes) ? oNodes : [oNodes];

        for (var i = 0; i < aNodes.length; i++) {
          var oNode = aNodes[i];

          if (oNode.type === "bot" && oNode.ID === sBotId) {
            oNode.nodes = aNewChildren;
            return true;
          }

          if (oNode.nodes && oNode.nodes.length > 0) {
            var bFound = this._updateBotNodeChildren(
              oNode.nodes,
              sBotId,
              aNewChildren
            );
            if (bFound) {
              return true;
            }
          }
        }

        return false;
      },

      _expandBotNode: function (sBotId) {
        var oTree = this.byId("taskAndBotTree");
        var aItems = oTree.getItems();

        for (var i = 0; i < aItems.length; i++) {
          var oItem = aItems[i];
          var oContext = oItem.getBindingContext("taskTree");

          if (
            oContext &&
            oContext.getProperty("type") === "bot" &&
            oContext.getProperty("ID") === sBotId
          ) {
            oTree.expand(oTree.indexOfItem(oItem));
            break;
          }
        }
      },

      onEditSubTask: function () {
        const oTree = this.byId("taskAndBotTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected || oSelected.getProperty("type") == "bot") {
          MessageToast.show("Please select a task to edit!");
          return;
        }
      },

      onDeleteSubTask: function () {
        const oTree = this.byId("taskAndBotTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected) {
          MessageToast.show("Please select task to delete!");
          return;
        }

        const oJsonCtx = oSelected.getBindingContext("taskTree");
        const sID = oJsonCtx.getProperty("ID");

        if (!sID) {
          MessageToast.show("Select one task item first!");
          return;
        }

        const sPath = "/Tasks('" + sID + "')";

        MessageBox.confirm("Are you sure you want to delete this task?", {
          title: "Confirm Deletion",
          icon: MessageBox.Icon.WARNING,
          actions: [MessageBox.Action.OK, MessageBox.Action.CANCEL],
          emphasizedAction: MessageBox.Action.CANCEL,
          onClose: async (sAction) => {
            if (sAction === MessageBox.Action.OK) {
              try {
                const oODataModel = this.getOwnerComponent().getModel();
                const oContext = oODataModel.bindContext(sPath);

                await oContext.requestObject();
                const oBoundContext = oContext.getBoundContext();

                if (oBoundContext) {
                  await oBoundContext.delete();
                  MessageToast.show("Task deleted successfully.");
                  await this.getOwnerComponent()._loadMainTasks(this._sTaskId);
                } else {
                  MessageToast.show("Could not find task with ID: " + sID);
                }
              } catch (oError) {
                console.error("Delete error:", oError);
                MessageToast.show("Error: " + (oError.message || oError));
              }
            }
          },
        });
      },

      // ---------------------------------------Bot Instance -------------------------------------

      onCreateBotInstance: function () {
        var oTree = this.byId("taskAndBotTree");
        var oSelectedItem = oTree.getSelectedItem();

        if (!oSelectedItem) {
          MessageToast.show("Select the task first!");
          return;
        }

        var oContext = oSelectedItem.getBindingContext("taskTree");
        var sNodeType = oContext.getProperty("type");

        if (sNodeType !== "task") {
          MessageToast.show("Bot instance only created in the node Task");
          return;
        }

        var sTaskId = oContext.getProperty("ID");
        var sTaskName = oContext.getProperty("name");

        if (!this.oBotDialog) {
          this.oBotDialog = new Dialog({
            type: DialogType.Message,
            title: "Create",
            content: [this._createBotForm(sTaskName)],
            beginButton: new Button({
              type: ButtonType.Emphasized,
              text: "Create",
              press: function () {
                this._createBot(sTaskId);
                this.oBotDialog.close();
              }.bind(this),
            }),
            endButton: new Button({
              text: "Cancel",
              press: function () {
                this.oBotDialog.close();
              }.bind(this),
            }),
          });
        }

        this.oBotDialog.open();
      },

      _createSelectBotTypeDialog: function () {
        return this.oSelectBotTypeDialog
          ? this.oSelectBotTypeDialog
          : new SelectDialog({
              noDataText: "No bot types found",
              title: "Select Bot Type",
              items: {
                path: "/BotTypes",
                template: new sap.m.StandardListItem({
                  title: "{name}",
                  description: "{description}",
                  highlightText: "{ID}",
                }),
              },
              confirm: function (oEvent) {
                const oSelectedItem = oEvent.getParameter("selectedItem");
                if (oSelectedItem) {
                  Element.getElementById("botTypeId").setValue(
                    oSelectedItem.getHighlightText()
                  );
                }
              }.bind(this),
            });
      },

      _createBotForm: function (sTaskName) {
        return new SimpleForm({
          content: [
            new Label({ text: "Task" }),
            new Input({
              value: sTaskName,
              editable: false,
            }),
            new Label({ text: "Bot Type" }),
            new Input("botTypeId", {
              showValueHelp: true,
              valueHelpOnly: true,
              valueHelpRequest: function () {
                this.oSelectBotTypeDialog = this._createSelectBotTypeDialog();
                this.oSelectBotTypeDialog.setModel(
                  this.getOwnerComponent().getModel()
                );
                this.oSelectBotTypeDialog.open();
              }.bind(this),
            }),
            new Label({ text: "Sequence" }),
            new StepInput("botSequence", { min: 1, max: 999 }),
          ],
        });
      },

      _createBot: function (sTaskId) {
        const sBotTypeId = Element.getElementById("botTypeId").getValue();
        const sBotSequence = Element.getElementById("botSequence").getValue();
        const oNewBot = {
          sequence: sBotSequence,
          type_ID: sBotTypeId == "" ? null : sBotTypeId,
          task_ID: sTaskId == "" ? null : sTaskId,
        };
        const oModel = this.getOwnerComponent()
          .getModel()
          .bindList("/BotInstances");
        const oContext = oModel.create(oNewBot);
        oContext
          .created()
          .then(
            function () {
              MessageToast.show("Bot created successfully");
              this._refreshSelectedTaskNode(sTaskId);
            }.bind(this)
          )
          .catch(
            function (oError) {
              MessageToast.show("Error creating bot: " + oError.message);
            }.bind(this)
          );
      },

      onDeleteBotInstance: function () {
        const oTree = this.byId("taskAndBotTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected) {
          MessageToast.show("Please select bot to delete!");
          return;
        }

        const oJsonCtx = oSelected.getBindingContext("taskTree");
        const sID = oJsonCtx.getProperty("ID");

        if (!sID) {
          MessageToast.show("Select one bot item first!");
          return;
        }

        const sPath = "/BotInstances('" + sID + "')";

        MessageBox.confirm(
          "Are you sure you want to delete this bot instances?",
          {
            title: "Confirm Deletion",
            icon: MessageBox.Icon.WARNING,
            actions: [MessageBox.Action.OK, MessageBox.Action.CANCEL],
            emphasizedAction: MessageBox.Action.CANCEL,
            onClose: async (sAction) => {
              if (sAction === MessageBox.Action.OK) {
                try {
                  const oODataModel = this.getOwnerComponent().getModel();
                  const oContext = oODataModel.bindContext(sPath);

                  await oContext.requestObject();
                  const oBoundContext = oContext.getBoundContext();

                  if (oBoundContext) {
                    await oBoundContext.delete();
                    MessageToast.show("Bot Instances deleted successfully.");
                    await this.getOwnerComponent()._loadMainTasks(
                      this._sTaskId
                    );
                  } else {
                    MessageToast.show("Could not find bot with ID: " + sID);
                  }
                } catch (oError) {
                  console.error("Delete error:", oError);
                  MessageToast.show("Error: " + (oError.message || oError));
                }
              }
            },
          }
        );
      },

      _refreshSelectedTaskNode: function (sTaskId) {
        var oTree = this.byId("taskAndBotTree");
        if (!oTree) {
          MessageToast.show("Tree control not found");
          return;
        }

        var oTreeModel = this.getOwnerComponent().getModel("taskTree");

        this.getView().setBusy(true);

        try {
          var oModel = this.getOwnerComponent().getModel();

          oModel
            .bindList("/Tasks('" + sTaskId + "')/botInstances")
            .requestContexts()
            .then(
              function (aContexts) {
                var aBotInstances = aContexts.map(function (oContext) {
                  var oBot = oContext.getObject();
                  oBot.type = "bot";
                  oBot.nodes = [];
                  return oBot;
                });

                this._updateTaskNodeChildren(
                  oTreeModel.getData(),
                  sTaskId,
                  aBotInstances
                );

                oTreeModel.refresh(true);

                this._expandTaskNode(sTaskId);

                this.getView().setBusy(false);
              }.bind(this)
            )
            .catch(
              function (oError) {
                MessageToast.show(
                  "Error refreshing task node: " + oError.message
                );
                this.getView().setBusy(false);
              }.bind(this)
            );
        } catch (oError) {
          MessageToast.show("Error: " + oError.message);
          this.getView().setBusy(false);
        }
      },

      _updateTaskNodeChildren: function (oNodes, sTaskId, aNewChildren) {
        var aNodes = Array.isArray(oNodes) ? oNodes : [oNodes];

        for (var i = 0; i < aNodes.length; i++) {
          var oNode = aNodes[i];

          if (oNode.type === "task" && oNode.ID === sTaskId) {
            oNode.nodes = aNewChildren;
            return true;
          }

          if (oNode.nodes && oNode.nodes.length > 0) {
            var bFound = this._updateTaskNodeChildren(
              oNode.nodes,
              sTaskId,
              aNewChildren
            );
            if (bFound) {
              return true;
            }
          }
        }

        return false;
      },

      _expandTaskNode: function (sTaskId) {
        var oTree = this.byId("taskAndBotTree");
        var aItems = oTree.getItems();

        for (var i = 0; i < aItems.length; i++) {
          var oItem = aItems[i];
          var oContext = oItem.getBindingContext("taskTree");

          if (
            oContext &&
            oContext.getProperty("type") === "task" &&
            oContext.getProperty("ID") === sTaskId
          ) {
            oTree.expand(oTree.indexOfItem(oItem));
            break;
          }
        }
      },
      // ---------------------------------------Chat Bot -------------------------------------
      // conversationHistory: [],

      // parseAIResponse: function (response) {
      //   const parts = [];
      //   const codeBlockRegex = /```(\w+)?\n?([\s\S]*?)```|`([^`]+)`/g;

      //   let lastIndex = 0;
      //   let match;
      //   let partIndex = 0;

      //   while ((match = codeBlockRegex.exec(response)) !== null) {
      //     // Add text before the code block
      //     if (match.index > lastIndex) {
      //       const textBefore = response
      //         .substring(lastIndex, match.index)
      //         .trim();
      //       if (textBefore) {
      //         parts.push({
      //           type: "text",
      //           content: textBefore,
      //           index: partIndex++,
      //         });
      //       }
      //     }

      //     // Add the code block
      //     if (match[0].startsWith("```")) {
      //       // Multi-line code block
      //       parts.push({
      //         type: "code_block",
      //         language: match[1] || "text",
      //         content: match[2] ? match[2].trim() : "",
      //         raw: match[0],
      //         index: partIndex++,
      //       });
      //     } else {
      //       // Inline code
      //       parts.push({
      //         type: "inline_code",
      //         content: match[3] || "",
      //         raw: match[0],
      //         index: partIndex++,
      //       });
      //     }

      //     lastIndex = match.index + match[0].length;
      //   }

      //   // Add remaining text
      //   if (lastIndex < response.length) {
      //     const remainingText = response.substring(lastIndex).trim();
      //     if (remainingText) {
      //       parts.push({
      //         type: "text",
      //         content: remainingText,
      //         index: partIndex++,
      //       });
      //     }
      //   }

      //   // If no matches found, return the entire response as text
      //   if (parts.length === 0) {
      //     parts.push({
      //       type: "text",
      //       content: response.trim(),
      //       index: 0,
      //     });
      //   }

      //   return parts;
      // },

      // // Helper function to convert parsed response back to plain text for history
      // parsedResponseToText: function (parsedResponse) {
      //   if (typeof parsedResponse === "string") {
      //     return parsedResponse;
      //   }

      //   if (Array.isArray(parsedResponse)) {
      //     return parsedResponse
      //       .map((part) => {
      //         if (part.type === "text") {
      //           return part.content;
      //         } else if (part.type === "code_block") {
      //           return `\`\`\`${part.language || ""}\n${part.content}\n\`\`\``;
      //         } else if (part.type === "inline_code") {
      //           return `\`${part.content}\``;
      //         }
      //         return part.content || "";
      //       })
      //       .join("");
      //   }

      //   return String(parsedResponse);
      // },

      // // Function to manage conversation history
      // addToHistory: function (role, content) {
      //   // Convert parsed content to plain text for API
      //   const textContent = this.parsedResponseToText(content);

      //   this.conversationHistory.push({
      //     role: role === "user" ? "user" : "model", // Gemini uses "model" instead of "assistant"
      //     parts: [
      //       {
      //         text: textContent,
      //       },
      //     ],
      //   });

      //   // Optional: Limit history to prevent token overflow (keep last 20 exchanges)
      //   const maxHistoryLength = 40; // 20 user + 20 AI messages
      //   if (this.conversationHistory.length > maxHistoryLength) {
      //     this.conversationHistory = this.conversationHistory.slice(
      //       -maxHistoryLength
      //     );
      //   }
      // },

      // // Function to clear conversation history
      // clearHistory: function () {
      //   this.conversationHistory = [];
      // },

      onSubmitQuery: async function () {
        var oInput = this.byId("chatInput");
        var sMessage = oInput.getValue().trim();

        if (sMessage) {
          try {
            // Tampilkan pesan pengguna
            this.addChatMessage(sMessage, "user");

            // Tambahkan ke history lokal
            // this.addToHistory("user", sMessage);

            // Kosongkan input
            oInput.setValue("");

            // Tandai sedang loading
            // this.getView().getModel("ui").setProperty("/busy", true);
            // ID Bot Instance (ganti dengan cara mendapatkan ID yang sesuai)
            const botInstanceId = "'880e8400-e29b-41d4-a716-446655440500'"; // Ganti dengan ID yang valid

            // Panggil backend CAP Java
            const response = await fetch(
              `/odata/v4/MainService/BotInstances(${botInstanceId})/MainService.chatCompletion`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                  // Tambahkan CSRF token jika diperlukan
                  // "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                  content: sMessage,
                }),
              }
            );

            const resData = await response.json();
            const reply = resData.value;

            // Matikan loading
            // this.getView().getModel("ui").setProperty("/busy", false);

            // if (!response.ok) {
            //   throw new Error(`HTTP error ${response.status}`);
            // }

            // // Parse response
            // const data = await response.json();
            // const reply = data.value;

            // // Parsing dan tampilkan response
            // const parsedResponse = this.parseAIResponse(reply);
            this.addChatMessage(reply, "ai");

            // Tambahkan ke history lokal
            // this.addToHistory("ai", parsedResponse);
          } catch (error) {
            // Matikan loading
            // this.getView().getModel("ui").setProperty("/busy", false);

            // this.addChatMessage(errorMessage, "ai");
            console.error("Error in chat completion:", error);
          }
        }
      },

      // // Add these properties to your controller
      // _chatHistory: [], // Array to store chat sessions
      // _currentChatId: null, // Current active chat session ID
      // _currentMessages: [], // Current chat messages

      // // Modified addChatMessage function
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

        //   // Handle parsed AI responses
        if (sType === "ai" && Array.isArray(sMessage)) {
          // Handle regular string messages
          const messageContent =
            typeof sMessage === "string" ? sMessage : JSON.stringify(sMessage);
          // create chat bubble if there's text content
          var oHTML = new sap.ui.core.HTML({
            content: `
                    <div class="chatBubbleContainer ${sType}">
                        <div class="chatBubble ${sType}">
                            <div>${messageContent}</div>
                            <div class="chatTimestamp">${sTimestamp}</div>
                        </div>
                    </div>
                `,
          });
          oChatBox.addItem(oHTML);
        } else {
          // Handle regular string messages
          const messageContent =
            typeof sMessage === "string" ? sMessage : JSON.stringify(sMessage);

          var oHTML = new sap.ui.core.HTML({
            content: `
                <div class="chatBubbleContainer ${sType}">
                    <div class="chatBubble ${sType}">
                        <div>${messageContent}</div>
                        <div class="chatTimestamp">${sTimestamp}</div>
                    </div>
                </div>
            `,
          });
          oChatBox.addItem(oHTML);
        }
      },

      // // Helper function to generate unique message IDs
      // _generateMessageId: function () {
      //   return (
      //     "msg_" + Date.now() + "_" + Math.random().toString(36).substr(2, 9)
      //   );
      // },

      // // Helper function to generate unique chat session IDs
      // _generateChatId: function () {
      //   return (
      //     "chat_" + Date.now() + "_" + Math.random().toString(36).substr(2, 9)
      //   );
      // },

      // // Function to start a new chat session
      // startNewChat: function () {
      //   // Save current chat if it has messages
      //   if (this._currentMessages.length > 0) {
      //     this._saveChatSession();
      //   }

      //   // Clear current chat
      //   this._currentChatId = this._generateChatId();
      //   this._currentMessages = [];

      //   // Clear chat UI
      //   var oChatBox = this.byId("chatMessagesBox");
      //   if (oChatBox) {
      //     oChatBox.destroyItems();
      //   }

      //   // Clear code results
      //   var oCodeResultText = this.byId("codeResultText");
      //   if (oCodeResultText) {
      //     oCodeResultText.setContent("");
      //   }
      // },

      // // Function to save current chat session to history
      // _saveChatSession: function () {
      //   if (this._currentMessages.length === 0) return;

      //   var oChatSession = {
      //     id: this._currentChatId || this._generateChatId(),
      //     title: this._generateChatTitle(),
      //     messages: [...this._currentMessages],
      //     createdAt: new Date().toISOString(),
      //     lastUpdated: new Date().toISOString(),
      //   };

      //   // Find existing session or add new one
      //   var existingIndex = this._chatHistory.findIndex(
      //     (chat) => chat.id === oChatSession.id
      //   );
      //   if (existingIndex >= 0) {
      //     this._chatHistory[existingIndex] = oChatSession;
      //   } else {
      //     this._chatHistory.unshift(oChatSession); // Add to beginning
      //   }

      //   // Limit history to last 50 chats
      //   if (this._chatHistory.length > 50) {
      //     this._chatHistory = this._chatHistory.slice(0, 50);
      //   }

      //   // Save to local storage for persistence
      //   this._saveHistoryToStorage();
      // },

      // // Function to update current chat in history (called after each message)
      // _updateCurrentChatInHistory: function () {
      //   if (!this._currentChatId) {
      //     this._currentChatId = this._generateChatId();
      //   }
      //   this._saveChatSession();
      // },

      // // Generate a chat title based on first user message
      // _generateChatTitle: function () {
      //   var firstUserMessage = this._currentMessages.find(
      //     (msg) => msg.type === "user"
      //   );
      //   if (firstUserMessage) {
      //     var title =
      //       typeof firstUserMessage.message === "string"
      //         ? firstUserMessage.message
      //         : "Chat";
      //     return title.length > 50 ? title.substring(0, 50) + "..." : title;
      //   }
      //   return "New Chat - " + new Date().toLocaleDateString();
      // },

      // // Load chat history from storage
      // _loadHistoryFromStorage: function () {
      //   try {
      //     var storedHistory = localStorage.getItem("chatHistory");
      //     if (storedHistory) {
      //       this._chatHistory = JSON.parse(storedHistory);
      //     }
      //   } catch (error) {
      //     console.error("Error loading chat history:", error);
      //     this._chatHistory = [];
      //   }
      // },

      // // Save chat history to storage
      // _saveHistoryToStorage: function () {
      //   try {
      //     localStorage.setItem(
      //       "chatHistory",
      //       JSON.stringify(this._chatHistory)
      //     );
      //   } catch (error) {
      //     console.error("Error saving chat history:", error);
      //   }
      // },

      // // Function to load a specific chat session
      // loadChatSession: function (sChatId) {
      //   var oChatSession = this._chatHistory.find(
      //     (chat) => chat.id === sChatId
      //   );
      //   if (!oChatSession) {
      //     console.error("Chat session not found:", sChatId);
      //     return;
      //   }

      //   // Save current chat before switching
      //   if (this._currentMessages.length > 0) {
      //     this._saveChatSession();
      //   }

      //   // Set current chat
      //   this._currentChatId = oChatSession.id;
      //   this._currentMessages = [...oChatSession.messages];

      //   // Clear and rebuild chat UI
      //   this._rebuildChatUI();

      //   // Close history dialog
      //   this.onCloseChatHistory();
      // },

      // // Function to rebuild chat UI from stored messages
      // _rebuildChatUI: function () {
      //   var oChatBox = this.byId("chatMessagesBox");
      //   var oCodeResultText = this.byId("codeResultText");

      //   // Clear existing content
      //   if (oChatBox) {
      //     oChatBox.destroyItems();
      //   }
      //   if (oCodeResultText) {
      //     oCodeResultText.setContent("");
      //   }

      //   // Rebuild messages
      //   this._currentMessages.forEach((oMessage) => {
      //     this._recreateMessageUI(oMessage);
      //   });

      //   // Scroll to bottom
      //   setTimeout(() => {
      //     var oScrollContainer = this.byId("chatMessagesContainer");
      //     if (oScrollContainer && oScrollContainer.getDomRef("scroll")) {
      //       oScrollContainer.scrollTo(
      //         0,
      //         oScrollContainer.getDomRef("scroll").scrollHeight
      //       );
      //     }
      //   }, 100);
      // },

      // // Function to recreate message UI from stored data
      // _recreateMessageUI: function (oMessage) {
      //   var oChatBox = this.byId("chatMessagesBox");
      //   var sType = oMessage.type;
      //   var sTimestamp = oMessage.timestamp;

      //   if (sType === "ai" && oMessage.processedText !== undefined) {
      //     // Handle parsed AI responses
      //     if (oMessage.processedText) {
      //       var oHTML = new sap.ui.core.HTML({
      //         content: `
      //               <div class="chatBubbleContainer ${sType}">
      //                   <div class="chatBubble ${sType}">
      //                       <div>${oMessage.processedText}</div>
      //                       <div class="chatTimestamp">${sTimestamp}</div>
      //                   </div>
      //               </div>
      //           `,
      //       });
      //       oChatBox.addItem(oHTML);
      //     }

      //     // Recreate code blocks
      //     if (oMessage.codeBlocks && oMessage.codeBlocks.length > 0) {
      //       var oCodeResultText = this.byId("codeResultText");
      //       if (oCodeResultText) {
      //         const codeSections = oMessage.codeBlocks.map((block) => {
      //           const language = block.language || "code";
      //           const langLabel = `<div class="codeLangLabel">${language.toUpperCase()}</div>`;
      //           const codeBlock = `
      //                   <div class="codeBlockWrapper">
      //                       ${langLabel}
      //                       <pre><code class="language-${language}">${this.escapeHtml(
      //             block.content
      //           )}</code></pre>
      //                   </div>
      //               `;
      //           return codeBlock;
      //         });
      //         oCodeResultText.setContent(codeSections.join("<br/>"));
      //       }
      //     }
      //   } else {
      //     // Handle regular messages
      //     const messageContent =
      //       typeof oMessage.message === "string"
      //         ? oMessage.message
      //         : JSON.stringify(oMessage.message);

      //     var oHTML = new sap.ui.core.HTML({
      //       content: `
      //           <div class="chatBubbleContainer ${sType}">
      //               <div class="chatBubble ${sType}">
      //                   <div>${messageContent}</div>
      //                   <div class="chatTimestamp">${sTimestamp}</div>
      //               </div>
      //           </div>
      //       `,
      //     });
      //     oChatBox.addItem(oHTML);
      //   }
      // },

      // // Enhanced onHistoryPress function
      // onHistoryPress: async function () {
      //   // Load history from storage
      //   this._loadHistoryFromStorage();

      //   if (!this._oHistoryDialog) {
      //     this._oHistoryDialog = await Fragment.load({
      //       id: this.getView().getId(),
      //       name: "task-runtime.view.ChatHistory",
      //       controller: this,
      //     });
      //   }

      //   // Bind history data to the dialog
      //   this._bindHistoryData();
      //   this._oHistoryDialog.open();
      // },

      // // Function to bind history data to the dialog
      // _bindHistoryData: function () {
      //   var oHistoryList = this.byId("chatHistoryList");
      //   if (oHistoryList && this._chatHistory.length > 0) {
      //     var oModel = new sap.ui.model.json.JSONModel({
      //       chatHistory: this._chatHistory,
      //     });
      //     oHistoryList.setModel(oModel);
      //     oHistoryList.bindItems({
      //       path: "/chatHistory",
      //       template: new sap.m.StandardListItem({
      //         title: "{title}",
      //         description: "Messages: {/messages/length} • {createdAt}",
      //         type: "Active",
      //         press: this.onHistoryItemPress.bind(this),
      //       }),
      //     });
      //   }
      // },

      // // Function to handle history item press
      // onHistoryItemPress: function (oEvent) {
      //   var oItem = oEvent.getSource();
      //   var oContext = oItem.getBindingContext();
      //   var oChatData = oContext.getObject();

      //   // Load the selected chat session
      //   this.loadChatSession(oChatData.id);
      // },

      // // Helper function to escape HTML (keeping your existing function)
      // escapeHtml: function (text) {
      //   const div = document.createElement("div");
      //   div.textContent = text;
      //   return div.innerHTML;
      // },

      // // Enhanced onCloseChatHistory function
      // onCloseChatHistory: function () {
      //   if (this._oHistoryDialog) {
      //     this._oHistoryDialog.close();
      //     this._oHistoryDialog.destroy();
      //     this._oHistoryDialog = null;
      //   }
      // },
      // ---------------------------------------Chat Bot -------------------------------------
    });
  }
);
