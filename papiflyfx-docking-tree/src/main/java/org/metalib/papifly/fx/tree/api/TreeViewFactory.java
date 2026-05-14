package org.metalib.papifly.fx.tree.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

public class TreeViewFactory implements ContentFactory {

    public static final String FACTORY_ID = "tree-view";

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) {
            return null;
        }
        return createDefaultTreeView();
    }

    public static TreeView<String> createDefaultTreeView() {
        TreeView<String> treeView = new TreeView<>();
        treeView.setEditCommitHandler(TreeItem::setValue);
        treeView.setRoot(createDefaultRoot());
        return treeView;
    }

    public static TreeItem<String> createDefaultRoot() {
        TreeItem<String> root = new TreeItem<>("Workspace");
        TreeItem<String> src = new TreeItem<>("src");
        TreeItem<String> main = new TreeItem<>("main");
        TreeItem<String> java = new TreeItem<>("java");
        TreeItem<String> org = new TreeItem<>("org");
        TreeItem<String> metalib = new TreeItem<>("metalib");
        TreeItem<String> app = new TreeItem<>("App.java");
        TreeItem<String> config = new TreeItem<>("Config.java");
        TreeItem<String> resources = new TreeItem<>("resources");
        TreeItem<String> applicationProperties = new TreeItem<>("application.properties");
        TreeItem<String> tests = new TreeItem<>("test");
        TreeItem<String> appTest = new TreeItem<>("AppTest.java");

        root.addChild(src);
        src.addChild(main);
        main.addChild(java);
        java.addChild(org);
        org.addChild(metalib);
        metalib.addChild(app);
        metalib.addChild(config);
        main.addChild(resources);
        resources.addChild(applicationProperties);
        src.addChild(tests);
        tests.addChild(appTest);

        root.setExpanded(true);
        src.setExpanded(true);
        main.setExpanded(true);
        java.setExpanded(true);
        org.setExpanded(true);
        metalib.setExpanded(true);
        resources.setExpanded(true);
        tests.setExpanded(true);
        return root;
    }
}
