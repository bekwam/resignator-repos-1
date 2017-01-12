import javafx.scene.Scene
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tornadofx.*
import java.security.Security

/**
 * Created by carl_000 on 1/9/2017.
 */

val HELP_LINK = "http://www.bekwam.com/resignator/help.html"

class Profile {
    var profileName by property<String>()
    fun profileNameProperty() = getProperty(Profile::profileName)
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

    init {
        Security.addProvider(BouncyCastleProvider())
    }
}