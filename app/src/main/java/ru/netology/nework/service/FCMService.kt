package ru.netology.nework.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {
    @Inject
    lateinit var auth: AppAuth
    private val action = "action"
    private val content = "content"
    private val channelId = "remote"
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        auth.sendPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notificationMessage = message.data[action]
        val push = gson.fromJson(message.data[content], Push::class.java)
        val userId = auth.authStateFlow.value.id

        when (push.recipientId) {
            userId -> handlePush(push)
            null -> handlePush(push)
        }

        when  {
            push.recipientId == 0L && push.recipientId != userId -> auth.sendPushToken()
            push.recipientId != 0L && push.recipientId != userId -> auth.sendPushToken()
        }

        Action.entries.forEach{
            if (it.toString() == notificationMessage){
                message.data[action]?.let {
                    when (Action.valueOf(it)) {
                        Action.LIKE -> handleLike(gson.fromJson(message.data[content], Like::class.java))
                        Action.NEW_POST -> handleNewPost(gson.fromJson(message.data[content],
                            NewPost::class.java))
                    }
                }
            }
        }
    }

    private fun handlePush(push: Push) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.push_successfully,
                    push.content
                )
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.push_text)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun handleLike(content: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_user_liked,
                    content.userName,
                    content.postAuthor,
                )
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_creating_new_post)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun handleNewPost(content: NewPost) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_creating_new_post,
                    content.userName,
                    content.postName
                )
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_text)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun notify(notification: Notification) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(Random.nextInt(100_000), notification)
        }
    }
}

enum class Action {
    LIKE,
    NEW_POST
}

data class Like(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String
)

data class NewPost(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postName: String
)

data class Push(
    val recipientId: Long?,
    val content: String
)