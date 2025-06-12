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

      onDeleteSubTask: function () {},

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

      // Initialize history on controller init
      onInit: function () {
        // Your existing onInit code...

        // Initialize chat history
        this._chatHistory = [];
        this._currentMessages = [];
        this._currentChatId = null;
        this._loadHistoryFromStorage();

        // Start with a new chat
        this.startNewChat();
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
