sap.ui.define(
  [
    "sap/ui/core/UIComponent",
    "task-runtime/model/models",
    "sap/ui/model/json/JSONModel",
  ],
  (UIComponent, models, JSONModel) => {
    "use strict";

    return UIComponent.extend("task-runtime.Component", {
      metadata: {
        manifest: "json",
        interfaces: ["sap.ui.core.IAsyncContentCreation"],
      },

      init() {
        // call the base component's init function
        UIComponent.prototype.init.apply(this, arguments);

        // set the device model
        this.setModel(models.createDeviceModel(), "device");

        const oCNModel = new JSONModel({ nodes: [] });
        this.setModel(oCNModel, "contextNodes");

        const oBIModel = new JSONModel({ results: [] });
        this.setModel(oBIModel, "botInstances");

        // enable routing
        this.getRouter().initialize();
        this.getRouter().attachRouteMatched(this._onRouteMatched.bind(this));
      },

      _onRouteMatched: function (oEvent) {
        const sRoute = oEvent.getParameter("name");
        const oArgs = oEvent.getParameter("arguments");
        if (sRoute === "RouteTaskDetail" && oArgs.taskId) {
          // reuse loader
          this._loadBotInstances(oArgs.taskId);
          this._loadContextNodes(oArgs.taskId);
        }
      },

      _loadBotInstances: function (taskId) {
        return this.getModel()
          .bindList("/Tasks('" + taskId + "')/botInstances")
          .requestContexts()
          .then((aCtx) => {
            const aData = aCtx.map((c) => {
              const o = c.getObject();
              o.type = "bot";
              return o;
            });
            this.getModel("botInstances").setData({ results: aData });
          });
      },

      _loadContextNodes: function (taskId) {
        return this.getModel()
          .bindList("/Tasks('" + taskId + "')/contextNodes")
          .requestContexts()
          .then((aCtx) => {
            const sectionMap = {};
            aCtx
              .map((c) => c.getObject())
              .forEach((item) => {
                if (!sectionMap[item.path]) {
                  sectionMap[item.path] = {
                    path: item.path,
                    label: item.path,
                    children: [],
                  };
                }
                sectionMap[item.path].children.push({
                  ID: item.ID,
                  path: item.path,
                  type: item.type,
                  label: item.label,
                  value: item.value,
                  children: [],
                });
              });
            const aRoots = Object.values(sectionMap);
            this.getModel("contextNodes").setData({ nodes: aRoots });
          });
      },
    });
  }
);
