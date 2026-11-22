package com.omniremote.app
import okhttp3.*; import okio.ByteString; import okio.ByteString.Companion.toByteString; import org.json.JSONObject; import java.net.URLEncoder; import java.util.concurrent.TimeUnit
object WsManager {
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    var onStatus: ((String) -> Unit)? = null
    var onPcFrame: ((ByteArray) -> Unit)? = null
    var onPhoneStream: ((Boolean) -> Unit)? = null
    fun connect(url: String, room: String, token: String) {
        ws?.close(1000, "Reconnect")
        val full = "$url?room=${URLEncoder.encode(room, "UTF-8")}&role=phone&token=${URLEncoder.encode(token, "UTF-8")}"
        ws = client.newWebSocket(Request.Builder().url(full).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { onStatus?.invoke("Подключено") }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val j = JSONObject(text)
                    when (j.optString("type")) {
                        "phone_stream" -> onPhoneStream?.invoke(j.optBoolean("enabled", false))
                        "phone_touch" -> {
                            if (j.optString("action") == "tap") RemoteAccessibilityService.instance?.tap(j.optDouble("nx", 0.5).toFloat(), j.optDouble("ny", 0.5).toFloat())
                            else if (j.optString("action") == "swipe") RemoteAccessibilityService.instance?.swipe(j.optDouble("nx1", 0.5).toFloat(), j.optDouble("ny1", 0.7).toFloat(), j.optDouble("nx2", 0.5).toFloat(), j.optDouble("ny2", 0.3).toFloat(), j.optLong("duration", 250L))
                        }
                        "phone_nav" -> RemoteAccessibilityService.instance?.performNav(j.optString("action"))
                        "phone_text" -> RemoteAccessibilityService.instance?.inputText(j.optString("text"))
                    }
                } catch (_: Exception) {}
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val arr = bytes.toByteArray()
                if (arr.size > 1 && arr[0] == 0x01.toByte()) onPcFrame?.invoke(arr.copyOfRange(1, arr.size))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { onStatus?.invoke("Ошибка: ${t.message}") }
        })
    }
    fun sendJson(obj: JSONObject) { ws?.send(obj.toString()) }
    fun sendPhoneFrame(jpeg: ByteArray) {
        val out = ByteArray(jpeg.size + 1); out[0] = 0x02; System.arraycopy(jpeg, 0, out, 1, jpeg.size); ws?.send(out.toByteString())
    }
}
