import javafx.animation.FadeTransition
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Duration
import tornadofx.*

/**
 * @author carl
 */
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