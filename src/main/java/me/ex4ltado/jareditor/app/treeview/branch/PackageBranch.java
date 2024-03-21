package me.ex4ltado.jareditor.app.treeview.branch;

import javafx.scene.image.Image;

import java.util.Objects;

public class PackageBranch extends Branch {

    public static final Image ICON_PACKAGE = new Image(
            Objects.requireNonNull(PackageBranch.class.getResourceAsStream("/img/package.png")));

    private final String name;

    public PackageBranch(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageBranch that = (PackageBranch) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
