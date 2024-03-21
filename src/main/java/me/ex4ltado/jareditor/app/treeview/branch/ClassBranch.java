package me.ex4ltado.jareditor.app.treeview.branch;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.jetbrains.annotations.NotNull;

public class ClassBranch extends Branch {

    private final String className;
    private final String[] treePackagePath;

    public ClassBranch(String className, String[] treePackagePath) {
        this.className = className;
        this.treePackagePath = treePackagePath;
    }

    @NotNull
    private static Text classIconText() {
        Text classIcon = new Text("C");
        //classIcon.setId("class-icon");
        classIcon.setStyle("-fx-font-family: Consolas;" +
                           "-fx-font-smoothing-type: gray;" +
                           "-fx-fill: WHITE;" +
                           "-fx-font-weight: bolder;" +
                           "-fx-font-size: 15px;");

        // Update text bounds
        //new Scene(new Group(classIcon));
        classIcon.applyCss();

        classIcon.setBoundsType(TextBoundsType.LOGICAL_VERTICAL_CENTER);
        return classIcon;
    }

    public static Node getIcon() {
        final Text classIcon = classIconText();

        final double padding = 3;
        Circle circle = new Circle((classIcon.getLayoutBounds().getWidth() + classIcon.getStrokeWidth()) / 2 + padding);
        circle.setFill(Color.GREEN);

        StackPane layout = new StackPane();

        layout.getChildren().addAll(circle, classIcon);
        layout.setPadding(new Insets(padding));

        return layout;
    }

    public String getClassName() {
        return className;
    }

    public String[] getTreePackagePath() {
        return treePackagePath;
    }

    public String getPackagePathString() {
        return String.join("/", treePackagePath);
    }

    @Override
    public String toString() {
        return className + ".class";
    }

}
