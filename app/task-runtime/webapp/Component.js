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
          this._loadContextNodes(oArgs.taskId);
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

      _loadContextNodes: function (taskId) {
        return this.getModel()
          .bindList("/Tasks('" + taskId + "')/contextNodes")
          .requestContexts()
          .then((aCtx) => {
            const items = aCtx.map((c) => c.getObject());

            // Group items berdasarkan path hierarchy
            const treeMap = {};

            items.forEach((item) => {
              // Split path menggunakan dot notation
              const pathParts = item.path
                .split(".")
                .filter((part) => part !== "");
              let currentPath = "";

              // Buat hierarchy untuk setiap level
              pathParts.forEach((part, index) => {
                const previousPath = currentPath;
                currentPath = currentPath ? currentPath + "." + part : part;

                // Buat node jika belum ada
                if (!treeMap[currentPath]) {
                  treeMap[currentPath] = {
                    path: currentPath,
                    label: part,
                    children: [],
                    isFolder: true,
                  };
                }

                // Link dengan parent
                if (previousPath && treeMap[previousPath]) {
                  const parent = treeMap[previousPath];
                  const current = treeMap[currentPath];

                  if (
                    !parent.children.find(
                      (child) => child.path === current.path
                    )
                  ) {
                    parent.children.push(current);
                  }
                }
              });

              // Tambahkan item data ke node terakhir
              const finalPath = item.path;
              if (treeMap[finalPath]) {
                treeMap[finalPath].children.push({
                  ID: item.ID,
                  path: item.path,
                  type: item.type,
                  label: item.label,
                  value: item.value,
                  children: [],
                  isFolder: false,
                });
              }
            });

            // Ambil root nodes
            const rootNodes = Object.values(treeMap).filter((node) => {
              const parentPath = node.path.includes(".")
                ? node.path.substring(0, node.path.lastIndexOf("."))
                : "";
              return parentPath === "" || !treeMap[parentPath];
            });

            // Sort secara rekursif
            const sortChildren = (node) => {
              if (node.children && node.children.length > 0) {
                node.children.sort((a, b) => {
                  if (a.isFolder !== b.isFolder) {
                    return a.isFolder ? -1 : 1;
                  }
                  return a.label.localeCompare(b.label);
                });
                node.children.forEach((child) => sortChildren(child));
              }
            };

            rootNodes.forEach((rootNode) => sortChildren(rootNode));
            this.getModel("contextNodes").setData({ nodes: rootNodes });
          });
      },
    });
  }
);
