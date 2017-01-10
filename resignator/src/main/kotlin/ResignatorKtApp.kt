import javafx.scene.Scene
import tornadofx.*

/**
 * Created by carl_000 on 1/9/2017.
 */

class ResignatorMainView : View("Resignator") {
    override val root = vbox() {
        menubar {
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
                menuitem("Close")
            }
            menu("View") {
                checkmenuitem("Show Profile Browser")
                checkmenuitem("Show Console")
            }
        }
    }
}

class ResignatorKtApp : App(ResignatorMainView::class) {
    override fun createPrimaryScene(view: UIComponent) =
            Scene(view.root, 800.0, 600.0)
}