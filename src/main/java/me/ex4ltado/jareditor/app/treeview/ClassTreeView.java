package me.ex4ltado.jareditor.app.treeview;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import me.ex4ltado.jareditor.JeFile;
import me.ex4ltado.jareditor.app.treeview.branch.Branch;
import me.ex4ltado.jareditor.app.treeview.branch.ClassBranch;
import me.ex4ltado.jareditor.app.treeview.branch.PackageBranch;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

// TODO: Fix a bug when 2 classes have the same name (in different package or another .jar file), their content will always be the same as the first class decompiled
public class ClassTreeView {

    private static final String ROOT_PACKAGE = "Classes";
    private static final String PACKAGE_SEPARATOR = "/";
    private static TreeView<Branch> treeView = null;
    private static HashMap<JeFile, HashMap<String, TreeItem<Branch>>> treeItems = new HashMap<>(); // TODO: Create JarBranch
    private static TreeItem<Branch> treeRoot = new TreeItem<>(new PackageBranch(ROOT_PACKAGE));

    private static void addClass(@NotNull JeFile jar, @NotNull JeClassWrapper jeClass) {
        final String jarName = jar.getJarName();
        final String className = jeClass.getClassName();

        // The absolute package is the jar name + the package name
        final String absolutePackage = jarName + (jeClass.getPackageName() == null ? "" : PACKAGE_SEPARATOR + jeClass.getPackageName());
        final String classPath = absolutePackage + PACKAGE_SEPARATOR + className + ".class";

        final String[] packagesSplit = classPath.replace(".class", "").split(PACKAGE_SEPARATOR);

        StringBuilder currentPackage = new StringBuilder();
        for (int i = 0; i < packagesSplit.length; i++) {

            if (i > 0)
                currentPackage.append(PACKAGE_SEPARATOR);

            currentPackage.append(packagesSplit[i]);

            // The last item is always a class
            boolean isAClass = i == packagesSplit.length - 1;

            if (treeItems.get(jar).containsKey(currentPackage.toString()) && !isAClass) {
                continue;
            }

            TreeItem<Branch> item;

            if (isAClass) {
                item = new TreeItem<>(new ClassBranch(className, absolutePackage.split(PACKAGE_SEPARATOR)), ClassBranch.getIcon());
            } else {
                final ImageView icon = new ImageView(PackageBranch.ICON_PACKAGE);
                icon.setFitHeight(18);
                icon.setFitWidth(18);
                item = new TreeItem<>(new PackageBranch(packagesSplit[i]), icon);
            }

            treeItems.get(jar).put(currentPackage.toString(), item);
        }

        // Class in the base package directory
        /*if (packagesSplit.length == 1)
            ROOT.getChildren().add(ITEM_HASH_MAP.get(jar).get(packagesSplit[0]));*/
    }

    public static TreeView<Branch> getTreeView() {
        if (treeView == null) {
            treeView = new TreeView<>();
            treeView.setMinWidth(200);
        }
        return treeView;
    }

    public static void addJar(@NotNull JeFile jar) {
        treeItems.put(jar, new HashMap<>());

        final String jarName = jar.getJarName().replace(".", "-");

        jar.getClassList().stream()
                .map(p -> new JeClassWrapper(p.getFirst()))
                .forEach(c -> addClass(jar, c));

        // If the root package isn't added, add it to the root of the tree
        if (!treeRoot.getChildren().contains(treeItems.get(jar).get(jarName)))
            treeRoot.getChildren().add(treeItems.get(jar).get(jarName));
    }

    public static void remapTreeView(JeFile jar) {
        treeItems.get(jar).entrySet()
                .stream()
                .sorted(Comparator.comparing(o -> o.getValue().getValue().toString()))
                .sorted(Comparator.comparing(o -> o.getValue().getValue().getClass() == ClassBranch.class))
                .forEach(entry -> {
                    String key = entry.getKey().replace(".class", "");
                    final List<String> packagesHierarchy = Arrays.stream(key.split(PACKAGE_SEPARATOR)).collect(Collectors.toList());
                    packagesHierarchy.remove(packagesHierarchy.size() - 1);
                    String packageName = String.join(PACKAGE_SEPARATOR, packagesHierarchy);
                    if (!packageName.isEmpty()) {
                        treeItems.get(jar).get(packageName).getChildren().add(entry.getValue());
                    }
                });
    }

    public static void showRoot() {
        if (treeView == null)
            return;

        treeView.setRoot(treeRoot);
        treeView.setShowRoot(true);

        treeRoot.setExpanded(true);
        //remapTreeView();
    }

    public static void cleanTree() {
        treeView.setRoot(null);
        treeRoot = new TreeItem<>(new PackageBranch(ROOT_PACKAGE));
        treeItems = new HashMap<>();
        showRoot();
    }

    public static class JeClassWrapper {
        private final String classPath;

        public JeClassWrapper(String classPath) {
            this.classPath = classPath;
        }

        public String getClassName() {
            final String name2 = getClassPath(true);
            final int index = name2.lastIndexOf(PACKAGE_SEPARATOR);
            if (index == -1)
                return name2;
            return name2.substring(index + 1);
        }

        public String getPackageName() {
            final String name2 = getClassPath(true);
            final int index = name2.lastIndexOf(PACKAGE_SEPARATOR);
            if (index == -1)
                return null;
            return name2.substring(0, index);
        }

        public String getClassPath(boolean noClassSuffix) {
            if (noClassSuffix)
                return classPath.endsWith(".class") ? classPath.substring(0, classPath.length() - 6) : classPath;
            return classPath;
        }

    }

}
