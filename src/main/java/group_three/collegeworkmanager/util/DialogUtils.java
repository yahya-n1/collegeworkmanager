package group_three.collegeworkmanager.util;

import javafx.scene.control.*;

import java.util.Objects;

public class DialogUtils {

    public static void AddDialogStyling(Dialog<?> dialog){
        DialogPane pane = dialog.getDialogPane();

        pane.getStylesheets().add(Objects.requireNonNull(DialogUtils.class.getResource("/group_three/collegeworkmanager/styles.css")).toExternalForm());

        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("bg-primary");
        }

        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("bg-error");
        }
    }

    public static void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        DialogUtils.AddDialogStyling(a);
        a.showAndWait();
    }

    public static void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        DialogUtils.AddDialogStyling(a);
        a.setTitle(title);
        a.showAndWait();
    }
}
