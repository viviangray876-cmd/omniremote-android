package com.omniremote.app
import android.app.*; import android.content.Context; import android.content.Intent; import android.content.pm.ServiceInfo; import android.graphics.Bitmap; import android.graphics.PixelFormat; import android.hardware.display.DisplayManager; import android.hardware.display.VirtualDisplay; import android.media.ImageReader; import android.media.projection.MediaProjection; import android.media.projection.MediaProjectionManager; import android.os.Build; import android.os.Handler; import android.os.HandlerThread; import android.os.IBinder; import androidx.core.app.NotificationCompat; import java.io.ByteArrayOutputStream
class ScreenCaptureService : Service() {
    private var mp: MediaProjection? = null; private var vd: VirtualDisplay? = null; private var ir: ImageReader? = null; private var lastFrame = 0L
    companion object { var isStreaming = false }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("omni", "OmniRemote", NotificationManager.IMPORTANCE_LOW))
        val notif = NotificationCompat.Builder(this, "omni").setContentTitle("OmniRemote").setContentText("Трансляция экрана").setSmallIcon(android.R.drawable.ic_menu_camera).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(101, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(101, notif)
        val code = intent?.getIntExtra("code", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION") val data: Intent? = intent?.getParcelableExtra("data")
        if (code == Activity.RESULT_OK && data != null) {
            mp = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code, data)
            val dm = resources.displayMetrics; val w = (dm.widthPixels / 2).coerceAtLeast(360); val h = (dm.heightPixels / 2).coerceAtLeast(640)
            val th = HandlerThread("Cap").apply { start() }; val handler = Handler(th.looper)
            ir = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            ir?.setOnImageAvailableListener({ reader ->
                val now = System.currentTimeMillis()
                if (!isStreaming || now - lastFrame < 120) { reader.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
                lastFrame = now; val img = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val pl = img.planes[0]; val pad = pl.rowStride - pl.pixelStride * w
                    val bmp = Bitmap.createBitmap(w + pad / pl.pixelStride, h, Bitmap.Config.ARGB_8888); bmp.copyPixelsFromBuffer(pl.buffer)
                    val crop = Bitmap.createBitmap(bmp, 0, 0, w, h); val out = ByteArrayOutputStream(); crop.compress(Bitmap.CompressFormat.JPEG, 55, out)
                    WsManager.sendPhoneFrame(out.toByteArray()); bmp.recycle(); crop.recycle()
                } finally { img.close() }
            }, handler)
            vd = mp?.createVirtualDisplay("Omni", w, h, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, ir?.surface, null, handler)
            isStreaming = true
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() { isStreaming = false; vd?.release(); ir?.close(); mp?.stop(); super.onDestroy() }
}
