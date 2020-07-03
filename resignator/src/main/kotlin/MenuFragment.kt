import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import tornadofx.*

/**
 * @author carl
 */
class MenuFragment : Fragment() {

    val mainViewModel : MainViewModel by inject()

    val closeShortcut = KeyCodeCombination( KeyCode.W, KeyCombination.SHORTCUT_DOWN )

    override val root = menubar {
        menu("File") {
            menuitem("New Profile")
            menuitem("Load Profile")
            menu("Recent Profiles") {}
            separator()
            menuitem("Save Profile")
            menuitem("Save As Profile")
            separator()
            menuitem("Settings")
            menuitem("Change Password")
            menuitem(name = "Close", keyCombination = closeShortcut, onAction = { evt -> close() })
        }
        menu("View") {
            checkmenuitem("Show Profile Browser", null, null) {
                selectedProperty().bindBidirectional( mainViewModel.profileBrowserShowing )
            }
            checkmenuitem("Show Console", null, null) {
                selectedProperty().bindBidirectional( mainViewModel.consoleShowing )
            }
        }
        menu("Help") {
            menuitem("About", onAction = { evt -> about() })
            menuitem("Help", onAction = { evt -> help() })
        }
    }

    private fun about() {
        val aboutView = find( AboutView::class )
        aboutView.openModal()
    }

    private fun help() {
    }

    private fun close() {

        var okToClose = true

        if( mainViewModel.needsSave.get() ) {

            val discardButton =
                    ButtonType("Discard", ButtonBar.ButtonData.OTHER)

            val alert = Alert( Alert.AlertType.CONFIRMATION,
                    "Save profile?",
                    ButtonType.OK,
                    ButtonType.CANCEL,
                    discardButton)

            alert.headerText = "Unsaved profile"

            alert.showAndWait().ifPresent { response ->
                if( response == ButtonType.OK ) {
                    mainViewModel.saveLastSelectedProfile()
                } else if( response == ButtonType.CANCEL ) {
                    okToClose = false
                }
                // no special logic for OTHER
            }
        }

        if( okToClose ) {
            Platform.exit()
            System.exit(0)  // close webstart
        }
    }
}