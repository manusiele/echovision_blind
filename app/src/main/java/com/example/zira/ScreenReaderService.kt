package com.example.zira

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.*

class ScreenReaderService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    private enum class AutomationState {
        IDLE,
        OPENING_WHATSAPP,
        OPENING_CHAT,
        READING_MESSAGE
    }
    private var currentState: AutomationState = AutomationState.IDLE

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_OPEN_CHAT") {
            val contactName = intent.getStringExtra("contact_name")
            if (contactName != null) {
                findAndClickContact(contactName)
            }
        } else if (intent?.action == "ACTION_READ_LAST_MESSAGE") {
            findAndReadLastMessage()
        } else if (intent?.action == "ACTION_RESPOND_TO_MESSAGE") {
            val message = intent.getStringExtra("message")
            if (message != null) {
                respondToMessage(message)
            }
        } else if (intent?.action == "ACTION_READ_LATEST_WHATSAPP_MESSAGE") {
            currentState = AutomationState.OPENING_WHATSAPP
            openWhatsAppFromService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source
                source?.let {
                    val text = it.text ?: it.contentDescription
                    if (!text.isNullOrEmpty()) {
                        speak(text.toString())
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d("ScreenReaderService", "Window State Changed. Current State: $currentState")
                val eventText = event.text.joinToString()
                if (eventText.isNotEmpty()) {
                    speak(eventText)
                }

                when (currentState) {
                    AutomationState.OPENING_WHATSAPP -> {
                        if (event.packageName == "com.whatsapp") {
                            speak("WhatsApp opened. Looking for latest chat.")
                            currentState = AutomationState.OPENING_CHAT
                            findAndClickFirstChat()
                        } else {
                            speak("Failed to open WhatsApp. Please ensure WhatsApp is installed.")
                            currentState = AutomationState.IDLE
                        }
                    }
                    AutomationState.OPENING_CHAT -> {
                        if (event.packageName == "com.whatsapp" && isChatScreen(event.className.toString())) {
                            speak("Chat opened. Reading last message.")
                            currentState = AutomationState.READING_MESSAGE
                            findAndReadLastMessage()
                            currentState = AutomationState.IDLE
                        } else if (event.packageName == "com.whatsapp") {
                            speak("Could not open chat screen. Please ensure you are in the chats list.")
                            currentState = AutomationState.IDLE
                        }
                    }
                    AutomationState.READING_MESSAGE,
                    AutomationState.IDLE -> {
                        // Do nothing
                    }
                }
            }
            AccessibilityEvent.TYPE_ANNOUNCEMENT,
            AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT -> {
                // Handle these event types explicitly
                Log.d("ScreenReaderService", "Announcement or Assist event received")
            }
        }
    }

    private fun isChatScreen(className: String): Boolean {
        return className.contains("Conversation") || className.contains("Chat")
    }

    private fun openWhatsAppFromService() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            speak("WhatsApp is not installed.")
            currentState = AutomationState.IDLE
        }
    }

    private fun findAndClickContact(contactName: String) {
        val rootNode = rootInActiveWindow ?: run {
            speak("Could not find contact.")
            return
        }

        val contactNode = findNodeByText(rootNode, contactName)
        contactNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        if (contactNode == null) {
            speak("Could not find contact $contactName")
        }
    }

    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes.find { node ->
            node.text?.toString().equals(text, ignoreCase = true) ||
                    node.contentDescription?.toString().equals(text, ignoreCase = true)
        }
    }

    private fun findAndClickFirstChat() {
        val rootNode = rootInActiveWindow ?: run {
            speak("Could not find chats list.")
            currentState = AutomationState.IDLE
            return
        }

        // Try to find a chat item with unread messages first
        val unreadChatItems = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/unread_badge_text")
        if (!unreadChatItems.isNullOrEmpty()) {
            unreadChatItems[0].parent?.let { chatItem ->
                chatItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }

        // Fallback: Find the first visible chat item in the scrollable list
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode != null) {
            for (i in 0 until scrollableNode.childCount) {
                val child = scrollableNode.getChild(i) ?: continue
                if (child.isClickable && child.viewIdResourceName?.contains("conversations_row_container") == true) {
                    child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }

        // Final fallback
        val firstChatItem = findFirstClickableChild(rootNode)
        if (firstChatItem != null) {
            firstChatItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            speak("Could not find any chat to open.")
            currentState = AutomationState.IDLE
        }
    }

    private fun findScrollableNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (rootNode.isScrollable) {
            return rootNode
        }
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val scrollableChild = findScrollableNode(child)
            if (scrollableChild != null) {
                return scrollableChild
            }
        }
        return null
    }

    private fun findFirstClickableChild(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if the current node is clickable
        if (node.isClickable) {
            return node
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val clickableChild = findFirstClickableChild(child)
                if (clickableChild != null) {
                    return clickableChild
                }
            }
        }

        return null
    }

    private fun findFirstClickableChildOfScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && child.isClickable) {
                    return child
                }
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val clickableChild = findFirstClickableChildOfScrollable(child)
                if (clickableChild != null) {
                    return clickableChild
                }
            }
        }
        Log.d("ScreenReaderService", "No clickable item found in scrollable list.")
        return null
    }

    private fun findAndReadLastMessage() {
        val rootNode = rootInActiveWindow ?: run {
            speak("Could not read the last message.")
            return
        }

        val lastMessageNode = findLastMessageNode(rootNode)

        if (lastMessageNode != null) {
            findSenderName(lastMessageNode)?.let { senderNode ->
                speak("Message from ${senderNode.text}. ")
            }

            val messageText = lastMessageNode.text ?: lastMessageNode.contentDescription
            if (!messageText.isNullOrEmpty()) {
                speak(messageText.toString())
            } else {
                speak("Could not read the last message.")
            }
        } else {
            speak("Could not find the last message.")
        }
    }

    private fun findSenderName(messageNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val parent = messageNode.parent ?: return null

        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            if (child != messageNode && !child.text.isNullOrEmpty()) {
                return child
            }
        }

        Log.d("ScreenReaderService", "Could not find sender's name.")
        return null
    }

    private fun findLastMessageNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val leafNodes = mutableListOf<AccessibilityNodeInfo>()
        findLeafNodes(rootNode, leafNodes)

        for (i in leafNodes.size - 1 downTo 0) {
            val node = leafNodes[i]
            if (!node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty()) {
                return node
            }
        }

        return null
    }

    private fun findLeafNodes(node: AccessibilityNodeInfo, leafNodes: MutableList<AccessibilityNodeInfo>) {
        if (node.childCount == 0) {
            leafNodes.add(node)
        } else {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    findLeafNodes(child, leafNodes)
                }
            }
        }
    }

    private fun respondToMessage(message: String) {
        val rootNode = rootInActiveWindow ?: run {
            speak("Could not send message.")
            return
        }

        val inputField = findEditableTextField(rootNode) ?: run {
            speak("Could not find input field.")
            return
        }

        // Type the message
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
        inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        // Find and click send button
        val sendButton = findSendButton(rootNode)
        if (sendButton != null) {
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            speak("Could not find send button.")
        }
    }

    private fun findEditableTextField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText("")
        return nodes.find { node ->
            node.className == "android.widget.EditText" && node.isEditable
        }
    }

    private fun findSendButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText("send")
        return nodes.find { node ->
            node.isClickable && (node.text?.toString().equals("send", ignoreCase = true) ||
                    node.contentDescription?.toString().equals("send", ignoreCase = true))
        }
    }

    override fun onInterrupt() {
        Log.d("ScreenReaderService", "onInterrupt")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            Log.d("ScreenReaderService", "TTS initialized successfully")
        } else {
            Log.e("ScreenReaderService", "TTS initialization failed")
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}