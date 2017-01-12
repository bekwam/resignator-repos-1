import javafx.geometry.Insets
import tornadofx.*

/**
 * @author carl
 */
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