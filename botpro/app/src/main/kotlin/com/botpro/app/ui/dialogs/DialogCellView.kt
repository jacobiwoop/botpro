package com.botpro.app.ui.dialogs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import com.botpro.app.R
import com.botpro.app.core.utils.AndroidUtilities
import com.botpro.app.core.utils.TypefaceManager
import com.botpro.app.data.model.Conversation
import com.botpro.app.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Vue unitaire représentant une conversation dans la liste de dialogues,
 * reproduisant fidèlement le DialogCell de Telegram.
 *
 * Contraintes architecturales strictes (SPEC_PORTAGE_DIALOGS.md) :
 * - Aucune vue enfant : hérite directement de View, tout est peint dans onDraw().
 * - Hauteur : 70dp + 1px physique de séparateur (= 194px @440dpi).
 * - Aucune allocation d'objet dans onDraw().
 * - Tailles de texte impérativement en dp.
 */
class DialogCellView(context: Context) : View(context) {

    companion object {
        private const val AVATAR_START = 11f
        private const val AVATAR_TOP = 9f
        private const val AVATAR_SIZE = 52f
        private const val MESSAGE_PADDING_START = 76f // 72 + 4
        private const val NAME_TOP = 14f
        private const val MESSAGE_TOP = 39f
        private const val TIME_TOP = 16f
        private const val COUNT_TOP = 38f
        private const val BADGE_SIZE = 20.666f
        private const val BADGE_TEXT_PADDING = 6.333f
        private const val BADGE_MARGIN = 15.666f
    }

    // Modèle de données lié
    var currentConversation: Conversation? = null
        private set

    // Paints pré-alloués
    private val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avatarTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.AVATAR_TEXT
        typeface = TypefaceManager.getMedium(context)
        textAlign = Paint.Align.CENTER
    }

    private val onlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHATS_ONLINE_CIRCLE
    }
    private val onlineStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.WINDOW_BACKGROUND_WHITE
    }

    private val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHATS_NAME
        typeface = TypefaceManager.getMedium(context)
    }

    private val messagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHATS_MESSAGE
        typeface = android.graphics.Typeface.DEFAULT
    }

    private val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHATS_DATE
        typeface = android.graphics.Typeface.DEFAULT
    }

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHATS_UNREAD_COUNTER_TEXT
        typeface = TypefaceManager.getMedium(context)
        textAlign = Paint.Align.CENTER
    }

    private val dividerPaint = Paint().apply {
        color = ThemeColors.DIVIDER
        strokeWidth = 1f
    }

    // Rectangles et calculs mis en cache
    private val badgeRect = RectF()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    private val todayCalendar = Calendar.getInstance()
    private val msgCalendar = Calendar.getInstance()

    // Drawables d'état
    private val pinDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.list_pin)?.apply {
            setTint(ThemeColors.CHATS_PINNED_ICON)
        }
    }
    private val muteDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.list_mute)?.apply {
            setTint(ThemeColors.CHATS_MUTE_ICON)
        }
    }
    private val checkDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.list_check)?.apply {
            setTint(ThemeColors.CHATS_SENT_READ_CHECK)
        }
    }
    private val halfCheckDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.list_halfcheck)?.apply {
            setTint(ThemeColors.CHATS_SENT_CHECK)
        }
    }

    // Layouts de texte pré-calculés
    private var nameLayout: StaticLayout? = null
    private var messageLayout: StaticLayout? = null
    private var timeText: String = ""
    private var timeWidth: Float = 0f
    private var timeLeft: Float = 0f
    private var badgeWidth: Float = 0f
    private var badgeLeft: Float = 0f
    private var countText: String = ""
    private var avatarLetter: String = ""
    private var avatarColor: Int = ThemeColors.AVATAR_BACKGROUND_BLUE

    init {
        // Initialiser les tailles de texte en dp
        updateTextSizes()

        // Configurer le feedback tactile Material / Ripple
        val rippleColor = ColorStateList.valueOf(ThemeColors.LIST_SELECTOR_SDK21)
        val mask = ColorDrawable(Color.WHITE)
        background = RippleDrawable(rippleColor, null, mask)
        isClickable = true
        isFocusable = true
    }

    private fun updateTextSizes() {
        avatarTextPaint.textSize = AndroidUtilities.dp(22f).toFloat()
        namePaint.textSize = AndroidUtilities.dp(17f).toFloat()
        messagePaint.textSize = AndroidUtilities.dp(16f).toFloat()
        timePaint.textSize = AndroidUtilities.dp(12f).toFloat()
        badgeTextPaint.textSize = AndroidUtilities.dp(12f).toFloat()
    }

    fun setConversation(conversation: Conversation) {
        currentConversation = conversation
        val bot = conversation.bot

        avatarColor = ThemeColors.getAvatarColor(bot.id)
        avatarPaint.color = avatarColor
        avatarLetter = bot.name.trim().take(1).uppercase(Locale.getDefault())

        // Formatage de la date
        conversation.lastMessage?.let { msg ->
            timeText = formatTimestamp(msg.timestamp)
            timeWidth = timePaint.measureText(timeText)
        } ?: run {
            timeText = ""
            timeWidth = 0f
        }

        // Configuration badge non-lu
        if (conversation.unreadCount > 0) {
            countText = if (conversation.unreadCount > 999) "999+" else conversation.unreadCount.toString()
            val textW = badgeTextPaint.measureText(countText)
            val minWidth = AndroidUtilities.dp(BADGE_SIZE).toFloat()
            val contentWidth = textW + AndroidUtilities.dp(BADGE_TEXT_PADDING * 2f).toFloat()
            badgeWidth = kotlin.math.max(minWidth, contentWidth)
            badgePaint.color = if (conversation.isMuted) {
                ThemeColors.CHATS_UNREAD_COUNTER_MUTED
            } else {
                ThemeColors.CHATS_UNREAD_COUNTER
            }
        } else {
            countText = ""
            badgeWidth = 0f
        }

        buildLayouts(measuredWidth)
        invalidate()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        todayCalendar.timeInMillis = now
        msgCalendar.timeInMillis = timestamp

        return if (todayCalendar.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR) &&
            todayCalendar.get(Calendar.DAY_OF_YEAR) == msgCalendar.get(Calendar.DAY_OF_YEAR)
        ) {
            timeFormat.format(Date(timestamp))
        } else {
            dateFormat.format(Date(timestamp))
        }
    }

    private fun buildLayouts(width: Int) {
        if (width <= 0) return
        val conversation = currentConversation ?: return

        val textStart = AndroidUtilities.dp(MESSAGE_PADDING_START)
        val rightMargin = AndroidUtilities.dp(BADGE_MARGIN).toFloat()

        // 1. Position Heure
        timeLeft = width - rightMargin - timeWidth

        // 2. Position Badge
        if (conversation.unreadCount > 0) {
            badgeLeft = width - rightMargin - badgeWidth
            val countTop = AndroidUtilities.dp(COUNT_TOP).toFloat()
            badgeRect.set(
                badgeLeft,
                countTop,
                badgeLeft + badgeWidth,
                countTop + AndroidUtilities.dp(BADGE_SIZE).toFloat()
            )
        }

        // 3. Layout du Nom
        val nameAvailWidth = (timeLeft - textStart - AndroidUtilities.dp(8f)).coerceAtLeast(0f).toInt()
        val botName = conversation.bot.name
        val ellipsizedName = TextUtils.ellipsize(botName, namePaint, nameAvailWidth.toFloat(), TextUtils.TruncateAt.END)
        nameLayout = StaticLayout(
            ellipsizedName,
            namePaint,
            nameAvailWidth,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            false
        )

        // 4. Layout de l'aperçu du Message
        val messageRightLimit = if (conversation.unreadCount > 0) {
            badgeLeft - AndroidUtilities.dp(8f)
        } else if (conversation.isPinned) {
            width - rightMargin - AndroidUtilities.dp(20f)
        } else {
            width - rightMargin
        }
        val messageAvailWidth = (messageRightLimit - textStart).coerceAtLeast(0f).toInt()
        val rawMessage = conversation.lastMessage?.text?.replace('\n', ' ') ?: ""
        val ellipsizedMessage = TextUtils.ellipsize(rawMessage, messagePaint, messageAvailWidth.toFloat(), TextUtils.TruncateAt.END)
        messageLayout = StaticLayout(
            ellipsizedMessage,
            messagePaint,
            messageAvailWidth,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            false
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // 70dp + 1px séparateur physique = 194px à 440dpi
        val height = AndroidUtilities.dp(70f) + 1
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw && w > 0) {
            buildLayouts(w)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val conversation = currentConversation ?: return
        val bot = conversation.bot

        val width = measuredWidth
        val height = measuredHeight

        // ── 1. Avatar ──────────────────────────────────────────────────────────
        val avatarLeft = AndroidUtilities.dp(AVATAR_START).toFloat()
        val avatarTop = AndroidUtilities.dp(AVATAR_TOP).toFloat()
        val avatarDiameter = AndroidUtilities.dp(AVATAR_SIZE).toFloat()
        val avatarRadius = avatarDiameter / 2f
        val avatarCx = avatarLeft + avatarRadius
        val avatarCy = avatarTop + avatarRadius

        canvas.drawCircle(avatarCx, avatarCy, avatarRadius, avatarPaint)

        // Initiale au centre
        if (avatarLetter.isNotEmpty()) {
            val textY = avatarCy - (avatarTextPaint.descent() + avatarTextPaint.ascent()) / 2f
            canvas.drawText(avatarLetter, avatarCx, textY, avatarTextPaint)
        }

        // Indicateur en ligne (pastille verte)
        if (bot.isOnline) {
            val onlineRadius = AndroidUtilities.dp(4.5f).toFloat()
            val onlineCx = avatarCx + AndroidUtilities.dp(17f).toFloat()
            val onlineCy = avatarCy + AndroidUtilities.dp(17f).toFloat()
            canvas.drawCircle(onlineCx, onlineCy, onlineRadius + AndroidUtilities.dp(1.5f).toFloat(), onlineStrokePaint)
            canvas.drawCircle(onlineCx, onlineCy, onlineRadius, onlinePaint)
        }

        // ── 2. Nom du bot ──────────────────────────────────────────────────────
        val textLeft = AndroidUtilities.dp(MESSAGE_PADDING_START).toFloat()
        nameLayout?.let { layout ->
            canvas.save()
            canvas.translate(textLeft, AndroidUtilities.dp(NAME_TOP).toFloat())
            layout.draw(canvas)
            canvas.restore()
        }

        // ── 3. Heure du dernier message ────────────────────────────────────────
        if (timeText.isNotEmpty()) {
            val timeY = AndroidUtilities.dp(TIME_TOP).toFloat() - timePaint.ascent()
            canvas.drawText(timeText, timeLeft, timeY, timePaint)
        }

        // ── 4. Aperçu du message ───────────────────────────────────────────────
        messageLayout?.let { layout ->
            canvas.save()
            canvas.translate(textLeft, AndroidUtilities.dp(MESSAGE_TOP).toFloat())
            layout.draw(canvas)
            canvas.restore()
        }

        // ── 5. Badge non-lu ou Icône épinglé ────────────────────────────────────
        if (conversation.unreadCount > 0) {
            val radius = AndroidUtilities.dp(11.5f).toFloat()
            canvas.drawRoundRect(badgeRect, radius, radius, badgePaint)
            val badgeTextY = badgeRect.centerY() - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
            canvas.drawText(countText, badgeRect.centerX(), badgeTextY, badgeTextPaint)
        } else if (conversation.isPinned) {
            pinDrawable?.let { pin ->
                val pinW = pin.intrinsicWidth
                val pinH = pin.intrinsicHeight
                val pinRight = (width - AndroidUtilities.dp(BADGE_MARGIN)).toInt()
                val pinTop = AndroidUtilities.dp(COUNT_TOP)
                pin.setBounds(pinRight - pinW, pinTop, pinRight, pinTop + pinH)
                pin.draw(canvas)
            }
        }

        // ── 6. Séparateur ──────────────────────────────────────────────────────
        // 1 pixel physique au bas de la cellule, indenté au bord gauche du texte
        val lineY = (height - 1).toFloat()
        canvas.drawLine(textLeft, lineY, width.toFloat(), lineY, dividerPaint)
    }
}
