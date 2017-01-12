import javafx.geometry.Pos
import javafx.scene.text.TextAlignment
import tornadofx.View
import tornadofx.label
import tornadofx.vbox

/**
 * @author carl
 */
class AboutView : View("About") {

    override val root = vbox {
        label("Resignator\n\u00a9 2017 Bekwam, Inc.\nSupport: dev@bekwam.com"){
            textAlignment = TextAlignment.CENTER
        }
        alignment = Pos.CENTER
        prefWidth = 568.0
        prefHeight = 320.0
    }
}