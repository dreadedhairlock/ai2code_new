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
    MessageToast,
    MessageBox,
    SelectDialog,
    Fragment
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

      onDeleteCNData: function () {
        const oTree = this.byId("docTree");
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

        // Tampilkan konfirmasi sebelum delete
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

                  // Pastikan context valid
                  await oContext.requestObject();
                  const oBoundContext = oContext.getBoundContext();

                  if (oBoundContext) {
                    await oBoundContext.delete();
                    MessageToast.show("Context node deleted successfully.");
                    // Reload ulang tree/list
                    await this.getOwnerComponent()._loadContextNodes(
                      this._sTaskId
                    );
                  } else {
                    MessageToast.show("Could not find context with ID: " + sID);
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

      onEditCNData: function () {
        const oTree = this.byId("docTree");
        const oSelected = oTree.getSelectedItem();
        const oJsonCtx = oSelected.getBindingContext("contextNodes");
        const sId = oJsonCtx.getProperty("ID");

        if (!sId) {
          MessageToast.show("Please select a context node item to edit!");
          return;
        }
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

      _createTask: function () {
        const sTaskName = Element.getElementById("taskNameDetail").getValue();
        const sTaskDescription = Element.getElementById(
          "taskDescriptionDetail"
        ).getValue();
        const sTaskTypeId =
          Element.getElementById("taskTypeIdDetail").getValue();
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
            async function () {
              MessageToast.show("Task created successfully");
              await this.getOwnerComponent()._loadMainTasks(this._sTaskId);
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

      onSubmitQuery: async function () {
        var oInput = this.byId("chatInput");
        var sMessage = oInput.getValue().trim();
        if (sMessage) {
          // Add user message
          this.addChatMessage(sMessage, "user");
          // Clear input
          oInput.setValue("");
          console.log(sMessage);
          // Simulate AI response (replace with your actual AI call)
          try {
            const API_KEY = "AIzaSyDyE_D4ej7SljvLAV5vWMmkQxg5OjGv5r4";
            const model = "gemini-2.0-flash";
            const response = await fetch(
              `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${API_KEY}`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json", // Replace with a secure way of storing it
                },
                body: JSON.stringify({
                  contents: [
                    {
                      role: "user",
                      parts: [
                        {
                          text: sMessage,
                        },
                      ],
                    },
                  ],
                }),
              }
            );
            console.log(response);
            const data = await response.json();
            console.log(data);
            const reply =
              data.candidates?.[0]?.content?.parts?.[0]?.text ||
              "No response from AI.";

            this.addChatMessage(reply, "ai");
          } catch (error) {
            this.addChatMessage("Error: " + error.message, "ai");
          }
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

        if (sType === "ai") {
          var oCodeResultText = this.byId("codeResultText");
          if (oCodeResultText) {
            oCodeResultText.setText(sMessage);
          }
        }
      },

      // ---------------------------------------Chat Bot -------------------------------------
    });
  }
);
