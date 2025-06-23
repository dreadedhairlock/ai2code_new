sap.ui.define(
  [
    "sap/ui/core/UIComponent",
    "task-runtime/model/models",
    "sap/ui/model/json/JSONModel",
    "sap/ui/model/Filter",
    "sap/ui/model/FilterOperator",
  ],
  (UIComponent, models, JSONModel, Filter, FilterOperator) => {
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
        this.setModel(oBIModel, "taskTree");

        // enable routing
        this.getRouter().initialize();
        this.getRouter().attachRouteMatched(this._onRouteMatched.bind(this));
      },

      _onRouteMatched: function (oEvent) {
        const sRoute = oEvent.getParameter("name");
        const oArgs = oEvent.getParameter("arguments");
        if (sRoute === "RouteTaskDetail" && oArgs.taskId) {
          // reuse loader
          this._loadMainTasks(oArgs.taskId);
          // this._loadContextNodes(oArgs.taskId);
        }
      },

      _loadMainTasks: function () {
        return this.getModel()
          .bindList("/Tasks", null, null, [
            new Filter("isMain", FilterOperator.EQ, true),
          ])
          .requestContexts()
          .then((aCtx) => {
            const aData = aCtx.map((c) => {
              const o = c.getObject();
              o.type = "task"; // Set type sebagai task
              o.nodes = []; // Initialize nodes untuk children
              return o;
            });
            // Set sebagai root data untuk tree
            this.getModel("taskTree").setData(aData);
          });
      },
    });
  }
);
