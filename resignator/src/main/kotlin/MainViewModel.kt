import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ViewModel

/**
 * @author carl
 */
class MainViewModel : ViewModel() {

    val needsSave = SimpleBooleanProperty()
    val profileBrowserShowing = SimpleBooleanProperty(true)
    val consoleShowing = SimpleBooleanProperty(true)

    val taskRunning = SimpleBooleanProperty()
    val taskProgress = SimpleDoubleProperty()
    val taskMessage = SimpleStringProperty()

    fun saveLastSelectedProfile() {}
}

