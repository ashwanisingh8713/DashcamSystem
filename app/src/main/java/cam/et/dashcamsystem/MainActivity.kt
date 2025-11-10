package cam.et.dashcamsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cam.et.dashcamsystem.app.presentation.components.SystemUsage
import cam.et.dashcamsystem.ui.theme.DashcamSystemTheme
import cam.et.dashcamsystem.permissions.PermissionManager
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)

        // If all permissions are not granted, invoke callback behavior directly.
        if (!permissionManager.allPermissionsGranted()) {
            permissionManager.requestPermissions(object : PermissionManager.Callback {
                override fun onResult(allGranted: Boolean, denied: List<String>) {
                    if (!allGranted) {
                        // Simple feedback; the PermissionManager handles the requests.
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Permissions denied: ${denied.joinToString()}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }

        setContent {
            DashcamSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                        Greeting(name = "Assignment")
                        Spacer(modifier = Modifier.height(12.dp))
                        // Centralized SystemUsage composable (starts/stops monitor internally)
                        SystemUsage()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = "EdgeTensor $name",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DashcamSystemTheme {
        Greeting("Android")
    }
}