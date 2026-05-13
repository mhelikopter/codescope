package de.thkoeln.codescope.logic

class NotificationServiceImpl : INotificationService {
    override fun showNotification(title: String, message: String) {
        // Desktop Benachrichtigungen (einfaches Print für jetzt oder System-Tray)
        println("DESKTOP NOTIFICATION: $title - $message")
    }
}
