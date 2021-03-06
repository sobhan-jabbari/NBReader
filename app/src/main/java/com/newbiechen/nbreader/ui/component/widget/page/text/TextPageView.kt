package com.newbiechen.nbreader.ui.component.widget.page.text

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.newbiechen.nbreader.ui.component.book.text.config.TextConfig
import com.newbiechen.nbreader.ui.component.book.text.entity.TextPosition
import com.newbiechen.nbreader.ui.component.book.text.engine.*
import com.newbiechen.nbreader.ui.component.widget.page.PageType
import com.newbiechen.nbreader.ui.component.widget.page.TextAnimType
import com.newbiechen.nbreader.ui.component.widget.page.action.*
import com.newbiechen.nbreader.ui.component.widget.page.anim.ControlPageAnimation
import com.newbiechen.nbreader.ui.component.widget.page.anim.ScrollPageAnimation
import com.newbiechen.nbreader.ui.component.widget.page.anim.TextPageAnimation

/**
 *  author : newbiechen
 *  date : 2020-01-26 20:15
 *  description :页面文本内容 View
 */

typealias PageActionListener = (action: PageAction) -> Unit

class TextPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "TextPageView"
    }

    // 页面管理器
    private val mTextPageManager = TextPageManager(TextPageCallback())

    // 文本处理器
    // TODO：设置默认的 TextConfig 暂时先这么写
    private val mTextEngine: TextEngine = TextEngine(context, TextConfig.Builder(context).build())

    // 文本手势探测器
    private var mTextGestureDetector = TextGestureDetector(context, TextGestureCallback())

    private var mPageActionListener: PageActionListener? = null

    // View 使用的翻页动画，默认使用滑动动画
    private var mPageAnimation: TextPageAnimation = ScrollPageAnimation(this, mTextPageManager)

    // 当前翻页类型
    private var mPageAnimType: TextAnimType = TextAnimType.SCROLL

    private var isPrepareSize = false

    /**
     * 初始化
     */
    fun setTextModel(textModel: TextModel) {
        // 初始化
        mTextEngine.init(textModel)
        // 刷新
        pageInvalidate()
    }

    fun setTextConfig(textConfig: TextConfig) {
        // 设置配置项
        mTextEngine.setTextConfig(textConfig)
        // 通知页面无效
        pageInvalidate()
    }

    fun setPageAnim(type: TextAnimType): TextPageAnimation {
        if (mPageAnimType == type) {
            return mPageAnimation
        }

        mPageAnimation = when (type) {
            TextAnimType.CONTROL -> {
                ControlPageAnimation(this, mTextPageManager)
            }
            TextAnimType.SCROLL -> {
                ScrollPageAnimation(this, mTextPageManager)
            }
        }

        if (isPrepareSize) {
            mPageAnimation.setViewPort(
                mTextPageManager.pageWidth,
                mTextPageManager.pageHeight
            )
        }

        mPageAnimType = type

        // 请求刷新
        postInvalidate()

        return mPageAnimation
    }

    fun setPageListener(pageListener: TextPageListener) {
        // 设置页面监听
        mTextEngine.setPageListener(pageListener)
    }

    /**
     * 设置行为监听器
     */
    fun setPageActionListener(pageActionListener: PageActionListener) {
        mPageActionListener = pageActionListener
    }

    /**
     * 是否页面存在
     */
    fun hasPage(type: PageType): Boolean {
        return mTextPageManager.hasPage(type)
    }

    fun hasChapter(type: PageType): Boolean {
        return mTextEngine.hasChapter(type)
    }

    fun hasChapter(index: Int): Boolean {
        return mTextEngine.hasChapter(index)
    }

    fun getCurChapterIndex(): Int {
        return mTextEngine.getPagePosition(PageType.CURRENT)?.chapterIndex ?: 0
    }

    fun getPagePosition(pageType: PageType): PagePosition? {
        return mTextEngine.getPagePosition(pageType)
    }

    fun getPageProgress(type: PageType): PageProgress? {
        return mTextEngine.getPageProgress(type)
    }

    fun getPageCount(pageType: PageType): Int {
        return mTextEngine.getPageCount(pageType)
    }

    fun getTextConfig(): TextConfig {
        return mTextEngine.getTextConfig()
    }

    fun skipChapter(type: PageType) {
        if (!hasChapter(type)) {
            return
        }

        val currentIndex = getCurChapterIndex()

        val index = when (type) {
            PageType.PREVIOUS -> {
                currentIndex - 1
            }
            PageType.NEXT -> {
                currentIndex + 1
            }
            else -> {
                currentIndex
            }
        }

        skipPage(index, 0)
    }

    fun skipChapter(index: Int) {
        if (!hasChapter(index)) {
            return
        }

        skipPage(index, 0)
    }

    fun skipPage(chapterIndex: Int, pageIndex: Int) {
        // TODO:需要检测 position 是否正确
        // 跳转页面
        mTextEngine.skipPage(PagePosition(chapterIndex, pageIndex))
        // 通知重绘
        pageInvalidate()
    }

    fun skipPage(position: TextPosition) {
        // TODO:需要检测 position 是否正确
        // 跳转页面
        mTextEngine.skipPage(position)
        // 通知重绘
        pageInvalidate()
    }

    /**
     * 页面无效
     */
    fun pageInvalidate() {
        // 重置所有页面
        mTextPageManager.resetPages()
        // 请求刷新
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mPageAnimation.setViewPort(w, h)

        isPrepareSize = true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 将点击事件全部交给 pageAction 进行处理
        mTextGestureDetector.onTouchEvent(event!!)
        return true
    }

    /**
     * 接收事件分发的处理
     */
    private fun onPageAction(action: PageAction) {
        // 页面行为处理
        val result = when (action) {
            is MotionAction -> {
                // 专门处理点击事件
                onPageMotionEvent(action)
            }
            else -> {
                onPageActionEvent(action)
            }
        }

        // 如果不消耗该事件，则直接返回
        if (!result) {
            mPageActionListener?.invoke(action)
        }
    }

    /**
     * 是否消耗页面行为事件
     */
    private fun onPageActionEvent(action: PageAction): Boolean {
        return false
    }

    // 是否消耗按下事件
    private var isConsumerTouch: Boolean = false

    // 是否消耗了点击事件
    private var isConsumerAction: Boolean = false

    /**
     * 处理页面运动事件的逻辑
     */
    private fun onPageMotionEvent(action: MotionAction): Boolean {
        when (action.type) {
            MotionType.PRESS -> {
                // 处理常规点击事件的逻辑
                isConsumerTouch = onPageTouchEvent(action)
                isConsumerAction = isConsumerTouch
            }
            MotionType.MOVE, MotionType.RELEASE, MotionType.CANCEL -> {
                // 如果 TouchEvent 消耗了 press，则之后的  MOVE、RELEASE、CANCEL 都默认消耗
                // 如果 TouchEvent 没有消耗 press，则之后的  MOVE、RELEASE、CANCEL 都不会消耗
                // 如果 TouchEvent 消耗了 press，但是 MOVE 返 false，则之后不会调用 TouchEvent，但是后续事件默认消耗
                if (isConsumerTouch) {
                    isConsumerTouch = onPageTouchEvent(action)
                }
            }
            else -> {
                // 处理手势逻辑
                isConsumerAction = onPageGestureEvent(action)
            }
        }

        // 是否消耗事件
        return isConsumerAction
    }

    /**
     * 处理页面点击事件的逻辑：包含如下事件
     * PRESS、MOVE、RELEASE、CANCEL
     *
     */
    private fun onPageTouchEvent(action: MotionAction): Boolean {
        return mPageAnimation.onTouchEvent(action)
    }

    /**
     * 处理页面手势事件的逻辑
     */
    private fun onPageGestureEvent(action: MotionAction): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas?) {
        mPageAnimation.draw(canvas!!)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mPageAnimation.abortAnim()
    }

    override fun computeScroll() {
        super.computeScroll()
        // 对滑动动画做处理
        mPageAnimation.computeScroll()
    }

    // 页面改变回调
    private inner class TextPageCallback :
        TextPageManager.OnPageListener {
        override fun onPageSizeChanged(width: Int, height: Int) {
            mTextEngine.setViewPort(width, height)
        }

        override fun onTurnPage(pageType: PageType) {
            mTextEngine.turnPage(pageType)

            // TODO：页面预加载逻辑(暂时先不处理)

            // 如果切换页面，则根据切换类型预加载页面
            // 因为，PageBitmap 实现了三张图的缓存，因此翻到上一页，则 preBitmap 空缺。
            // TODO:逻辑放在这里有点不知所云，不懂原理很难理解。(看看有没有好的情况处理这个问题)
/*            if (hasPage(type)) {
                mSingleExecutor.execute {
                    LogHelper.i(PageView.TAG, "onTurnPage: $type")
                    mTextProcessor.preparePage(type)
                }
            }*/
        }

        override fun hasPage(type: PageType): Boolean {
            return mTextEngine.hasPage(type)
        }

        override fun drawPage(canvas: Canvas, type: PageType) {
            // 绘制文本内容
            mTextEngine.draw(canvas, type)
            // TODO:页面预加载逻辑(暂时先不处理)
/*            // 如果绘制的是当前页，预加载下一页
            if (type == PageType.CURRENT && hasPage(PageType.NEXT)) {
                mSingleExecutor.execute {
                    mTextProcessor.preparePage(PageType.NEXT)
                }
            }*/
        }
    }

    // 事件点击回调
    private inner class TextGestureCallback : TextGestureDetector.OnTextGestureListener {
        // TODO:需要创建一个 RecyclerBin 解决 MotionAction 被频繁创建的问题？(之后优化)，应该用 MotionAction.obtain() 获取缓存 MotionAction
        override fun onPress(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.PRESS, event)
            )
        }

        override fun onMove(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.MOVE, event)
            )
        }

        override fun onRelease(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.RELEASE, event)
            )
        }

        override fun onLongPress(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.LONG_PRESS, event)
            )
        }

        override fun onMoveAfterLongPress(event: MotionEvent) {
        }

        override fun onReleaseAfterLongPress(event: MotionEvent) {
        }

        override fun onSingleTap(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.SINGLE_TAP, event)
            )
        }

        override fun onDoubleTap(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.DOUBLE_TAP, event)
            )
        }

        override fun onCancelTap(event: MotionEvent) {
            onPageAction(
                MotionAction(MotionType.CANCEL, event)
            )
        }
    }
}