package com.newbiechen.nbreader.ui.component.book.text.engine

import android.content.Context
import android.graphics.Canvas
import android.util.Size
import com.newbiechen.nbreader.ui.component.book.text.config.TextConfig
import com.newbiechen.nbreader.ui.component.book.text.entity.TextMetrics
import com.newbiechen.nbreader.ui.component.book.text.entity.element.*
import com.newbiechen.nbreader.ui.component.book.text.entity.textstyle.TreeTextStyle
import com.newbiechen.nbreader.ui.component.book.text.engine.cursor.TextParagraphCursor
import com.newbiechen.nbreader.ui.component.book.text.util.TextDimenUtil
import com.newbiechen.nbreader.ui.component.widget.page.PageType

/**
 *  author : newbiechen
 *  date : 2019-10-24 18:16
 *  description :基础文本渲染引擎
 */

abstract class BaseTextEngine(private val context: Context, textConfig: TextConfig) {
    /**
     * 视口宽高
     */
    var viewWidth: Int = 0
        private set
    var viewHeight: Int = 0
        private set

    // 文本画笔
    protected var mPaintContext: TextPaintContext = TextPaintContext()

    // 文本配置项
    private var mTextConfig: TextConfig = textConfig

    // 使用的文本样式
    private var mTextStyle: TreeTextStyle? = null

    private var mWordHeight: Int? = null

    protected abstract fun onSizeChanged(width: Int, height: Int)

    protected abstract fun drawInternal(canvas: TextCanvas, pageType: PageType)

    /**
     * 设置视口
     */
    fun setViewPort(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height

        // TODO：需要清除缓存
        mMetrics = null

        onSizeChanged(width, height)
    }

    /**
     *  绘制传入的页面
     */
    fun draw(canvas: Canvas, pageType: PageType) {
        drawInternal(TextCanvas(mPaintContext, canvas), pageType)
    }

    /**
     * 获取文本绘制区域
     */
    fun getTextAreaSize(): Size {
        return Size(getTextAreaWidth(), getTextAreaHeight())
    }

    /**
     * 获取文本区域宽
     */
    fun getTextAreaWidth(): Int {
        return viewWidth - mTextConfig.getMarginLeft() - mTextConfig.getMarginRight()
    }

    /**
     * 获取文本区域高
     */
    fun getTextAreaHeight(): Int {
        return viewHeight - mTextConfig.getMarginTop() - mTextConfig.getMarginBottom()
    }

    fun setTextConfig(textConfig: TextConfig) {
        mTextConfig = textConfig
        // TODO:需要清空缓存
    }

    fun getTextConfig() = mTextConfig

    /**
     * 设置当前文本样式
     */
    protected fun setTextStyle(style: TreeTextStyle) {
        if (mTextStyle != style) {
            mTextStyle = style
            mWordHeight = null
        }

        mPaintContext.setFont(
            context,
            style.getFontSize(getMetrics()),
            bold = false,
            italic = false,
            underline = false,
            strikeThrough = false
        )
    }

    /**
     * 获取当前文本样式
     */
    protected fun getTextStyle(): TreeTextStyle {
        if (mTextStyle == null) {
            resetTextStyle()
        }
        return mTextStyle!!
    }

    /**
     * 重置文本样式
     */
    protected fun resetTextStyle() {
        setTextStyle(mTextConfig.getBaseTextStyle())
    }

    /**
     * 是否样式类型元素如
     * @see
     */
    protected fun isStyleElement(element: TextElement): Boolean {
        return element === TextElement.StyleClose ||
                element is TextStyleElement ||
                element is TextControlElement
    }

    /**
     * 应用样式类型元素
     */
    protected fun applyStyleElement(element: TextElement) {
        when {
            element === TextElement.StyleClose -> applyStyleClose()
            element is TextStyleElement -> applyStyle(element)
            element is TextControlElement -> applyControl(element)
        }
    }

    protected fun applyStyleChange(cursor: TextParagraphCursor, index: Int, end: Int) {
        var index = index
        // 从 cursor 中处理所有的 Element
        while (index < end) {
            applyStyleElement(cursor.getElement(index)!!)
            ++index
        }
    }

    private fun applyStyleClose() {
        if (mTextStyle!!.parent != null) {
            setTextStyle(mTextStyle!!.parent!!)
        }
    }

    private fun applyStyle(element: TextStyleElement) {
        setTextStyle(
            mTextConfig.getCSSDecoratedStyle(mTextStyle!!, element.styleTag)
        )
    }

    private fun applyControl(control: TextControlElement) {
        if (control.isStart) {
            // 暂时不处理超链接
/*            val hyperlink = if (control is TextHyperlinkControlElement)
                (control as TextHyperlinkControlElement).Hyperlink
            else null*/
            setTextStyle(
                mTextConfig.getControlDecoratedStyle(mTextStyle!!, control.type)
            )
        } else {
            if (mTextStyle!!.parent != null) {
                setTextStyle(mTextStyle!!.parent!!)
            }
        }
    }

    private var mMetrics: TextMetrics? = null

    /**
     * 文本指标，用于解决 px、em、rem 的换算问题
     */
    protected fun getMetrics(): TextMetrics {
        var m: TextMetrics? = mMetrics
        if (m == null) {
            m = TextMetrics(
                TextDimenUtil.getDisplayDPI(context), // dpi
                viewWidth,
                viewHeight,
                mTextConfig.getBaseTextStyle().getFontSize()// 获取文字的大小
            )
            mMetrics = m
        }
        return m
    }

    /**
     * 计算元素的宽
     */
    // 获取文本元素可用的宽度
    protected fun getElementWidth(element: TextElement, charIndex: Int): Int {
        return when {
            element is TextWordElement -> getWordWidth(element, charIndex)
            element === TextElement.NBSpace -> mPaintContext.getSpaceWidth()
            element === TextElement.Indent -> mTextStyle!!.getFirstLineIndent(getMetrics())
            element is TextImageElement -> {
                // 获取图片的尺寸
                val size: Size? = mPaintContext.getImageSize(
                    element.image,
                    // 传入当前可显示区域
                    getTextAreaSize()
                    // 传入缩放类型
/*                getScalingType(imageElement)*/
                )
                return size?.width ?: 0
            }
            else -> 0
        }
    }

    /**
     * 计算元素的高
     */
    // 获取文本元素可用的高度
    protected fun getElementHeight(element: TextElement): Int {
        if (element === TextElement.NBSpace ||
            element is TextWordElement ||
            element is TextFixedHSpaceElement
        ) {
            return getWordHeight()
        } else if (element is TextImageElement) {
            val size: Size? = mPaintContext.getImageSize(
                element.image,
                // 传入当前可显示区域
                getTextAreaSize()
                // 传入缩放类型
/*                getScalingType(imageElement)*/
            )
            // 设置图片的高度
            return (size?.height ?: 0) + (mPaintContext.getStringHeight() *
                    (mTextStyle!!.getLineSpacePercent() - 100) / 100).coerceAtLeast(3)
        }
        return 0
    }

    protected fun getElementDescent(element: TextElement): Int {
        return if (element is TextWordElement) mPaintContext.getDescent() else 0
    }

    /**
     * 获取单词的宽
     */
    protected fun getWordWidth(word: TextWordElement, start: Int): Int {
        return if (start == 0) {
            word.getWidth(mPaintContext)
        } else {
            mPaintContext.getStringWidth(word.data, word.offset + start, word.length - start)
        }
    }

    private var mWordPartArray = CharArray(20)

    protected fun getWordWidth(
        word: TextWordElement,
        start: Int,
        length: Int,
        addHyphenationSign: Boolean
    ): Int {
        var len = length
        if (len == -1) {
            if (start == 0) {
                return word.getWidth(mPaintContext)
            }
            len = word.length - start
        }

        if (!addHyphenationSign) {
            return mPaintContext.getStringWidth(word.data, word.offset + start, len)
        }

        var part = mWordPartArray
        if (len + 1 > part.size) {
            part = CharArray(len + 1)
            mWordPartArray = part
        }

        System.arraycopy(word.data, word.offset + start, part, 0, len)
        part[len] = '-'
        return mPaintContext.getStringWidth(part, 0, len + 1)
    }


    /**
     * 获取单词的高度
     */
    protected fun getWordHeight(): Int {
        if (mWordHeight == null) {
            val textStyle = mTextStyle!!
            mWordHeight =
                mPaintContext.getStringHeight() * textStyle.getLineSpacePercent() / 100 + textStyle.getVerticalAlign(
                    getMetrics()
                )
        }
        return mWordHeight!!
    }

    protected fun drawWord(
        canvas: TextCanvas,
        x: Int,
        y: Int,
        word: TextWordElement,
        start: Int,
        length: Int,
        addHyphenationSign: Boolean,
        color: Int
    ) {
        if (start == 0 && length == -1) {
            drawString(canvas, x, y, word.data, word.offset, word.length, color, 0)
        } else {
            var len = length
            if (len == -1) {
                len = word.length - start
            }
            if (!addHyphenationSign) {
                drawString(canvas, x, y, word.data, word.offset + start, len, color, start)
            } else {
                var part = mWordPartArray
                if (len + 1 > part.size) {
                    part = CharArray(len + 1)
                    mWordPartArray = part
                }

                System.arraycopy(word.data, word.offset + start, part, 0, len)
                part[len] = '-'
                drawString(canvas, x, y, part, 0, len + 1, color, start)
            }
        }
    }

    protected fun drawString(
        canvas: TextCanvas,
        x: Int,
        y: Int,
        str: CharArray,
        offset: Int,
        length: Int,
        color: Int,
        shift: Int
    ) {
        // 设置文字显示的颜色
        mPaintContext.setTextColor(color)
        // 进行绘制
        canvas.drawString(x, y, str, offset, length)

        // TODO:没有处理存在 Mark 的情况
    }
}