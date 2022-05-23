import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.samourai.wallet.theme.SamouraiWalletTheme
import kotlinx.coroutines.Job

@Composable
fun SweepPrivateKeyView(onDismiss: () -> Job) {
    SamouraiWalletTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    content = {

                    }
                )
            }
        ) {

        }
    }
}


@Composable
@Preview(widthDp = 400, heightDp = 320)
fun SweepPreview() {

}