package com.botpro.app.ui.chat

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import com.botpro.app.core.utils.AndroidUtilities
import com.botpro.app.core.utils.AndroidUtilities.dp
import com.botpro.app.core.utils.AndroidUtilities.dpf2
import com.botpro.app.core.utils.TypefaceManager
import com.botpro.app.data.model.InlineButton
import com.botpro.app.data.model.Message
import com.botpro.app.data.model.MessageType
import com.botpro.app.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Cellule de message unitaire pour la conversation BotPro,
 * reproduisant à 100% le ChatMessageCell de Telegram.
 *
 * Contraintes architecturales strictes (MISSION_IMPLEMENTATION_CONVERSATION.md) :
 * - Vue unique héritant directement de View, 0 vue enfant.
 * - Tout est peint au Canvas dans onDraw().
 * - Dégradé sortant ancré à l'écran (Matrix.postTranslate(0, -topY)).
 * - Boutons inline de bot peints au Canvas avec détection de clic.
 * - Aucune allocation d'objet dans onDraw().
 */
class MessageCellView(context: Context) : View(context) {

    companion object {
        private const val BUBBLE_RADIUS_DP = 17f
        private const val BUBBLE_NEAR_RADIUS_DP = 5f
        private const val BUTTON_INNER_RADIUS_DP = 6.75f
        private const val TAIL_WIDTH_DP = 15f
        private const val BUTTON_HEIGHT_DP = 44f
        private const val BUTTON_ROW_GAP_DP = 4f
        private const val BUTTON_COL_GAP_DP = 5f
        private const val DATE_CELL_HEIGHT_DP = 30.9f
        private const val MARGIN_SCREEN_EDGE_DP = 11.27f // 31px @ 440dpi
        private const val MAX_BUBBLE_WIDTH_DP = 333.8f // 918px @ 440dpi
        private const val QUOTE_BAR_WIDTH_DP = 3.3f // 9px @ 440dpi
    }

    var currentMessage: Message? = null
        private set

    var onInlineButtonClicked: ((InlineButton) -> Unit)? = null

    // Peintures pré-allouées
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbleSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_BUBBLE_SELECTED
    }

    private val textPaintIn = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_MESSAGE_TEXT_IN
        textSize = dpf2(16f)
        typeface = Typeface.DEFAULT
    }

    private val textPaintOut = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_MESSAGE_TEXT_OUT
        textSize = dpf2(16f)
        typeface = Typeface.DEFAULT
    }

    private val linkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_LINKS
        textSize = dpf2(16f)
        typeface = Typeface.DEFAULT
    }

    private val timePaintIn = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_TIME_TEXT
        textSize = dpf2(12f)
        typeface = Typeface.DEFAULT
    }

    private val timePaintOut = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_OUT_TIME_TEXT
        textSize = dpf2(12f)
        typeface = Typeface.DEFAULT
    }

    private val readCheckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_OUT_SENT_CHECK
        style = Paint.Style.STROKE
        strokeWidth = dpf2(1.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dateServiceBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_SERVICE_BACKGROUND
    }

    private val dateServiceTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_SERVICE_TEXT
        textSize = dpf2(13f)
        typeface = TypefaceManager.getMedium(context)
        textAlign = Paint.Align.CENTER
    }

    private val quoteBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_REPLY_LINE
    }

    private val quoteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_REPLY_BACKGROUND
    }

    private val quoteNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_REPLY_NAME
        textSize = dpf2(13f)
        typeface = TypefaceManager.getMedium(context)
    }

    private val quoteTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dpf2(13f)
        typeface = Typeface.DEFAULT
    }

    private val forwardHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_LINKS
        textSize = dpf2(13f)
        typeface = TypefaceManager.getMedium(context)
    }

    private val docBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2C3E50.toInt()
    }

    private val docIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dpf2(2f)
        strokeCap = Paint.Cap.ROUND
    }

    private val docTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dpf2(15f)
        typeface = TypefaceManager.getMedium(context)
    }

    private val docSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.CHAT_IN_TIME_TEXT
        textSize = dpf2(12.5f)
        typeface = Typeface.DEFAULT
    }

    // Peintures boutons inline
    private val botButtonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.BOT_INLINE_BUTTON_BACKGROUND
    }

    private val botButtonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColors.BOT_INLINE_BUTTON_TEXT
        textSize = dpf2(14f)
        typeface = TypefaceManager.getMedium(context)
        textAlign = Paint.Align.CENTER
    }

    // Gradient sortant et Matrix
    private var outgoingShader: LinearGradient? = null
    private val shaderMatrix = Matrix()
    private var lastScreenHeight: Int = 0

    // Rectangles et chemins réutilisés pour éviter toute allocation
    private val bubbleRect = RectF()
    private val bubblePath = Path()
    private val quoteRect = RectF()
    private val tempRect = RectF()
    private val radii = FloatArray(8)

    // Layouts de texte calculés
    private var messageLayout: StaticLayout? = null
    private var forwardLayout: StaticLayout? = null
    private var quoteTextLayout: StaticLayout? = null
    private var timeText: String = ""
    private var timeWidth: Float = 0f

    // Boutons inline géométrie
    private class RenderButton(
        val button: InlineButton,
        val rect: RectF,
        val isBottomLeft: Boolean,
        val isBottomRight: Boolean
    )
    private val renderButtons = mutableListOf<RenderButton>()

    // Format d'heure
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun bind(message: Message) {
        currentMessage = message
        timeText = if (message.timestamp > 0) timeFormatter.format(Date(message.timestamp)) else ""
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val msg = currentMessage

        if (msg == null) {
            setMeasuredDimension(totalWidth, 0)
            return
        }

        // Cas 1 : Séparateur de date
        if (msg.isDateSeparator || msg.type == MessageType.DATE_SEPARATOR) {
            val h = dp(DATE_CELL_HEIGHT_DP)
            setMeasuredDimension(totalWidth, h)
            return
        }

        val maxBubbleW = min(dp(MAX_BUBBLE_WIDTH_DP), (totalWidth * 0.85f).toInt())
        val innerPaddingH = dp(11f)
        val textMaxW = maxBubbleW - (innerPaddingH * 2)

        var contentH = dp(7f) // padding haut bulle

        // 1. En-tête de transfert
        forwardLayout = null
        if (msg.forwardInfo != null) {
            val fwdText = "Transféré de " + msg.forwardInfo.authorName
            forwardLayout = StaticLayout.Builder.obtain(fwdText, 0, fwdText.length, forwardHeaderPaint, textMaxW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
            contentH += forwardLayout!!.height + dp(4f)
        }

        // 2. Bloc de citation
        quoteTextLayout = null
        if (msg.replyInfo != null) {
            val qText = msg.replyInfo.text
            val quoteInnerMaxW = textMaxW - dp(12f) - dp(QUOTE_BAR_WIDTH_DP)
            quoteTextLayout = StaticLayout.Builder.obtain(qText, 0, qText.length, quoteTextPaint, quoteInnerMaxW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            val quoteH = dp(18f) + quoteTextLayout!!.height + dp(4f)
            contentH += quoteH + dp(4f)
        }

        // 3. Document
        if (msg.type == MessageType.DOCUMENT && msg.documentInfo != null) {
            contentH += dp(48f)
        }

        // 4. Texte du message
        val pText = if (msg.isOutgoing) textPaintOut else textPaintIn
        val bodyText = msg.text
        if (bodyText.isNotEmpty()) {
            messageLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, pText, textMaxW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
            contentH += messageLayout!!.height
        } else {
            messageLayout = null
        }

        // Heure et accusé de lecture
        val timeP = if (msg.isOutgoing) timePaintOut else timePaintIn
        timeWidth = timeP.measureText(timeText)
        if (msg.isOutgoing) {
            timeWidth += dp(16f) // place pour la double coche
        }

        contentH += dp(11f) // padding bas avant boutons

        // 5. Boutons inline de bot (Phase A1)
        renderButtons.clear()
        var buttonsHeight = 0
        val markup = msg.replyMarkup
        if (markup != null && markup.rows.isNotEmpty()) {
            val rowCount = markup.rows.size
            val btnH = dp(BUTTON_HEIGHT_DP)
            val rowGap = dp(BUTTON_ROW_GAP_DP)
            buttonsHeight = rowCount * btnH + (rowCount - 1) * rowGap + dp(6f)
        }

        // Calcul de la largeur finale de la bulle
        var measuredContentW = 0f
        if (messageLayout != null) {
            for (i in 0 until messageLayout!!.lineCount) {
                measuredContentW = max(measuredContentW, messageLayout!!.getLineWidth(i))
            }
        }
        if (forwardLayout != null) {
            for (i in 0 until forwardLayout!!.lineCount) {
                measuredContentW = max(measuredContentW, forwardLayout!!.getLineWidth(i))
            }
        }
        if (quoteTextLayout != null) {
            measuredContentW = max(measuredContentW, quoteTextLayout!!.getLineWidth(0) + dp(24f))
        }
        if (msg.type == MessageType.DOCUMENT) {
            measuredContentW = max(measuredContentW, dp(180f).toFloat())
        }

        // Minimum pour loger heure et double coche
        val minBubbleW = max(dp(64f), (timeWidth + innerPaddingH * 2).toInt())
        var bubbleW = (measuredContentW + innerPaddingH * 2).toInt()
        bubbleW = max(minBubbleW, min(maxBubbleW, bubbleW))

        if (markup != null && markup.rows.isNotEmpty()) {
            bubbleW = max(bubbleW, dp(200f))
        }

        val totalBubbleH = contentH + buttonsHeight
        val cellTopPadding = dp(2f)
        val cellBottomPadding = dp(2f)
        val totalCellH = max(dp(38.9f), totalBubbleH + cellTopPadding + cellBottomPadding) // 107px minimum

        // Positionnement de la bulle
        val marginEdge = dp(MARGIN_SCREEN_EDGE_DP)
        val tailW = dp(TAIL_WIDTH_DP)

        val left: Float
        val right: Float
        if (msg.isOutgoing) {
            right = (totalWidth - marginEdge).toFloat()
            left = right - bubbleW
        } else {
            left = marginEdge.toFloat()
            right = left + bubbleW
        }

        val top = cellTopPadding.toFloat()
        val bottom = (cellTopPadding + totalBubbleH).toFloat()
        bubbleRect.set(left, top, right, bottom)

        // Configuration des boutons inline
        if (markup != null && markup.rows.isNotEmpty()) {
            var btnY = bottom - buttonsHeight + dp(4f)
            val btnH = dp(BUTTON_HEIGHT_DP).toFloat()
            val rowGap = dp(BUTTON_ROW_GAP_DP).toFloat()
            val colGap = dp(BUTTON_COL_GAP_DP).toFloat()
            val btnAreaW = bubbleRect.width() - dp(12f)
            val btnAreaLeft = bubbleRect.left + dp(6f)

            for (rowIndex in markup.rows.indices) {
                val row = markup.rows[rowIndex]
                val cols = row.size
                if (cols > 0) {
                    val singleBtnW = (btnAreaW - colGap * (cols - 1)) / cols
                    for (colIndex in row.indices) {
                        val btnLeft = btnAreaLeft + colIndex * (singleBtnW + colGap)
                        val btnRight = btnLeft + singleBtnW
                        val btnRect = RectF(btnLeft, btnY, btnRight, btnY + btnH)
                        val isBottomRow = (rowIndex == markup.rows.size - 1)
                        val isBottomLeft = isBottomRow && (colIndex == 0)
                        val isBottomRight = isBottomRow && (colIndex == cols - 1)
                        renderButtons.add(RenderButton(row[colIndex], btnRect, isBottomLeft, isBottomRight))
                    }
                }
                btnY += btnH + rowGap
            }
        }

        // Construction du Path de la bulle (radii et tail)
        buildBubblePath(msg, left, top, right, bottom, tailW)

        setMeasuredDimension(totalWidth, totalCellH)
    }

    private fun buildBubblePath(
        msg: Message,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        tailW: Int
    ) {
        bubblePath.rewind()
        val rNormal = dpf2(BUBBLE_RADIUS_DP)
        val rNear = dpf2(BUBBLE_NEAR_RADIUS_DP)

        val hasBotButtons = msg.replyMarkup != null && msg.replyMarkup!!.rows.isNotEmpty()

        val rTopLeft = if (msg.isOutgoing) rNormal else (if (msg.isGroupedTop) rNear else rNormal)
        val rTopRight = if (!msg.isOutgoing) rNormal else (if (msg.isGroupedTop) rNear else rNormal)
        val rBottomLeft = if (msg.isOutgoing) rNormal else (if (hasBotButtons || msg.isGroupedBottom) rNear else rNormal)
        val rBottomRight = if (!msg.isOutgoing) rNormal else (if (hasBotButtons || msg.isGroupedBottom) rNear else rNormal)

        radii[0] = rTopLeft; radii[1] = rTopLeft
        radii[2] = rTopRight; radii[3] = rTopRight
        radii[4] = rBottomRight; radii[5] = rBottomRight
        radii[6] = rBottomLeft; radii[7] = rBottomLeft

        bubblePath.addRoundRect(bubbleRect, radii, Path.Direction.CW)

        // Ajout de la pointe (tail) débordante si non groupé en bas et pas de boutons inline
        if (!hasBotButtons && !msg.isGroupedBottom) {
            if (msg.isOutgoing) {
                // Pointe à droite débordant de +15dp
                val tailPath = Path().apply {
                    moveTo(right - dpf2(2f), bottom - dpf2(12f))
                    quadTo(right + tailW, bottom - dpf2(2f), right + tailW, bottom)
                    lineTo(right - dpf2(10f), bottom)
                    close()
                }
                bubblePath.op(tailPath, Path.Op.UNION)
            } else {
                // Pointe à gauche débordant de -15dp (ou intégrée au bord x=31px)
                val tailPath = Path().apply {
                    moveTo(left + dpf2(2f), bottom - dpf2(12f))
                    quadTo(left - tailW, bottom - dpf2(2f), left - tailW, bottom)
                    lineTo(left + dpf2(10f), bottom)
                    close()
                }
                bubblePath.op(tailPath, Path.Op.UNION)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val msg = currentMessage ?: return

        // Cas 1 : Séparateur de date
        if (msg.isDateSeparator || msg.type == MessageType.DATE_SEPARATOR) {
            drawDateSeparator(canvas, msg.text)
            return
        }

        // Dessin de la bulle
        if (msg.isOutgoing) {
            drawOutgoingBubble(canvas)
        } else {
            drawIncomingBubble(canvas, msg.isSelected)
        }

        var contentY = bubbleRect.top + dp(7f)
        val contentX = bubbleRect.left + dp(11f)

        // En-tête transféré
        forwardLayout?.let { layout ->
            canvas.save()
            canvas.translate(contentX, contentY)
            layout.draw(canvas)
            canvas.restore()
            contentY += layout.height + dp(4f)
        }

        // Bloc de citation (Reply)
        if (msg.replyInfo != null && quoteTextLayout != null) {
            drawQuoteBlock(canvas, msg.replyInfo, contentX, contentY)
            contentY += dp(18f) + quoteTextLayout!!.height + dp(6f)
        }

        // Document
        if (msg.type == MessageType.DOCUMENT && msg.documentInfo != null) {
            drawDocumentBlock(canvas, msg.documentInfo, contentX, contentY)
            contentY += dp(48f)
        }

        // Texte du message
        messageLayout?.let { layout ->
            canvas.save()
            canvas.translate(contentX, contentY)
            layout.draw(canvas)
            canvas.restore()
            contentY += layout.height
        }

        // Heure et accusé de lecture
        drawTimeAndStatus(canvas, msg)

        // Boutons inline de bot (Phase A1)
        if (renderButtons.isNotEmpty()) {
            drawBotButtons(canvas)
        }
    }

    private fun drawDateSeparator(canvas: Canvas, text: String) {
        val textW = dateServiceTextPaint.measureText(text)
        val pillW = textW + dp(22f)
        val pillH = dp(21f)
        val cx = width / 2f
        val cy = height / 2f

        tempRect.set(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f)
        val r = pillH / 2f
        canvas.drawRoundRect(tempRect, r, r, dateServiceBgPaint)

        val fontMetrics = dateServiceTextPaint.fontMetrics
        val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(text, cx, textY, dateServiceTextPaint)
    }

    private fun drawIncomingBubble(canvas: Canvas, isSelected: Boolean) {
        bubblePaint.shader = null
        bubblePaint.color = if (isSelected) ThemeColors.CHAT_IN_BUBBLE_SELECTED else ThemeColors.CHAT_IN_BUBBLE
        canvas.drawPath(bubblePath, bubblePaint)
    }

    private fun drawOutgoingBubble(canvas: Canvas) {
        val screenH = max(1000, AndroidUtilities.displaySize.y)
        if (outgoingShader == null || lastScreenHeight != screenH) {
            lastScreenHeight = screenH
            val colors = intArrayOf(
                ThemeColors.CHAT_OUT_BUBBLE_GRADIENT_3, // #9F3EAA haut
                ThemeColors.CHAT_OUT_BUBBLE_GRADIENT_2, // #8146D7
                ThemeColors.CHAT_OUT_BUBBLE_GRADIENT_1, // #4272DF
                ThemeColors.CHAT_OUT_BUBBLE             // #258DE5 bas
            )
            val positions = floatArrayOf(0f, 0.33f, 0.66f, 1f)
            outgoingShader = LinearGradient(
                0f, 0f, 0f, screenH.toFloat(),
                colors, positions, Shader.TileMode.CLAMP
            )
        }

        // Ancrage à l'ÉCRAN : translate le shader par -cell.top (Phase A3)
        val cellTopOnScreen = top.toFloat()
        shaderMatrix.reset()
        shaderMatrix.postTranslate(0f, -cellTopOnScreen)
        outgoingShader!!.setLocalMatrix(shaderMatrix)

        bubblePaint.shader = outgoingShader
        canvas.drawPath(bubblePath, bubblePaint)
    }

    private fun drawQuoteBlock(canvas: Canvas, info: com.botpro.app.data.model.MessageReplyInfo, x: Float, y: Float) {
        val qLayout = quoteTextLayout ?: return
        val quoteW = bubbleRect.width() - dp(22f)
        val quoteH = dp(18f) + qLayout.height

        quoteRect.set(x, y, x + quoteW, y + quoteH)
        val r = dpf2(4f)
        canvas.drawRoundRect(quoteRect, r, r, quoteBgPaint)

        // Barre verticale verte 3.3dp
        val barW = dpf2(QUOTE_BAR_WIDTH_DP)
        tempRect.set(x, y, x + barW, y + quoteH)
        canvas.drawRoundRect(tempRect, barW / 2f, barW / 2f, quoteBarPaint)

        // Nom de l'auteur cité
        val textX = x + barW + dp(6f)
        val authorY = y + dp(13f)
        canvas.drawText(info.authorName, textX, authorY, quoteNamePaint)

        // Contenu cité
        canvas.save()
        canvas.translate(textX, y + dp(16f))
        qLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawDocumentBlock(canvas: Canvas, doc: com.botpro.app.data.model.MessageDocumentInfo, x: Float, y: Float) {
        // Cercle d'icône document
        val circleRadius = dpf2(20f)
        val cx = x + circleRadius
        val cy = y + circleRadius + dp(2f)
        canvas.drawCircle(cx, cy, circleRadius, docBgPaint)

        // Flèche vers le bas dans le cercle
        val arrowH = dpf2(6f)
        canvas.drawLine(cx, cy - arrowH, cx, cy + arrowH, docIconPaint)
        canvas.drawLine(cx - dpf2(4f), cy + dpf2(2f), cx, cy + arrowH, docIconPaint)
        canvas.drawLine(cx + dpf2(4f), cy + dpf2(2f), cx, cy + arrowH, docIconPaint)

        // Titre et poids du fichier
        val textX = cx + circleRadius + dp(10f)
        canvas.drawText(doc.fileName, textX, cy - dp(2f), docTitlePaint)
        canvas.drawText(doc.fileSizeString, textX, cy + dp(14f), docSubPaint)
    }

    private fun drawTimeAndStatus(canvas: Canvas, msg: Message) {
        if (timeText.isEmpty()) return

        val pTime = if (msg.isOutgoing) timePaintOut else timePaintIn
        val buttonsH = if (renderButtons.isNotEmpty()) {
            val rows = msg.replyMarkup?.rows?.size ?: 0
            rows * dp(BUTTON_HEIGHT_DP) + (rows - 1) * dp(BUTTON_ROW_GAP_DP) + dp(6f)
        } else 0

        val timeY = bubbleRect.bottom - buttonsH - dp(6f)
        val timeX = bubbleRect.right - dp(10f) - timeWidth

        canvas.drawText(timeText, timeX, timeY, pTime)

        // Double coche si sortant
        if (msg.isOutgoing) {
            val checkX = bubbleRect.right - dp(18f)
            val checkY = timeY - dp(3f)

            // Première coche
            canvas.drawLine(checkX - dp(5f), checkY, checkX - dp(2f), checkY + dp(3f), readCheckPaint)
            canvas.drawLine(checkX - dp(2f), checkY + dp(3f), checkX + dp(3f), checkY - dp(3f), readCheckPaint)

            // Deuxième coche si lu
            if (msg.isRead) {
                val c2 = checkX + dp(4f)
                canvas.drawLine(c2 - dp(5f), checkY, c2 - dp(2f), checkY + dp(3f), readCheckPaint)
                canvas.drawLine(c2 - dp(2f), checkY + dp(3f), c2 + dp(3f), checkY - dp(3f), readCheckPaint)
            }
        }
    }

    private fun drawBotButtons(canvas: Canvas) {
        val bubbleR = dpf2(BUBBLE_RADIUS_DP)
        val innerR = dpf2(BUTTON_INNER_RADIUS_DP)

        for (btn in renderButtons) {
            radii.fill(innerR)
            if (btn.isBottomLeft) {
                radii[6] = bubbleR; radii[7] = bubbleR
            }
            if (btn.isBottomRight) {
                radii[4] = bubbleR; radii[5] = bubbleR
            }

            bubblePath.rewind()
            bubblePath.addRoundRect(btn.rect, radii, Path.Direction.CW)
            canvas.drawPath(bubblePath, botButtonBgPaint)

            val textY = btn.rect.centerY() - (botButtonTextPaint.descent() + botButtonTextPaint.ascent()) / 2f
            canvas.drawText(btn.button.text, btn.rect.centerX(), textY, botButtonTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && renderButtons.isNotEmpty()) {
            val x = event.x
            val y = event.y
            for (btn in renderButtons) {
                if (btn.rect.contains(x, y)) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    onInlineButtonClicked?.invoke(btn.button)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
