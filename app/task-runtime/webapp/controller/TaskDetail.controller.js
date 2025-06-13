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
    Fragment
  ) {
    "use strict";

    return Controller.extend("task-runtime.controller.TaskDetail", {
      // Initialize router & chat history
      onInit: function () {
        const oRouter = this.getOwnerComponent().getRouter();
        const oModel = this.getOwnerComponent().getModel();

        oRouter.attachRouteMatched(
          function (oEvent) {
            this._sTaskId = oEvent.getParameter("arguments").taskId;
            oModel.refresh();
          }.bind(this)
        );

        this._chatHistory = [];
        this._currentMessages = [];
        this._currentChatId = null;
        this._loadHistoryFromStorage();
        this.startNewChat();
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
        const oForm = this._createCNFormForCreate();
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
              this._selectedNodePath = null;
              this._selectedNodeIsFolder = false;
            },
          }),
          afterClose: () => {
            this.oCreateDialog.destroy();
            this.oCreateDialog = null;
            this._selectedNodePath = null;
            this._selectedNodeIsFolder = false;
          },
        });
        this.getView().addDependent(this.oCreateDialog);
        this.oCreateDialog.open();
      },

      // Build form for creating a Context Node
      _createCNFormForCreate: function () {
        // Pre-fill with path from selected node
        let initialPath = "";

        if (this._selectedNodePath) {
          // For SAP.m.Tree, use the exact path from the selected node
          initialPath = this._selectedNodePath;
        }

        // Path information text to help the user
        let pathInfo = "Current path: " + (initialPath || "root");
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
              value: initialPath,
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

        this._selectedNodePath = null;
        this._selectedNodeIsFolder = false;

        await this.getOwnerComponent()._loadContextNodes(this._sTaskId);
        this.oCreateDialog.close();
      },

      // Edit an existing Context Node
      onEditCNData: function () {
        const oTree = this.byId("docTree");
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
            new Input("CNPathEdit", { value: oData.path }),
            new Label({ text: "Label" }),
            new Input("CNLabelEdit", { value: oData.label }),
            new Label({ text: "Type" }),
            new Input("CNTypeEdit", { value: oData.type }),
            new Label({ text: "Value" }),
            new Input("CNValueEdit", { value: oData.value }),
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
          await this.getOwnerComponent()._loadContextNodes(this._sTaskId);
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

        var oTree = this.byId("tree");
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

        // Jika yang dipilih adalah TASK -> load BotInstances
        if (sType === "task") {
          oModel
            .bindList("/Tasks('" + sID + "')/botInstances")
            .requestContexts()
            .then(
              function (aContexts) {
                var aData = aContexts.map(function (oContext) {
                  var oObj = oContext.getObject();
                  oObj.type = "bot"; // Set type sebagai bot
                  oObj.nodes = []; // Initialize nodes untuk children
                  return oObj;
                });

                // Add children ke selected task node
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
        // Jika yang dipilih adalah BOT -> load sub-Tasks
        else if (sType === "bot") {
          oModel
            .bindList("/BotInstances('" + sID + "')/tasks")
            .requestContexts()
            .then(
              function (aContexts) {
                var aData = aContexts.map(function (oContext) {
                  var oObj = oContext.getObject();
                  oObj.type = "task"; // Set type sebagai task
                  oObj.nodes = []; // Initialize nodes untuk children
                  return oObj;
                });

                // Add children ke selected bot node
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

      /**
       * Handler untuk membuat Sub Task di bawah Bot Instance yang dipilih
       */
      onCreateSubTask: function () {
        // Dapatkan node yang dipilih (harus Bot Instance)
        var oTree = this.byId("tree");
        var oSelectedItem = oTree.getSelectedItem();

        // Validasi jika item dipilih dan tipenya adalah bot
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

        // Simpan ID Bot Instance yang dipilih
        this._selectedBotInstanceId = oContext.getProperty("ID");

        // Buka dialog create task
        this._openCreateTaskDialog();
      },

      /**
       * Membuka dialog untuk pembuatan Task
       */
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

      /**
       * Membuat dialog untuk memilih Task Type
       */
      _createSelectTaskTypeDialog: function () {
        return this.oSelectTypeDialog
          ? this.oSelectTypeDialog
          : new SelectDialog({
              noDataText: "No task types found",
              title: "Select Task Type",
              items: {
                path: "/TaskType", // Sesuaikan dengan entity set OData Anda
                template: new StandardListItem({
                  title: "{name}",
                  description: "{description}",
                  info: "{ID}", // OData V4 tidak menggunakan highlightText
                }),
              },
              confirm: function (oEvent) {
                const oSelectedItem = oEvent.getParameter("selectedItem");
                if (oSelectedItem) {
                  Element.getElementById("taskTypeIdDetail").setValue(
                    oSelectedItem.getInfo()
                  );
                }
              }.bind(this),
            });
      },

      /**
       * Membuat form untuk input Task
       */
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

      /**
       * Membuat Sub Task di bawah Bot Instance
       * Dimodifikasi untuk menggunakan OData V4 dan binding ke Bot Instance yang dipilih
       */
      _createSubTask: function () {
        // Validasi Bot Instance yang dipilih
        if (!this._selectedBotInstanceId) {
          MessageToast.show("Bot Instance not found");
          return;
        }

        // Ambil nilai dari form
        const sTaskName = Element.getElementById("taskNameDetail").getValue();
        const sTaskDescription = Element.getElementById(
          "taskDescriptionDetail"
        ).getValue();
        const sTaskTypeId =
          Element.getElementById("taskTypeIdDetail").getValue();

        // Set busy state
        this.getView().setBusy(true);

        try {
          // Siapkan payload untuk Task
          var oPayload = {
            name: sTaskName,
            description: sTaskDescription,
            isMain: false, // Sub task, bukan main task
            sequence: 0, // Default sequence
            contextPath: "", // Default empty path
            botInstance_ID: this._selectedBotInstanceId, // Set parent Bot Instance
          };

          // Add type_ID jika ada
          if (sTaskTypeId && sTaskTypeId !== "") {
            oPayload.type_ID = sTaskTypeId;
          }

          // Gunakan OData V4 untuk create Task
          var oContext = this.getOwnerComponent()
            .getModel()
            .bindList("/Tasks")
            .create(oPayload);

          oContext
            .created()
            .then(
              function () {
                MessageToast.show("Sub Task successfully created");

                // Reset selected BotInstance ID
                var sBotId = this._selectedBotInstanceId;
                this._selectedBotInstanceId = null;

                // Refresh node BotInstance untuk menampilkan Task baru
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

      /**
       * Refresh node Bot Instance yang dipilih
       * @param {string} sBotId - ID Bot Instance yang perlu di-refresh
       */
      _refreshSelectedBotNode: function (sBotId) {
        // Dapatkan Tree UI control
        var oTree = this.byId("tree");
        if (!oTree) {
          MessageToast.show("Tree control not found");
          return;
        }

        // Dapatkan model tree
        var oTreeModel = this.getOwnerComponent().getModel("taskTree");

        // Set busy indicator
        this.getView().setBusy(true);

        try {
          // Dapatkan OData model
          var oModel = this.getOwnerComponent().getModel();

          // Fetch tasks yang baru untuk Bot Instance ini
          oModel
            .bindList("/BotInstances('" + sBotId + "')/tasks")
            .requestContexts()
            .then(
              function (aContexts) {
                // Ubah hasil menjadi nodes untuk tree
                var aTasks = aContexts.map(function (oContext) {
                  var oTask = oContext.getObject();
                  oTask.type = "task"; // Set type sebagai task
                  oTask.nodes = []; // Initialize nodes untuk children
                  return oTask;
                });

                // Cari node Bot Instance dalam tree model
                this._updateBotNodeChildren(
                  oTreeModel.getData(),
                  sBotId,
                  aTasks
                );

                // Refresh model tree untuk update UI
                oTreeModel.refresh(true);

                // Expand node Bot Instance untuk menampilkan tasks baru
                this._expandBotNode(sBotId);

                // Reset busy indicator
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

      /**
       * Fungsi rekursif untuk mencari node Bot Instance dan update children-nya
       * @param {Array|Object} oNodes - Nodes untuk dicari
       * @param {string} sBotId - ID bot instance yang dicari
       * @param {Array} aNewChildren - Children baru untuk ditambahkan
       * @returns {boolean} True jika node ditemukan dan diupdate
       */
      _updateBotNodeChildren: function (oNodes, sBotId, aNewChildren) {
        // Handle jika parameter adalah array (multiple nodes) atau objek (single node)
        var aNodes = Array.isArray(oNodes) ? oNodes : [oNodes];

        // Iterasi semua node untuk menemukan bot instance yang sesuai
        for (var i = 0; i < aNodes.length; i++) {
          var oNode = aNodes[i];

          // Cek apakah ini node yang dicari
          if (oNode.type === "bot" && oNode.ID === sBotId) {
            // Node ditemukan, update children
            oNode.nodes = aNewChildren;
            return true;
          }

          // Jika node ini memiliki children, cek secara rekursif
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

      /**
       * Expand node Bot Instance dalam tree UI
       * @param {string} sBotId - ID bot instance yang akan di-expand
       */
      _expandBotNode: function (sBotId) {
        var oTree = this.byId("tree");
        var aItems = oTree.getItems();

        // Iterasi items di tree untuk menemukan node Bot Instance
        for (var i = 0; i < aItems.length; i++) {
          var oItem = aItems[i];
          var oContext = oItem.getBindingContext("taskTree");

          if (
            oContext &&
            oContext.getProperty("type") === "bot" &&
            oContext.getProperty("ID") === sBotId
          ) {
            // Node ditemukan, expand
            oTree.expand(oTree.indexOfItem(oItem));
            break;
          }
        }
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
        const oTree = this.byId("tree");
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

        // Tampilkan konfirmasi sebelum delete
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

                // Pastikan context valid
                await oContext.requestObject();
                const oBoundContext = oContext.getBoundContext();

                if (oBoundContext) {
                  await oBoundContext.delete();
                  MessageToast.show("Task deleted successfully.");
                  // Reload ulang tree/list
                  await this.getOwnerComponent()._loadMainTasks(this._sTaskId);
                } else {
                  MessageToast.show("Could not find task with ID: " + sID);
                }
              } catch (oError) {
                console.error("Delete error:", oError);
                MessageToast.show("Error: " + (oError.message || oError));
              }
            }
            // Jika Cancel, tidak ada yang terjadi
          },
        });
      },

      // ---------------------------------------Bot Instance -------------------------------------

      /* =========================================================== */
      /* Create Bot Instance - Main Function                         */
      /* =========================================================== */

      /**
       * Handler untuk tombol Create Bot Instance
       */
      onCreateBotInstance: function () {
        // Dapatkan node Task yang saat ini dipilih
        var oTree = this.byId("tree");
        var oSelectedItem = oTree.getSelectedItem();

        // Validasi jika item dipilih dan tipenya adalah task
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
        var sTaskName = oContext.getProperty("name") || "Task";

        // Tampilkan dialog untuk membuat BotInstance baru
        this._openSimpleCreateBotInstanceDialog(sTaskId, sTaskName);
      },

      /* =========================================================== */
      /* Create Bot Instance - Helper Functions                      */
      /* =========================================================== */

      /**
       * Membuka dialog sederhana untuk pembuatan BotInstance
       * @param {string} sTaskId - ID task yang menjadi parent
       * @param {string} sTaskName - Nama task untuk ditampilkan di dialog
       */
      _openSimpleCreateBotInstanceDialog: function (sTaskId, sTaskName) {
        // Siapkan model untuk dialog dengan hardcoded bot types
        var oDialogModel = new JSONModel({
          taskId: sTaskId,
          taskName: sTaskName,
          botInstance: {
            sequence: 1, // Default value
            type_code: "GPT", // Default value, dapat diubah user
            status_code: "C", // Default status: Created
          },
          // Hardcoded bot types untuk dropdown
          botTypes: [
            { code: "GPT", name: "GPT Bot" },
            { code: "SEARCH", name: "Search Bot" },
            { code: "ANALYSIS", name: "Analysis Bot" },
            { code: "SUMMARIZE", name: "Summary Bot" },
          ],
        });

        this.getView().setModel(oDialogModel, "dialog");

        // Cek apakah dialog sudah ada
        if (!this._oBotInstanceDialog) {
          // Buat dialog via fragment
          Fragment.load({
            id: this.getView().getId(),
            name: "task-runtime.view.CreateBotInstanceDialog", // Adjust fragment name if needed
            controller: this,
          }).then(
            function (oDialog) {
              this._oBotInstanceDialog = oDialog;
              this.getView().addDependent(this._oBotInstanceDialog);
              this._oBotInstanceDialog.open();
            }.bind(this)
          );
        } else {
          this._oBotInstanceDialog.open();
        }
      },

      /**
       * Handler untuk tombol Cancel di dialog
       */
      onCancelBotInstance: function () {
        if (this._oBotInstanceDialog) {
          this._oBotInstanceDialog.close();
        }
      },

      /**
       * Handler untuk tombol Save di dialog
       * Menyimpan BotInstance baru ke backend menggunakan OData V4
       */
      onSaveBotInstance: function () {
        // Dapatkan data dari model dialog
        var oDialogModel = this.getView().getModel("dialog");
        // var sBotTypeCode = oDialogModel.getProperty("/botInstance/type_code");
        var iSequence = oDialogModel.getProperty("/botInstance/sequence");
        var sTaskId = oDialogModel.getProperty("/taskId");

        // Validasi input
        // if (!sBotTypeCode) {
        //   MessageToast.show("Silahkan pilih Bot Type");
        //   return;
        // }

        // Siapkan payload untuk create - gunakan type_code tanpa perlu type_ID
        var oPayload = {
          sequence: iSequence,
          status_code: "C", // Default status: Created
          task_ID: sTaskId,
        };

        // Set busy indicator
        // this.getView().setBusy(true);

        // Kirim request create ke backend dengan OData V4
        try {
          var oList = this.getOwnerComponent()
            .getModel()
            .bindList("/BotInstances");
          var oContext = oList.create(oPayload);

          // Handle hasil operasi create
          oContext
            .created()
            .then(
              function () {
                // Notifikasi sukses
                MessageToast.show("Bot Instance successfully created!");

                // Tutup dialog
                if (this._oBotInstanceDialog) {
                  this._oBotInstanceDialog.close();
                }

                // this.getView().setBusy(false);

                // Optional: refresh parent node untuk tampilkan data baru
                // this._refreshTaskNode(sTaskId);
                this._refreshSelectedTaskNode(sTaskId);
              }.bind(this)
            )
            .catch(
              function (oError) {
                // Error handling
                MessageBox.error(
                  "Error creating Bot Instance: " + oError.message
                );
                this.getView().setBusy(false);
              }.bind(this)
            );
        } catch (oError) {
          MessageBox.error("Error: " + oError.message);
          this.getView().setBusy(false);
        }
      },

      onDeleteBotInstance: function () {
        const oTree = this.byId("tree");
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

        // Tampilkan konfirmasi sebelum delete
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

                  // Pastikan context valid
                  await oContext.requestObject();
                  const oBoundContext = oContext.getBoundContext();

                  if (oBoundContext) {
                    await oBoundContext.delete();
                    MessageToast.show("Bot Instances deleted successfully.");
                    // Reload ulang tree/list
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
              // Jika Cancel, tidak ada yang terjadi
            },
          }
        );
      },

      /**
       * Refresh node Task yang dipilih
       * @param {string} sTaskId - ID task yang perlu di-refresh
       */
      _refreshSelectedTaskNode: function (sTaskId) {
        // Dapatkan Tree UI control
        var oTree = this.byId("tree");
        if (!oTree) {
          MessageToast.show("Tree control not found");
          return;
        }

        // Dapatkan model tree
        var oTreeModel = this.getOwnerComponent().getModel("taskTree");

        // Set busy indicator
        this.getView().setBusy(true);

        try {
          // Dapatkan OData model
          var oModel = this.getOwnerComponent().getModel();

          // Fetch botInstances yang baru untuk task ini
          oModel
            .bindList("/Tasks('" + sTaskId + "')/botInstances")
            .requestContexts()
            .then(
              function (aContexts) {
                // Ubah hasil menjadi nodes untuk tree
                var aBotInstances = aContexts.map(function (oContext) {
                  var oBot = oContext.getObject();
                  oBot.type = "bot"; // Set type sebagai bot
                  oBot.nodes = []; // Initialize nodes untuk children
                  return oBot;
                });

                // Cari node Task dalam tree model
                this._updateTaskNodeChildren(
                  oTreeModel.getData(),
                  sTaskId,
                  aBotInstances
                );

                // Refresh model tree untuk update UI
                oTreeModel.refresh(true);

                // Expand node Task untuk menampilkan botInstances baru
                this._expandTaskNode(sTaskId);

                // Reset busy indicator
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

      /**
       * Fungsi rekursif untuk mencari node Task dan update children-nya
       * @param {Array|Object} oNodes - Nodes untuk dicari
       * @param {string} sTaskId - ID task yang dicari
       * @param {Array} aNewChildren - Children baru untuk ditambahkan
       * @returns {boolean} True jika node ditemukan dan diupdate
       */
      _updateTaskNodeChildren: function (oNodes, sTaskId, aNewChildren) {
        // Handle jika parameter adalah array (multiple nodes) atau objek (single node)
        var aNodes = Array.isArray(oNodes) ? oNodes : [oNodes];

        // Iterasi semua node untuk menemukan task yang sesuai
        for (var i = 0; i < aNodes.length; i++) {
          var oNode = aNodes[i];

          // Cek apakah ini node yang dicari
          if (oNode.type === "task" && oNode.ID === sTaskId) {
            // Node ditemukan, update children
            oNode.nodes = aNewChildren;
            return true;
          }

          // Jika node ini memiliki children, cek secara rekursif
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

      /**
       * Expand node Task dalam tree UI
       * @param {string} sTaskId - ID task yang akan di-expand
       */
      _expandTaskNode: function (sTaskId) {
        var oTree = this.byId("tree");
        var aItems = oTree.getItems();

        // Iterasi items di tree untuk menemukan node Task
        for (var i = 0; i < aItems.length; i++) {
          var oItem = aItems[i];
          var oContext = oItem.getBindingContext("taskTree");

          if (
            oContext &&
            oContext.getProperty("type") === "task" &&
            oContext.getProperty("ID") === sTaskId
          ) {
            // Node ditemukan, expand
            oTree.expand(oTree.indexOfItem(oItem));
            break;
          }
        }
      },
      // ---------------------------------------Chat Bot -------------------------------------
      conversationHistory: [],

      parseAIResponse: function (response) {
        const parts = [];
        const codeBlockRegex = /```(\w+)?\n?([\s\S]*?)```|`([^`]+)`/g;

        let lastIndex = 0;
        let match;
        let partIndex = 0;

        while ((match = codeBlockRegex.exec(response)) !== null) {
          // Add text before the code block
          if (match.index > lastIndex) {
            const textBefore = response
              .substring(lastIndex, match.index)
              .trim();
            if (textBefore) {
              parts.push({
                type: "text",
                content: textBefore,
                index: partIndex++,
              });
            }
          }

          // Add the code block
          if (match[0].startsWith("```")) {
            // Multi-line code block
            parts.push({
              type: "code_block",
              language: match[1] || "text",
              content: match[2] ? match[2].trim() : "",
              raw: match[0],
              index: partIndex++,
            });
          } else {
            // Inline code
            parts.push({
              type: "inline_code",
              content: match[3] || "",
              raw: match[0],
              index: partIndex++,
            });
          }

          lastIndex = match.index + match[0].length;
        }

        // Add remaining text
        if (lastIndex < response.length) {
          const remainingText = response.substring(lastIndex).trim();
          if (remainingText) {
            parts.push({
              type: "text",
              content: remainingText,
              index: partIndex++,
            });
          }
        }

        // If no matches found, return the entire response as text
        if (parts.length === 0) {
          parts.push({
            type: "text",
            content: response.trim(),
            index: 0,
          });
        }

        return parts;
      },

      // Helper function to convert parsed response back to plain text for history
      parsedResponseToText: function (parsedResponse) {
        if (typeof parsedResponse === "string") {
          return parsedResponse;
        }

        if (Array.isArray(parsedResponse)) {
          return parsedResponse
            .map((part) => {
              if (part.type === "text") {
                return part.content;
              } else if (part.type === "code_block") {
                return `\`\`\`${part.language || ""}\n${part.content}\n\`\`\``;
              } else if (part.type === "inline_code") {
                return `\`${part.content}\``;
              }
              return part.content || "";
            })
            .join("");
        }

        return String(parsedResponse);
      },

      // Function to manage conversation history
      addToHistory: function (role, content) {
        // Convert parsed content to plain text for API
        const textContent = this.parsedResponseToText(content);

        this.conversationHistory.push({
          role: role === "user" ? "user" : "model", // Gemini uses "model" instead of "assistant"
          parts: [
            {
              text: textContent,
            },
          ],
        });

        // Optional: Limit history to prevent token overflow (keep last 20 exchanges)
        const maxHistoryLength = 40; // 20 user + 20 AI messages
        if (this.conversationHistory.length > maxHistoryLength) {
          this.conversationHistory = this.conversationHistory.slice(
            -maxHistoryLength
          );
        }
      },

      // Function to clear conversation history
      clearHistory: function () {
        this.conversationHistory = [];
      },

      onSubmitQuery: async function () {
        var oInput = this.byId("chatInput");
        var sMessage = oInput.getValue().trim();
        if (sMessage) {
          // Add user message
          this.addChatMessage(sMessage, "user");
          // Add user message to conversation history
          this.addToHistory("user", sMessage);
          // Clear input
          oInput.setValue("");
          console.log(sMessage);
          // Simulate AI response (replace with your actual AI call)
          try {
            const API_KEY = "AIzaSyDyE_D4ej7SljvLAV5vWMmkQxg5OjGv5r4";
            const model = "gemini-2.0-flash";

            // Prepare the request with full conversation history
            const requestBody = {
              contents: this.conversationHistory, // Send entire conversation history
            };

            console.log(
              "Sending conversation history:",
              this.conversationHistory
            );

            const response = await fetch(
              `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${API_KEY}`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify(requestBody),
              }
            );
            console.log(response);
            const data = await response.json();
            console.log(data);
            const reply =
              data.candidates?.[0]?.content?.parts?.[0]?.text ||
              "No response from AI.";

            // Check if reply is valid before parsing
            if (reply && reply !== "No response from AI.") {
              // Parse the AI response to separate code blocks
              const parsedResponse = this.parseAIResponse(reply);
              console.log("Parsed response:", parsedResponse);

              // Add the parsed response to chat
              this.addChatMessage(parsedResponse, "ai");

              // Add AI response to conversation history
              this.addToHistory("ai", parsedResponse);
            } else {
              // Fallback to original method if no valid response
              this.addToHistory("ai", reply);
              this.addChatMessage(reply, "ai");
            }
          } catch (error) {
            this.addChatMessage("Error: " + error.message, "ai");
            this.addToHistory("ai", errorMessage);
            this.addChatMessage(errorMessage, "ai");
          }
        }
      },

      // Add these properties to your controller
      _chatHistory: [], // Array to store chat sessions
      _currentChatId: null, // Current active chat session ID
      _currentMessages: [], // Current chat messages

      // Modified addChatMessage function
      addChatMessage: function (sMessage, sType) {
        var oChatBox = this.byId("chatMessagesBox");
        var sTimestamp = new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        });

        // Create message object for storage
        var oMessageData = {
          id: this._generateMessageId(),
          message: sMessage,
          type: sType,
          timestamp: sTimestamp,
          fullTimestamp: new Date().toISOString(),
        };

        // Add to current messages array
        this._currentMessages.push(oMessageData);

        // Update current chat session in history
        this._updateCurrentChatInHistory();

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

        // Handle parsed AI responses
        if (sType === "ai" && Array.isArray(sMessage)) {
          // Process parsed response parts
          let textContent = "";
          let codeBlocks = [];

          sMessage.forEach((part) => {
            console.log("Processing part:", part);
            if (part.type === "text") {
              textContent += part.content + " ";
            } else if (part.type === "code_block") {
              codeBlocks.push(part);
            } else if (part.type === "inline_code") {
              textContent += `<code>${this.escapeHtml(part.content)}</code> `;
            }
          });

          console.log("Processed content:", { textContent, codeBlocks });

          // Update stored message with processed content
          oMessageData.processedText = textContent.trim();
          oMessageData.codeBlocks = codeBlocks;

          // Only create chat bubble if there's text content
          if (textContent.trim()) {
            var oHTML = new sap.ui.core.HTML({
              content: `
                    <div class="chatBubbleContainer ${sType}">
                        <div class="chatBubble ${sType}">
                            <div>${textContent.trim()}</div>
                            <div class="chatTimestamp">${sTimestamp}</div>
                        </div>
                    </div>
                `,
            });
            oChatBox.addItem(oHTML);
          }

          // Handle code blocks separately
          if (codeBlocks.length > 0) {
            var oCodeResultText = this.byId("codeResultText");
            if (oCodeResultText) {
              const codeSections = codeBlocks.map((block) => {
                const language = block.language || "code";
                const langLabel = `<div class="codeLangLabel">${language.toUpperCase()}</div>`;
                const codeBlock = `
                        <div class="codeBlockWrapper">
                            ${langLabel}
                            <pre><code class="language-${language}">${this.escapeHtml(
                  block.content
                )}</code></pre>
                        </div>
                    `;
                return codeBlock;
              });
              oCodeResultText.setContent(codeSections.join("<br/>"));
            }
          }

          // If no text content and no code blocks, show the original message
          if (!textContent.trim() && codeBlocks.length === 0) {
            console.log("No content found, showing original message");
            const originalMessage = sMessage
              .map((part) => part.content || part.raw || "")
              .join(" ");
            var oHTML = new sap.ui.core.HTML({
              content: `
                    <div class="chatBubbleContainer ${sType}">
                        <div class="chatBubble ${sType}">
                            <div>${
                              originalMessage || "AI response received"
                            }</div>
                            <div class="chatTimestamp">${sTimestamp}</div>
                        </div>
                    </div>
                `,
            });
            oChatBox.addItem(oHTML);
          }
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

          // For backward compatibility with non-parsed responses
          if (sType === "ai" && typeof sMessage === "string") {
            var oCodeResultText = this.byId("codeResultText");
            if (oCodeResultText) {
              oCodeResultText.setText(sMessage);
            }
          }
        }

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

      // Helper function to generate unique message IDs
      _generateMessageId: function () {
        return (
          "msg_" + Date.now() + "_" + Math.random().toString(36).substr(2, 9)
        );
      },

      // Helper function to generate unique chat session IDs
      _generateChatId: function () {
        return (
          "chat_" + Date.now() + "_" + Math.random().toString(36).substr(2, 9)
        );
      },

      // Function to start a new chat session
      startNewChat: function () {
        // Save current chat if it has messages
        if (this._currentMessages.length > 0) {
          this._saveChatSession();
        }

        // Clear current chat
        this._currentChatId = this._generateChatId();
        this._currentMessages = [];

        // Clear chat UI
        var oChatBox = this.byId("chatMessagesBox");
        if (oChatBox) {
          oChatBox.destroyItems();
        }

        // Clear code results
        var oCodeResultText = this.byId("codeResultText");
        if (oCodeResultText) {
          oCodeResultText.setContent("");
        }
      },

      // Function to save current chat session to history
      _saveChatSession: function () {
        if (this._currentMessages.length === 0) return;

        var oChatSession = {
          id: this._currentChatId || this._generateChatId(),
          title: this._generateChatTitle(),
          messages: [...this._currentMessages],
          createdAt: new Date().toISOString(),
          lastUpdated: new Date().toISOString(),
        };

        // Find existing session or add new one
        var existingIndex = this._chatHistory.findIndex(
          (chat) => chat.id === oChatSession.id
        );
        if (existingIndex >= 0) {
          this._chatHistory[existingIndex] = oChatSession;
        } else {
          this._chatHistory.unshift(oChatSession); // Add to beginning
        }

        // Limit history to last 50 chats
        if (this._chatHistory.length > 50) {
          this._chatHistory = this._chatHistory.slice(0, 50);
        }

        // Save to local storage for persistence
        this._saveHistoryToStorage();
      },

      // Function to update current chat in history (called after each message)
      _updateCurrentChatInHistory: function () {
        if (!this._currentChatId) {
          this._currentChatId = this._generateChatId();
        }
        this._saveChatSession();
      },

      // Generate a chat title based on first user message
      _generateChatTitle: function () {
        var firstUserMessage = this._currentMessages.find(
          (msg) => msg.type === "user"
        );
        if (firstUserMessage) {
          var title =
            typeof firstUserMessage.message === "string"
              ? firstUserMessage.message
              : "Chat";
          return title.length > 50 ? title.substring(0, 50) + "..." : title;
        }
        return "New Chat - " + new Date().toLocaleDateString();
      },

      // Load chat history from storage
      _loadHistoryFromStorage: function () {
        try {
          var storedHistory = localStorage.getItem("chatHistory");
          if (storedHistory) {
            this._chatHistory = JSON.parse(storedHistory);
          }
        } catch (error) {
          console.error("Error loading chat history:", error);
          this._chatHistory = [];
        }
      },

      // Save chat history to storage
      _saveHistoryToStorage: function () {
        try {
          localStorage.setItem(
            "chatHistory",
            JSON.stringify(this._chatHistory)
          );
        } catch (error) {
          console.error("Error saving chat history:", error);
        }
      },

      // Function to load a specific chat session
      loadChatSession: function (sChatId) {
        var oChatSession = this._chatHistory.find(
          (chat) => chat.id === sChatId
        );
        if (!oChatSession) {
          console.error("Chat session not found:", sChatId);
          return;
        }

        // Save current chat before switching
        if (this._currentMessages.length > 0) {
          this._saveChatSession();
        }

        // Set current chat
        this._currentChatId = oChatSession.id;
        this._currentMessages = [...oChatSession.messages];

        // Clear and rebuild chat UI
        this._rebuildChatUI();

        // Close history dialog
        this.onCloseChatHistory();
      },

      // Function to rebuild chat UI from stored messages
      _rebuildChatUI: function () {
        var oChatBox = this.byId("chatMessagesBox");
        var oCodeResultText = this.byId("codeResultText");

        // Clear existing content
        if (oChatBox) {
          oChatBox.destroyItems();
        }
        if (oCodeResultText) {
          oCodeResultText.setContent("");
        }

        // Rebuild messages
        this._currentMessages.forEach((oMessage) => {
          this._recreateMessageUI(oMessage);
        });

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

      // Function to recreate message UI from stored data
      _recreateMessageUI: function (oMessage) {
        var oChatBox = this.byId("chatMessagesBox");
        var sType = oMessage.type;
        var sTimestamp = oMessage.timestamp;

        if (sType === "ai" && oMessage.processedText !== undefined) {
          // Handle parsed AI responses
          if (oMessage.processedText) {
            var oHTML = new sap.ui.core.HTML({
              content: `
                    <div class="chatBubbleContainer ${sType}">
                        <div class="chatBubble ${sType}">
                            <div>${oMessage.processedText}</div>
                            <div class="chatTimestamp">${sTimestamp}</div>
                        </div>
                    </div>
                `,
            });
            oChatBox.addItem(oHTML);
          }

          // Recreate code blocks
          if (oMessage.codeBlocks && oMessage.codeBlocks.length > 0) {
            var oCodeResultText = this.byId("codeResultText");
            if (oCodeResultText) {
              const codeSections = oMessage.codeBlocks.map((block) => {
                const language = block.language || "code";
                const langLabel = `<div class="codeLangLabel">${language.toUpperCase()}</div>`;
                const codeBlock = `
                        <div class="codeBlockWrapper">
                            ${langLabel}
                            <pre><code class="language-${language}">${this.escapeHtml(
                  block.content
                )}</code></pre>
                        </div>
                    `;
                return codeBlock;
              });
              oCodeResultText.setContent(codeSections.join("<br/>"));
            }
          }
        } else {
          // Handle regular messages
          const messageContent =
            typeof oMessage.message === "string"
              ? oMessage.message
              : JSON.stringify(oMessage.message);

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

      // Enhanced onHistoryPress function
      onHistoryPress: async function () {
        // Load history from storage
        this._loadHistoryFromStorage();

        if (!this._oHistoryDialog) {
          this._oHistoryDialog = await Fragment.load({
            id: this.getView().getId(),
            name: "task-runtime.view.ChatHistory",
            controller: this,
          });
        }

        // Bind history data to the dialog
        this._bindHistoryData();
        this._oHistoryDialog.open();
      },

      // Function to bind history data to the dialog
      _bindHistoryData: function () {
        var oHistoryList = this.byId("chatHistoryList");
        if (oHistoryList && this._chatHistory.length > 0) {
          var oModel = new sap.ui.model.json.JSONModel({
            chatHistory: this._chatHistory,
          });
          oHistoryList.setModel(oModel);
          oHistoryList.bindItems({
            path: "/chatHistory",
            template: new sap.m.StandardListItem({
              title: "{title}",
              description: "Messages: {/messages/length} • {createdAt}",
              type: "Active",
              press: this.onHistoryItemPress.bind(this),
            }),
          });
        }
      },

      // Function to handle history item press
      onHistoryItemPress: function (oEvent) {
        var oItem = oEvent.getSource();
        var oContext = oItem.getBindingContext();
        var oChatData = oContext.getObject();

        // Load the selected chat session
        this.loadChatSession(oChatData.id);
      },

      // Helper function to escape HTML (keeping your existing function)
      escapeHtml: function (text) {
        const div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
      },

      // Enhanced onCloseChatHistory function
      onCloseChatHistory: function () {
        if (this._oHistoryDialog) {
          this._oHistoryDialog.close();
          this._oHistoryDialog.destroy();
          this._oHistoryDialog = null;
        }
      },
      // ---------------------------------------Chat Bot -------------------------------------
    });
  }
);
