/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui;

import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.jackhuang.hmcl.ui.construct.DialogCloseEvent;
import org.jackhuang.hmcl.ui.construct.DialogPane;
import org.jackhuang.hmcl.ui.construct.HintPane;
import org.jackhuang.hmcl.ui.construct.MessageDialogPane;
import org.jackhuang.hmcl.util.RegionUtils;
import org.jetbrains.annotations.NotNullByDefault;

import static org.jackhuang.hmcl.ui.FXUtils.segmentToTextFlow;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/// Startup gate requiring mainland China users to confirm that they are at least 22 years old
/// and that they accept the Minecraft End User License Agreement.
///
/// The confirmation is never persisted, so the gate is shown on every launcher startup.
/// Dismissing the dialog in any way, including pressing Escape, terminates the launcher.
@NotNullByDefault
public final class ChinaCompliancePane extends DialogPane {

    /// The checkbox confirming the user is at least 22 years old.
    private final JFXCheckBox ageCheckBox = new JFXCheckBox();

    /// The checkbox confirming the user accepts the Minecraft End User License Agreement.
    private final JFXCheckBox eulaCheckBox = new JFXCheckBox();

    /// Whether the user pressed the confirmation button.
    private boolean accepted;

    /// Creates the compliance gate dialog.
    public ChinaCompliancePane() {
        setTitle(i18n("china_compliance.title"));

        HintPane hint = new HintPane(MessageDialogPane.MessageType.WARNING);
        hint.setText(i18n("china_compliance.notice"));

        VBox content = new VBox(6,
                hint,
                createCheckBoxRow(ageCheckBox, i18n("china_compliance.age")),
                createCheckBoxRow(eulaCheckBox, i18n("china_compliance.eula")));
        content.setPadding(new Insets(4, 0, 4, 0));
        setBody(content);

        // The confirmation button stays unavailable until both statements are checked.
        validProperty().bind(Bindings.createBooleanBinding(() -> ageCheckBox.isSelected() && eulaCheckBox.isSelected(),
                ageCheckBox.selectedProperty(), eulaCheckBox.selectedProperty()));

        cancelButton.setText(i18n("china_compliance.exit"));

        // Any close request that is not the confirmation terminates the launcher,
        // which keeps the gate impossible to dismiss silently.
        addEventFilter(DialogCloseEvent.CLOSE, event -> {
            if (accepted) {
                return;
            }

            event.consume();
            Platform.exit();
        });
    }

    /// Builds one statement with its checkbox, keeping the box and the text on the same line.
    ///
    /// The text is a sibling of the checkbox rather than its graphic: the checkbox skin lays
    /// out its label with text metrics, so a graphic label ends up shifted against the box.
    private static HBox createCheckBoxRow(JFXCheckBox checkBox, String text) {
        TextFlow label = segmentToTextFlow(text, Controllers::onHyperlinkAction);
        // The EULA link consumes its own clicks, so only plain text reaches this handler.
        label.setOnMouseClicked(event -> checkBox.setSelected(!checkBox.isSelected()));

        HBox row = new HBox(6, checkBox, label);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(label, Priority.ALWAYS);
        return row;
    }

    /// Shows the compliance gate unless the machine is detected as being outside mainland China.
    ///
    /// Only the synchronous machine detection is used here, because the network detection would
    /// block the JavaFX application thread on an HTTP request.
    public static void showIfRequired() {
        if ("CN".equals(RegionUtils.MACHINE_REGION)) {
            Controllers.dialog(new ChinaCompliancePane());
        }
    }

    /// Marks the gate as accepted and lets the close event through.
    @Override
    protected void onAccept() {
        accepted = true;
        super.onAccept();
    }
}
