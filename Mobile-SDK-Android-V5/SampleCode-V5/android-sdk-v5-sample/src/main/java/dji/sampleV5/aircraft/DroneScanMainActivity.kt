package dji.sampleV5.aircraft

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.models.MSDKManagerVM
import dji.sampleV5.aircraft.models.globalViewModels

/**
 * Activity principal simplificada para DroneScan V7.
 * Solo incluye la funcionalidad esencial de conexión DJI y escaneado de códigos.
 */
class DroneScanMainActivity : AppCompatActivity() {

    private val msdkManagerVM: MSDKManagerVM by globalViewModels()
    private lateinit var statusTextView: TextView
    private lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dronescan_main)

        statusTextView = findViewById(R.id.statusTextView)
        scanButton = findViewById(R.id.scanButton)

        // Observar estado de conexión del SDK
        msdkManagerVM.lvRegisterState.observe(this) { pair ->
            val (isRegistered, error) = pair
            if (isRegistered) {
                statusTextView.text = "✅ DJI SDK Conectado\nListo para escanear códigos"
                scanButton.isEnabled = true
            } else {
                statusTextView.text = "❌ Error DJI SDK: ${error?.description() ?: "Desconocido"}"
                scanButton.isEnabled = false
            }
        }

        // Observar conexión del producto (drone)
        msdkManagerVM.lvProductConnectionState.observe(this) { pair ->
            val (isConnected, productId) = pair
            if (isConnected) {
                statusTextView.text = "${statusTextView.text}\n🚁 Drone conectado (ID: $productId)"
            } else {
                statusTextView.text = "${statusTextView.text}\n🚁 Drone desconectado"
            }
        }

        // Botón para iniciar DroneScan
        scanButton.setOnClickListener {
            val intent = Intent(this, DroneScanActivity::class.java)
            startActivity(intent)
        }
    }
}
