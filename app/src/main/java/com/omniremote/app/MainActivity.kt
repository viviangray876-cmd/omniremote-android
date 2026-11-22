package com.omniremote.app
import android.annotation.SuppressLint; import android.app.Activity; import android.content.Context; import android.content.Intent; import android.graphics.BitmapFactory; import android.media.projection.MediaProjectionManager; import android.os.Bundle; import android.provider.Settings; import android.view.MotionEvent; import android.widget.*; import androidx.activity.result.contract.ActivityResultContracts; import androidx.appcompat.app.AppCompatActivity; import org.json.JSONObject
class MainActivity : AppCompatActivity() {
    private var downX = 0f; private var downY = 0f
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) startForegroundService(Intent(this, ScreenCaptureService::class.java).apply { putExtra("code", res.resultCode); putExtra("data", res.data) })
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        val status = findViewById<TextView>(R.id.statusText); val pcScreen = findViewById<ImageView>(R.id.pcScreen)
        val relay = findViewById<EditText>(R.id.relayInput); val room = findViewById<EditText>(R.id.roomInput); val token = findViewById<EditText>(R.id.tokenInput); val pcText = findViewById<EditText>(R.id.pcText)
        WsManager.onStatus = { s -> runOnUiThread { status.text = "Статус: $s" } }
        WsManager.onPhoneStream = { en -> ScreenCaptureService.isStreaming = en }
        WsManager.onPcFrame = { jpg -> val bmp = BitmapFactory.decodeByteArray(jpg, 0, jpg.size); runOnUiThread { pcScreen.setImageBitmap(bmp) } }
        findViewById<Button>(R.id.btnConnect).setOnClickListener { WsManager.connect(relay.text.toString().trim(), room.text.toString().trim(), token.text.toString().trim()) }
        findViewById<Button>(R.id.btnAcc).setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        findViewById<Button>(R.id.btnShare).setOnClickListener { launcher.launch((getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()) }
        findViewById<Button>(R.id.btnPcOn).setOnClickListener { WsManager.sendJson(JSONObject().put("target", "pc").put("type", "pc_stream").put("enabled", true)) }
        findViewById<Button>(R.id.btnPcOff).setOnClickListener { WsManager.sendJson(JSONObject().put("target", "pc").put("type", "pc_stream").put("enabled", false)) }
        findViewById<Button>(R.id.btnSendText).setOnClickListener {
            val t = pcText.text.toString(); if (t.isNotEmpty()) { WsManager.sendJson(JSONObject().put("target", "pc").put("type", "keyboard").put("action", "type").put("text", t)); pcText.setText("") }
        }
        pcScreen.setOnTouchListener { v, ev ->
            val nx = (ev.x / v.width.coerceAtLeast(1)).coerceIn(0f, 1f); val ny = (ev.y / v.height.coerceAtLeast(1)).coerceIn(0f, 1f)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = ev.x; downY = ev.y }
                MotionEvent.ACTION_MOVE -> WsManager.sendJson(JSONObject().put("target", "pc").put("type", "mouse").put("action", "move").put("nx", nx).put("ny", ny))
                MotionEvent.ACTION_UP -> if (kotlin.math.hypot(ev.x - downX, ev.y - downY) < 15f) WsManager.sendJson(JSONObject().put("target", "pc").put("type", "mouse").put("action", "click").put("nx", nx).put("ny", ny))
            }
            true
        }
    }
}
