package cool.mixi.dica.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.IndexActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.util.ApiService
import cool.mixi.dica.util.dLog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class NotificationJonService: JobService() {
    companion object {
        val jobId = 1
    }

    private val notificationId = 1
    override fun onStopJob(params: JobParameters?): Boolean {
        dLog("onStopJob")
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        dLog("onStartJob")
        doAsync {
            val response = ApiService.create().friendicaNotifications().execute()
            response.body()?.let {
                App.instance.addNotification(it)
            }
            uiThread {
                dLog("unread notification ${App.instance.getUnSeenNotificationCount()} ${App.instance.notifications.size}")
                val appName = getString(R.string.app_name)
                val strNotify = getString(R.string.notifications)
                val notificationChannelId = "$appName $strNotify"
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createChannel(notificationChannelId, notificationManager)

                val size = App.instance.getUnSeenNotificationCount()
                if(size == 0) {
                    notificationManager.cancel(notificationId)
                    jobFinished(params, false)
                    return@uiThread
                }

                val resultIntent = Intent(applicationContext, IndexActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    this.putExtra(Consts.EXTRA_NOTIFICATIONS, true)
                }

                val pendingIntent = PendingIntent.getActivity(applicationContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                val message = getString(R.string.new_notification_count).format("$size")
                val notification = NotificationCompat.Builder(App.instance.applicationContext)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher))
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setChannelId(notificationChannelId)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(notificationId, notification)
                jobFinished(params, false)
            }
        }
        return true
    }

    private fun createChannel(channelId: String, mgr: NotificationManager){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){ return; }
        val channel = NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
        mgr.createNotificationChannel(channel)
    }
}