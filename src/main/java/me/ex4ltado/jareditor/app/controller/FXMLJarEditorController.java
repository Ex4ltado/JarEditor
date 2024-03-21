package me.ex4ltado.jareditor.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javassist.ClassPool;
import me.ex4ltado.jareditor.ClassManager;
import me.ex4ltado.jareditor.JeFile;
import me.ex4ltado.jareditor.app.treeview.ClassTreeView;
import me.ex4ltado.jareditor.app.treeview.branch.Branch;
import me.ex4ltado.jareditor.app.treeview.branch.ClassBranch;
import me.ex4ltado.jareditor.decompiler.CfrDecompiler;
import me.ex4ltado.jareditor.decompiler.Decompiler;
import me.ex4ltado.jareditor.util.MaxSizeArrayList;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class FXMLJarEditorController {

    public static final ClassManager CLASS_MANAGER = new ClassManager(ClassPool.getDefault());

    private final TabPane classesTabPane = new TabPane();
    private final Decompiler cfrDecompiler = new CfrDecompiler(CLASS_MANAGER);
    private final MaxSizeArrayList<JeFile> loadedJars = new MaxSizeArrayList<>(10, 0); // TODO

    @FXML
    Label lblDragJarHere;

    @FXML
    BorderPane borderPane;

    @FXML
    public void onDragOver(DragEvent event) {
        if (!event.getDragboard().hasFiles())
            return;
        event.acceptTransferModes(TransferMode.ANY);
    }

    @FXML
    public void onDragDropped(DragEvent event) throws IOException {
        final List<File> files = event.getDragboard().getFiles();
        final List<File> collect = files.stream().filter(file -> file.getName().endsWith(".jar")).collect(Collectors.toList());

        if (collect.isEmpty())
            return;

        if (CLASS_MANAGER.isEmpty())
            initializeCenterPane();

        for (File file : collect) {
            final JarFile jar = new JarFile(file);
            JeFile jeFile = new JeFile(jar);

            jeFile.read(null);

            CLASS_MANAGER.putJar(jeFile);

            jeFile.getClassList().forEach(p -> CLASS_MANAGER.makeClass(jeFile, p.getSecond()));

            addJarToClassesTreeView(jeFile);
        }
    }

    private void handleTreeViewMouseClick(@NotNull MouseEvent event) {
        TreeView<Branch> treeView = ClassTreeView.getTreeView();
        if (event.getClickCount() == 2) {

            if (treeView.getSelectionModel().getSelectedItem() == null)
                return;

            final Branch branch = treeView.getSelectionModel().getSelectedItem().getValue();
            if (branch instanceof ClassBranch) {
                final ClassBranch classBranch = (ClassBranch) branch;
                displayDecompiledClass(classBranch.getClassName(), classBranch.getPackagePathString());
            }
        }
    }

    private void displayDecompiledClass(String className, String packagePath) {
        TextArea decompiledTextArea = new TextArea();
        decompiledTextArea.setStyle("-fx-font-size: 16;");
        decompiledTextArea.setWrapText(true);
        decompiledTextArea.setEditable(false);

        final int index = packagePath.indexOf("/");

        final String jeFileName;
        final String classPath;

        if (index != -1) {
            jeFileName = packagePath.substring(0, index);
            classPath = packagePath.substring(index + 1) + "/" + className;
        } else {
            jeFileName = packagePath;
            classPath = className;
        }

        final JeFile jar = CLASS_MANAGER.getJar(jeFileName);

        if (jar == null)
            throw new IllegalStateException("Jar not found");

        final Decompiler.DecompilationResult result = cfrDecompiler.decompile(jar, classPath);

        if (result != null) {
            decompiledTextArea.setText(result.getDecompiled().getJava());
        } else {
            decompiledTextArea.setText("Error decompiling class");
        }

        classesTabPane.getTabs().add(new Tab(className, decompiledTextArea));
        classesTabPane.getSelectionModel().select(classesTabPane.getTabs().size() - 1);
    }

    private void refreshClassesTreeView() {
        ClassTreeView.cleanTree();
    }

    private void addJarToClassesTreeView(JeFile file) {
        refreshClassesTreeView();
        loadedJars.add(file);
        ClassTreeView.addJar(file);
        ClassTreeView.remapTreeView(file);
        classesTabPane.getTabs().clear();
    }

    private void initializeCenterPane() {
        borderPane.getChildren().remove(borderPane.getCenter());

        SplitPane splitPane = new SplitPane();

        ClassTreeView.getTreeView().setOnMouseClicked(this::handleTreeViewMouseClick);

        splitPane.getItems().addAll(ClassTreeView.getTreeView(), classesTabPane);
        splitPane.setDividerPositions(0.25, 0.75);

        borderPane.setCenter(splitPane);

        ClassTreeView.showRoot();
    }

}
