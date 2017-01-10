import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * Created by carl_000 on 1/9/2017.
 */

class Profile {
    var profileName by property<String>()
    fun profileNameProperty() = getProperty(Profile::profileName)
}

class MainViewModel : ViewModel() {

    val needsSave = SimpleBooleanProperty(true)

    fun saveLastSelectedProfile() {}
}

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
            checkmenuitem("Show Profile Browser", null, null)
            checkmenuitem("Show Console", null, null)
        }
        menu("Help") {
            menuitem("About")
            menuitem("Help")
        }
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

            alert.setHeaderText("Unsaved profile")

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

class ContentFragment : Fragment() {

    override val root = splitpane {

        vbox{
            splitpane {
                vbox {
                    label("Profile Browser")
                    listview<Profile> {}
                }
                splitpane {
                    orientation = Orientation.VERTICAL
                    gridpane {}
                    vbox {
                        label("Console")
                        textarea()
                    }
                }
            }
        }
    }
}

class StatusBarFragment : Fragment() {

    override val root = hbox {
        progressbar()
        label()
    }
}

class ResignatorMainView : View("Resignator") {

    override val root = vbox {

        add(MenuFragment::class)
        add(ContentFragment::class)
        add(StatusBarFragment::class)
    }


}

class ResignatorKtApp : App(ResignatorMainView::class) {
    override fun createPrimaryScene(view: UIComponent) =
            Scene(view.root, 800.0, 600.0)
}