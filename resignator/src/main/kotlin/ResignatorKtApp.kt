import javafx.animation.FadeTransition
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.SplitPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Duration
import tornadofx.*

/**
 * Created by carl_000 on 1/9/2017.
 */

val HELP_LINK = "http://www.bekwam.com/resignator/help.html"

class Profile {
    var profileName by property<String>()
    fun profileNameProperty() = getProperty(Profile::profileName)
}

class MainViewModel : ViewModel() {

    val needsSave = SimpleBooleanProperty()
    val profileBrowserShowing = SimpleBooleanProperty(true)
    val consoleShowing = SimpleBooleanProperty(true)

    val taskRunning = SimpleBooleanProperty()
    val taskProgress = SimpleDoubleProperty()
    val taskMessage = SimpleStringProperty()

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
        println("TODO: call $HELP_LINK here")
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

class ContentFragment : Fragment() {

    var outerContainer : SplitPane by singleAssign()
    var innerContainer : SplitPane by singleAssign()
    var consoleVBox : VBox by singleAssign()
    var profileBrowserVBox : VBox by singleAssign()

    val mainViewModel : MainViewModel by inject()

    override val root = splitpane {
        vbox{
            outerContainer = splitpane {
                profileBrowserVBox = vbox {
                    label("Profile Browser") {
                        padding = Insets(2.0)
                    }
                    listview<Profile> {
                        vgrow = Priority.ALWAYS
                    }
                    spacing = 4.0
                }
                innerContainer = splitpane {
                    orientation = Orientation.VERTICAL
                    vbox {
                        label("Profile") {
                            padding = Insets(2.0)
                        }
                        gridpane {
                            row {
                                label("Name")
                                textfield()
                            }
                            row {
                                label("Type")
                                combobox<String>()
                            }
                            row {
                                label("Source JAR")
                                textfield()
                                button("Browse")
                            }
                            row {
                                label("Target JAR")
                                textfield()
                                button("Browse")
                                button("Copy")
                            }
                            row {
                                button("Configure")
                                checkbox("Replace existing signatures?") { }
                                button("Sign")
                            }
                            vgrow = Priority.ALWAYS
                            vgap = 10.0
                            hgap = 10.0
                            padding = Insets(10.0)
                            alignment = Pos.CENTER_LEFT
                        }
                        spacing = 4.0
                    }
                    consoleVBox = vbox {
                        label("Console") {
                            padding = Insets(2.0)
                        }
                        textarea {
                            vgrow = Priority.ALWAYS
                        }
                        spacing = 4.0
                    }
                }
                vgrow = Priority.ALWAYS
            }
            padding = Insets(10.0)
        }
        vgrow = Priority.ALWAYS
    }

    init {

        mainViewModel.consoleShowing.addListener {
            observableValue, ov, nv -> showContentPanel(nv, innerContainer, consoleVBox, false)
        }

        mainViewModel.profileBrowserShowing.addListener {
            observableValue, ov, nv -> showContentPanel(nv, outerContainer, profileBrowserVBox, true)
        }
    }

    private fun showContentPanel(show : Boolean, container : SplitPane, panel : Node, first : Boolean) {

        if( show && !container.items.contains(panel) ) {

            panel.opacity = 0.0

            if( !first ) {
                container.items.add(panel)
            } else {
                container.items.add(0, panel)
            }

            panel.fade(Duration.millis(400.0), opacity=1.0)

        } else if( !show && container.items.contains(panel) ) {

            val ft = FadeTransition(Duration.millis(300.0), panel)
            ft.fromValue = 1.0
            ft.toValue = 0.1
            ft.cycleCount = 1
            ft.isAutoReverse = false
            ft.play()
            ft.setOnFinished { e -> container.items.remove(panel) }
        }
    }
}

class StatusBarFragment : Fragment() {

    val mainViewModel : MainViewModel by inject()

    override val root = hbox {
        progressbar{
            bind( mainViewModel.taskProgress )
        }
        label {
            bind( mainViewModel.taskMessage )
        }
        visibleProperty().bind( mainViewModel.taskRunning )
        padding = Insets(10.0)
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
            Scene(view.root, 1024.0, 768.0)
}