sed -i '/import androidx.compose.material.icons.filled.\*/a \
import org.slashboard.ime.ime.KeyboardActions\
import org.slashboard.ime.ime.KeyboardView\
import org.slashboard.ime.engine.InputMode\
import androidx.compose.ui.viewinterop.AndroidView\
import androidx.compose.material3.ModalBottomSheet\
import androidx.compose.material3.ExperimentalMaterial3Api\
import androidx.compose.material3.OutlinedButton\
import androidx.compose.material3.Button' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
