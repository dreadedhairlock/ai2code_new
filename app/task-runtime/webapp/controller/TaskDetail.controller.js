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

            this._loadTaskTree(this._sTaskId);

            // load context tree
            this._loadContextNodesTree();
            oModel.refresh();
          }.bind(this)
        );

        // Initialize view model for contextNodes tree data
        var oViewModel = new JSONModel({
          rootNodes: [],
          selectedNode: null,
        });
        this.getView().setModel(oViewModel, "contextNodes");

        // Initialize view model for task tree
        this._oTaskModel = new JSONModel({
          rootNodes: [],
        });
        this.getView().setModel(this._oTaskModel, "taskAndBotTree");

        // Create context menu programmatically if it doesn't exist
        if (!this._oContextMenu) {
          this._oContextMenu = new sap.m.Menu({
            items: [
              new sap.m.MenuItem({
                text: "Create file",
                icon: "sap-icon://create",
                press: this.onCreateFile.bind(this),
              }),
              new sap.m.MenuItem({
                text: "Create new folder",
                icon: "sap-icon://add-folder",
                press: this.onCreateFolder.bind(this),
              }),
            ],
          });

          // Add the menu to the view
          this.getView().addDependent(this._oContextMenu);
        }

        // Get reference to the tree
        var oTree = this.byId("contextTree");

        // Attach browser's contextmenu event to the tree
        oTree.attachBrowserEvent("contextmenu", this._onContextMenu.bind(this));
      },

      _loadTaskTree: async function (sTaskId) {
        console.log("Loading task tree for ID:", sTaskId);
        const that = this;
        const oModel = this.getOwnerComponent().getModel();
        const oTaskModel = this.getOwnerComponent().getModel("taskAndBotTree");

        if (!sTaskId) {
          return;
        }

        const sPath = "/Tasks('" + sTaskId + "')";

        // Create binding
        const oContext = oModel.bindContext(sPath, null, {
          $expand: "type,botInstances($expand=type,status,messages,tasks)",
        });

        const oTask = await oContext.requestObject();
        if (oTask) {
          console.log("oTask loaded: ", oTask);
          this._buildTaskAndBotStructure([{ getObject: () => oTask }]);
        }
      },

      _buildTaskAndBotStructure: function (aTaskContexts) {
        const nodes = [];
        aTaskContexts.forEach((oContext) => {
          const oTask = oContext.getObject();
          const oTaskItem = this._buildTaskItemRecursively(oTask);
          nodes.push(oTaskItem);
        });
        this._oTaskModel.setProperty("/rootNodes", nodes);

        console.log(this._oTaskModel.getData());
      },

      _buildTaskItemRecursively: function (oTask) {
        const oTaskItem = {
          ID: `task_${oTask.ID}`,
          name: oTask.name || "Unnamed Task",
          type: "Task",
          hasNodes: false,
          data: oTask,
          nodes: [],
        };

        const aBotInstances = oTask.botInstances;
        if (aBotInstances && aBotInstances.length > 0) {
          oTaskItem.hasNodes = true;
          aBotInstances.forEach((oBotInstance) => {
            const oBotItem = this._buildBotInstanceItem(oBotInstance);
            oTaskItem.nodes.push(oBotItem);
          });
        }
        return oTaskItem;
      },

      _buildBotInstanceItem: function (oBotInstance) {
        const oBotItem = {
          ID: `bot_${oBotInstance.ID}`,
          name:
            oBotInstance.type?.name ||
            `Bot Instance ${oBotInstance.sequence || ""}`,
          hasNodes: false,
          type: "BotInstance",
          data: oBotInstance,
          nodes: [],
        };
        const aSubTasks = oBotInstance.tasks;
        if (aSubTasks && aSubTasks.length > 0) {
          oBotItem.hasNodes = true;
          aSubTasks.forEach((oSubTask) => {
            const oSubTaskItem = this._buildTaskItemRecursively(oSubTask);
            oSubTaskItem.name = `${oSubTask.name || "Unnamed SubTask"}`;
            oBotItem.nodes.push(oSubTaskItem);
          });
        }

        // const aBotMessages = oBotInstance.messages;
        // if (aBotMessages && aBotMessages.length > 0) {
        // if (!oBotItem.hasNodes) {
        // oBotItem.hasNodes = true;
        // }
        // aBotMessages.forEach((oBotMessage, index) => {
        // const oBotMessageItem = {
        // ID: `botmessage_${oBotMessage.ID}`,
        // name: `Message ${index + 1} (${oBotMessage.role || "unknown"})`,
        // hasNodes: false,
        // type: "BotMessage",
        // data: oBotMessage,
        // nodes: [],
        // };
        // oBotItem.nodes.push(oBotMessageItem);
        // });
        // }

        return oBotItem;
      },

      _storeClickedItem: function (oEvent) {
        // Get the DOM element that was right-clicked
        var oElement = oEvent.target;

        // Find the tree item from the DOM element
        while (oElement && oElement.id) {
          var oControl = sap.ui.getCore().byId(oElement.id);
          // Check if we found a tree item
          if (oControl instanceof sap.m.StandardTreeItem) {
            this._clickedItem = oControl;
            console.log("Right-clicked on: " + oControl.getTitle());
            return;
          }
          oElement = oElement.parentNode;
        }

        // If we get here, we didn't click directly on a tree item
        this._clickedItem = null;
      },

      _onContextMenu: function (oEvent) {
        // Prevent default browser context menu
        oEvent.preventDefault();

        // Store the clicked item
        this._storeClickedItem(oEvent);

        // Simple approach: use the DOM event target directly
        try {
          console.log("Opening menu using DOM element");
          this._oContextMenu.openBy(oEvent.target);
        } catch (oError) {
          // If openBy with target fails, try with current tree item
          console.log("Fallback: opening menu using tree item", oError);
          if (this._clickedItem) {
            this._oContextMenu.openBy(this._clickedItem);
          } else {
            // Last resort: try placing menu near the cursor
            console.log("Last resort: placing menu using viewport coordinates");

            // Create a temporary span element at cursor position
            var oTempSpan = document.createElement("span");
            oTempSpan.style.position = "fixed";
            oTempSpan.style.left = (oEvent.clientX || 0) + "px";
            oTempSpan.style.top = (oEvent.clientY || 0) + "px";
            oTempSpan.style.zIndex = -1;
            oTempSpan.className = "sapUiHidden";
            document.body.appendChild(oTempSpan);

            // Open menu by the temporary element and remove it afterward
            this._oContextMenu.openBy(oTempSpan);
            setTimeout(function () {
              if (document.body.contains(oTempSpan)) {
                document.body.removeChild(oTempSpan);
              }
            }, 100);
          }
        }
      },

      onCreateFile: function () {
        this.onCreateCNData();
      },

      onCreateFolder: function () {
        // Get the clicked item
        var oItem = this._clickedItem;

        if (oItem) {
          var sItemText = oItem.getTitle();
          console.log("Create folder action for: " + sItemText);

          // Here you would implement the actual folder creation
          // For now, just show a message
          MessageToast.show("Creating folder under " + sItemText);
        } else {
          console.log("Create folder action (no specific item)");
          MessageToast.show("Creating folder at root level");
        }
      },

      _loadContextNodesTree: function () {
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
          .invoke()
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

      _convertFlatToHierarchical: function (flatNodes) {
        // Handle empty input
        if (!flatNodes || flatNodes.length === 0) {
          return [];
        }

        // Maps and arrays for efficient lookups and storage
        const nodesByPath = {}; // Stores folder nodes indexed by path for O(1) lookups
        const rootNodes = []; // Stores the top-level nodes (nodes without parents)

        // Step 1: Pre-process the input array to separate folders and data nodes
        // This avoids multiple filter() iterations for better performance
        const folderNodesMap = {}; // Map folders by path for faster access
        const dataNodesArray = []; // Store data nodes separately

        flatNodes.forEach((node) => {
          if (node.isFolder) {
            // Store folder nodes in a map with path as key
            folderNodesMap[node.path] = node;
          } else {
            // Collect data nodes in a separate array
            dataNodesArray.push(node);
          }
        });

        // Step 2: Sort folder paths by depth (shallowest first)
        // This ensures parent folders are processed before their children
        const folderPaths = Object.keys(folderNodesMap);
        folderPaths.sort((a, b) => {
          // Count dots to determine path depth (faster than split for large strings)
          return (a.match(/\./g) || []).length - (b.match(/\./g) || []).length;
        });

        // Step 3: Process folders and build the folder hierarchy
        for (const path of folderPaths) {
          const folder = folderNodesMap[path];
          // Create a copy of the folder with an items array for children
          const folderCopy = { ...folder, items: [] };
          nodesByPath[path] = folderCopy;

          // Check if this is a root folder (no dots in path)
          if (path.indexOf(".") === -1) {
            // faster than includes() for simple checks
            rootNodes.push(folderCopy);
            continue; // Skip to next iteration
          }

          // Find parent by removing the last path segment
          const lastDotIndex = path.lastIndexOf(".");
          const parentPath = path.substring(0, lastDotIndex);

          // Add this folder to its parent's items array
          const parentFolder = nodesByPath[parentPath];
          if (parentFolder) {
            parentFolder.items.push(folderCopy);
          } else {
            // If parent not found, add to root nodes as fallback
            rootNodes.push(folderCopy);
          }
        }

        // Step 4: Process data nodes and place them in appropriate folders
        for (const dataNode of dataNodesArray) {
          // Create a copy of data node with empty items array for consistency
          const { items, ...dataCopy } = { ...dataNode, items: [] };

          // Case 1: Check if there's a folder with the exact same path
          // This handles cases where a data node shares the same path as a folder
          const parentFolder = nodesByPath[dataNode.path];

          if (parentFolder) {
            // Add the data node to the folder with the same path
            parentFolder.items.push(dataCopy);
            continue;
          }

          // Case 2: Find parent folder by looking at the path's parent segment
          if (dataNode.path.indexOf(".") !== -1) {
            const lastDotIndex = dataNode.path.lastIndexOf(".");
            const parentPath = dataNode.path.substring(0, lastDotIndex);
            const parentFolder = nodesByPath[parentPath];

            if (parentFolder) {
              // Add to the parent folder
              parentFolder.items.push(dataCopy);
              continue;
            }
          }

          // Case 3: If no parent folder found, add to root level
          rootNodes.push(dataCopy);
        }

        // Return the hierarchical structure
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
        console.log(this._selectedNodePath);
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

        this._loadContextNodesTree();
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

                    this._loadContextNodesTree();
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

        const oData = {
          path: oCtx.getProperty("path"),
          label: oCtx.getProperty("label"),
          type: oCtx.getProperty("type"),
          value: oCtx.getProperty("value"),
        };

        const oForm = this._createCNFormForEdit(oData);
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
          this._loadContextNodesTree();
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

      onTaskAndBotItemPress: function (oEvent) {},

      onCreateSubTask: function () {
        var oTree = this.byId("taskAndBotTree");
        var oSelectedItem = oTree.getSelectedItem();

        if (!oSelectedItem) {
          MessageToast.show("Select the bot instance first");
          return;
        }

        var oContext = oSelectedItem.getBindingContext("taskAndBotTree");
        var sNodeType = oContext.getProperty("type");
        console.log("Selected node type:", sNodeType);

        if (sNodeType !== "BotInstance") {
          MessageToast.show(
            "New tasks can only be created under a Bot Instance"
          );
          return;
        }

        this._selectedBotInstanceId = oContext
          .getProperty("ID")
          .replace("bot_", "");

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
                this._loadTaskTree(this._sTaskId);
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

      onDeleteSubTask: function () {
        const oTree = this.byId("taskAndBotTree");
        const oSelected = oTree.getSelectedItem();

        if (!oSelected) {
          MessageToast.show("Please select task to delete!");
          return;
        }

        const oJsonCtx = oSelected.getBindingContext("taskAndBotTree");
        const sID = oJsonCtx.getProperty("ID").replace("task_", "");
        const sType = oJsonCtx.getProperty("type");

        if (!sID || sType !== "Task") {
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
                  this._loadTaskTree(this._sTaskId);
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

        var oContext = oSelectedItem.getBindingContext("taskAndBotTree");
        var sNodeType = oContext.getProperty("type");

        if (sNodeType !== "Task") {
          MessageToast.show("Bot instance only created in the node Task");
          return;
        }

        var sTaskId = oContext.getProperty("ID").replace("task_", "");
        var sTaskName = oContext.getProperty("name");

        if (this.oBotDialog) {
          this.oBotDialog.close();
          this.oBotDialog.destroy();
          this.oBotDialog = null;
        }

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
            new Input("botTaskName", {
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
              this._loadTaskTree(this._sTaskId);
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

        const oJsonCtx = oSelected.getBindingContext("taskAndBotTree");
        const sID = oJsonCtx.getProperty("ID").replace("bot_", "");
        const sType = oJsonCtx.getProperty("type");

        if (!sID || sType !== "BotInstance") {
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
                    this._loadTaskTree(this._sTaskId);
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

      // ---------------------------------------Chat Bot -------------------------------------

      onSubmitQuery: async function () {
        var that = this;
        var oInput = this.byId("chatInput");
        var sMessage = oInput.getValue().trim();
        var sMessageDate = new Date().toISOString();
        const oBotTree = this.byId("taskAndBotTree");
        const oSelected = oBotTree.getSelectedItem();

        const oJsonCtx = oSelected.getBindingContext("taskAndBotTree");
        const sBotInstanceId = oJsonCtx.getProperty("ID").replace("bot_", "");

        if (!sBotInstanceId) {
          MessageToast.show("Select bot instance first!");
          return;
        }

        if (sMessage) {
          try {
            this.addChatMessage(sMessage, "user", null, sMessageDate);

            oInput.setValue("");

            const oModel = this.getOwnerComponent().getModel();

            const sPath = `/BotInstances('${sBotInstanceId}')/MainService.chatCompletion(...)`;

            const oBinding = oModel.bindContext(sPath);

            oBinding.setParameter("content", sMessage);

            oBinding
              .invoke()
              .then((oResult) => {
                that._reply = oBinding.getBoundContext().getProperty("message");
                that._messageId = oBinding.getBoundContext().getProperty("ID");
                that._messageRole = oBinding
                  .getBoundContext()
                  .getProperty("role");
                that._messageCreationDate = oBinding
                  .getBoundContext()
                  .getProperty("createdAt");

                that.addChatMessage(
                  that._reply,
                  that._messageRole,
                  that._messageId,
                  that._messageCreationDate
                );

                setTimeout(function () {
                  that._scrollToBottom();
                }, 100);
              })
              .catch((oError) => {
                MessageToast.show("Error: " + oError.message);
              });
          } catch (error) {
            console.error("Error in chat completion:", error);
          }
        }
      },

      addChatMessage: function (sMessage, sType, sMessageId, sMessageTime) {
        var oChatBox = this.byId("chatMessagesBox");

        var timestampForId = new Date().getTime();

        var messageContent =
          typeof sMessage === "string" ? sMessage : JSON.stringify(sMessage);

        // Generate unique ID for the button
        var buttonId = "adoptBtn_" + sMessageId + timestampForId;

        var oMessageContainer;

        var formattedTime = this._formatTimestamp(sMessageTime);

        if (sType === "assistant" && sMessageId) {
          var oContentBox = new sap.m.VBox({
            width: "100%",
            items: [
              new sap.m.Text({
                text: messageContent,
              }),

              new sap.m.HBox({
                height: "12px",
              }),
            ],
          });

          var oFooterBox = new sap.m.HBox({
            justifyContent: "SpaceBetween",
            alignItems: "Center",
            width: "100%",
            items: [
              new sap.m.Text({
                text: formattedTime,
              }).addStyleClass("chatTimestamp"),
              new sap.m.Button({
                id: buttonId,
                icon: "sap-icon://thumb-up",
                text: "Adopt",
                type: "Emphasized",
                press: function (oEvent) {
                  this._handleAdoptClick(oEvent, sMessageId);
                }.bind(this),
              }).addStyleClass("adoptButton"),
            ],
          });

          var oBubble = new sap.m.VBox({
            items: [oContentBox, oFooterBox],
          });
          oBubble.addStyleClass("chatBubble assistant");

          oMessageContainer = new sap.m.VBox({
            alignItems: "Start",
            items: [oBubble],
          });
          oMessageContainer.addStyleClass("chatBubbleContainer assistant");
        } else if (sType === "user") {
          var oBubble = new sap.m.VBox({
            items: [
              new sap.m.Text({
                text: messageContent,
              }).addStyleClass("userText"),
              new sap.m.Text({
                text: formattedTime,
              }).addStyleClass("chatTimestamp"),
            ],
          });
          oBubble.addStyleClass("chatBubble user");

          oMessageContainer = new sap.m.VBox({
            alignItems: "End",
            items: [oBubble],
          });
          oMessageContainer.addStyleClass("chatBubbleContainer user");
        }

        oChatBox.addItem(oMessageContainer);

        var that = this;
        setTimeout(function () {
          that._scrollToBottom();
        }, 100);
      },

      // Centralized handler for adopt clicks with debounce mechanism
      _handleAdoptClick: function (oEvent, messageId) {
        var oButton = oEvent.getSource(); // UI5 Button

        if (!messageId) return;

        // Prevent multiple clicks using custom property
        if (oButton._isProcessing) return;
        oButton._isProcessing = true;

        var originalText = oButton.getText();
        oButton.setText("Adopting...");
        oButton.setEnabled(false); // Disable the button during processing

        // Call adopt function
        this.onAdoptMessage(messageId)
          .then(function () {
            oButton.setText("Adopted!");

            setTimeout(function () {
              oButton.setText(originalText);
              oButton.setEnabled(true);
              oButton._isProcessing = false;
            }, 100);
          })
          .catch(function (error) {
            oButton.setText("Failed!");
            console.error("Error adopting message:", error);
          });
      },

      onAdoptMessage: function (sMessageId) {
        // Return a promise that can be awaited by the caller
        return new Promise((resolve, reject) => {
          var oModel = this.getOwnerComponent().getModel();

          // Binding context for action
          var sPath =
            "/BotMessages('" + sMessageId + "')/MainService.adopt(...)";
          var oOperation = oModel.bindContext(sPath);

          // execute action
          oOperation
            .invoke()
            .then((oResult) => {
              MessageToast.show("Message adopted successfully");
              // Load context tree
              this._loadContextNodesTree();
              resolve(oResult);
            })
            .catch((oError) => {
              MessageToast.show("Error: " + oError.message);
              reject(oError);
            });
        });
      },

      _loadChatHistory: function () {
        var that = this;
        var botInstanceId = this._selectedBotInstanceId;

        // Clear existing messages
        this.byId("chatMessagesBox").removeAllItems();

        // Get the OData V4 model from the view
        var oModel = this.getOwnerComponent().getModel();

        // Create a list binding for BotMessages
        var oListBinding = oModel.bindList(
          "/BotMessages",
          null,
          null,
          [
            new sap.ui.model.Filter(
              "botInstance_ID",
              sap.ui.model.FilterOperator.EQ,
              botInstanceId
            ),
          ],
          {
            $orderby: "createdAt asc",
          }
        );

        // Request data from the server
        oListBinding
          .requestContexts()
          .then(function (aContexts) {
            // Process the returned contexts
            aContexts.forEach(function (oContext) {
              // Get the message data for this context
              var oMessage = oContext.getObject();
              that.addChatMessage(
                oMessage.message,
                oMessage.role,
                oMessage.ID,
                oMessage.createdAt
              );
            });
            setTimeout(function () {
              that._scrollToBottom();
            }, 100);
          })

          .catch(function (oError) {
            console.error("Error loading chat history:", oError);
            that.addChatMessage(
              "Error loading chat history: " + (oError.message || oError),
              "error"
            );
          });
      },

      _formatTimestamp: function (sTimestamp) {
        if (!sTimestamp) return "";

        var date = new Date(sTimestamp);
        var day = String(date.getDate()).padStart(2, "0");
        var month = String(date.getMonth() + 1).padStart(2, "0");
        var year = date.getFullYear();
        var hours = String(date.getHours()).padStart(2, "0");
        var minutes = String(date.getMinutes()).padStart(2, "0");
        var seconds = String(date.getSeconds()).padStart(2, "0");

        return (
          day +
          "-" +
          month +
          "-" +
          year +
          " " +
          hours +
          ":" +
          minutes +
          ":" +
          seconds
        );
      },

      _scrollToBottom: function () {
        try {
          var oChatBox = this.byId("chatMessagesBox");
          if (oChatBox && oChatBox.getItems().length > 0) {
            var oLastItem = oChatBox.getItems()[oChatBox.getItems().length - 1];
            if (oLastItem) {
              oLastItem.getDomRef()?.scrollIntoView({
                behavior: "smooth",
                block: "end",
              });
            }
          }
        } catch (error) {
          console.warn("Scroll to bottom failed:", error);
        }
      },

      /**
       * Handles lazy loading of additional tree levels when a node is expanded.
       * Only loads children if not already loaded, and ensures robust error handling.
       * @param {sap.ui.base.Event} oEvent - The toggleOpenState event from sap.m.Tree
       */
      onToggleOpenState: function (oEvent) {
        try {
          var bExpanded = oEvent.getParameter("expanded");
          var oCtx = oEvent.getParameter("itemContext");
          if (!bExpanded || !oCtx) return;
          var oNode = oCtx.getObject();
          if (!oNode || oNode._childrenLoaded) return;

          this._loadMoreTaskAndBotLevels(oNode)
            .then(function (aNewChildren) {
              oNode.nodes = aNewChildren;
              oNode._childrenLoaded = true;
              oCtx.getModel().refresh(true);
            })
            .catch(function (err) {
              console.error("Error loading more tree levels:", err);
            });
        } catch (err) {
          console.error("onToggleOpenState error:", err);
        }
      },

      /**
       * Loads the next 3 levels of children for a Task or BotInstance node using OData V4 $expand.
       * Returns an array of child nodes, each marked for further lazy loading.
       * @param {object} oNode - The node object (Task or BotInstance)
       * @returns {Promise<Array>} - Promise resolving to array of child nodes
       */
      _loadMoreTaskAndBotLevels: async function (oNode) {
        const oModel = this.getOwnerComponent().getModel();
        let aChildren = [];
        try {
          if (oNode.type === "Task") {
            // Fetch botInstances for this Task, expand 3 levels
            const sTaskId = oNode.ID.replace("task_", "");
            const sPath = `/Tasks('${sTaskId}')`;
            const oContext = oModel.bindContext(sPath, null, {
              $expand: "botInstances($expand=tasks,type)",
            });
            const oTask = await oContext.requestObject();
            if (oTask && Array.isArray(oTask.botInstances)) {
              aChildren = oTask.botInstances.map(
                this._buildBotInstanceItem,
                this
              );
            }
          } else if (oNode.type === "BotInstance") {
            // Fetch tasks for this BotInstance, expand 3 levels
            const sBotId = oNode.ID.replace("bot_", "");
            const sPath = `/BotInstances('${sBotId}')`;
            const oContext = oModel.bindContext(sPath, null, {
              $expand: "tasks($expand=botInstances)",
            });
            const oBot = await oContext.requestObject();
            if (oBot && Array.isArray(oBot.tasks)) {
              aChildren = oBot.tasks.map(this._buildTaskItemRecursively, this);
            }
          }
          // Mark children as not loaded for further lazy loading
          aChildren.forEach((child) => (child._childrenLoaded = false));
        } catch (err) {
          console.error("_loadMoreTaskAndBotLevels error:", err);
        }
        return aChildren;
      },

      // ---------------------------------------Chat Bot -------------------------------------
    });
  }
);
