package com.example.newbiechen.nbreader.ui.component.widget.page

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.newbiechen.nbreader.R
import com.example.newbiechen.nbreader.ui.component.book.text.processor.TextProcessor
import com.example.newbiechen.nbreader.ui.component.widget.page.action.*

/**
 *  author : newbiechen
 *  date : 2020-01-26 20:15
 *  description :页面文本内容 View
 */

class PageTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mTextPageManager = TextPageManager(PageTextCallback())

    private var mTextProcessor: TextProcessor? = null

    // TODO:页面行为处理器，(传入 textView 有歧义，等之后处理 Page 的点击事件再详细分析)
    private var mPageActionProcessor = PageActionProcessor(this)
        .also {
            it.setPageActionListener(this::onDispatchAction)
        }

    private var mPageActionListener: PageActionListener? = null

    private var mCurPageType: PageType = PageType.CURRENT

    private var isPrepareSize = false


    /**
     * 设置文本处理器
     */
    fun setTextProcessor(textProcessor: TextProcessor) {
        mTextProcessor = textProcessor

        // 监听 TextProcessor 页面的改变
        mTextProcessor!!.setPageInvalidateListener(this::onPageInvalidate)

        // 更新文本处理器的绘制视口
        if (isPrepareSize) {
            mTextProcessor!!.setViewPort(mTextPageManager.pageWidth, mTextPageManager.pageHeight)
        }
    }

    /**
     * 设置行为监听器
     */
    fun setPageActionListener(pageActionListener: PageActionListener) {
        mPageActionListener = pageActionListener
    }

    /**
     * 指定要绘制的页面
     * @param: 绘制的页面类型
     */
    fun setDrawPage(type: PageType) {
        mCurPageType = type
    }

    /**
     * 是否页面存在
     */
    fun hasPage(type: PageType): Boolean {
        return mTextPageManager.hasPage(type)
    }

    /**
     * 通知翻页
     */
    fun turnPage(pageType: PageType) {
        if (pageType == PageType.NEXT) {
            mTextPageManager.turnPage(true)
        } else if (pageType == PageType.PREVIOUS) {
            mTextPageManager.turnPage(false)
        }
    }

    /**
     * 页面无效回调
     */
    private fun onPageInvalidate() {
        // 重置所有页面
        mTextPageManager.resetPages()
        // 请求刷新
        postInvalidate()
    }

    /**
     * 接收事件分发的处理
     */
    private fun onDispatchAction(action: PageAction) {
        // TODO:暂时不知道 PageTextView 是否要消耗这些事件，就先这么写
        mPageActionListener?.invoke(action)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mTextPageManager.setPageSize(w, h)
        isPrepareSize = true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 将点击事件全部交给 pageAction 进行处理
        mPageActionProcessor.onTouchEvent(event!!)
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        // 从管理器中获取页面内容
        val bitmap = mTextPageManager.getPage(mCurPageType)
        // 进行绘制
        canvas!!.drawBitmap(bitmap, 0f, 0f, null)
    }

    private inner class PageTextCallback : TextPageManager.OnPageListener {
        override fun onPageSizeChanged(width: Int, height: Int) {
            mTextProcessor?.setViewPort(width, height)
        }

        override fun onTurnPage(pageType: PageType) {
            mTextProcessor?.turnPage(pageType)

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

            // 发送翻页事件
            mPageActionListener?.invoke(TurnPageAction(pageType))
        }

        override fun hasPage(type: PageType): Boolean {
            return mTextProcessor?.hasPage(type) ?: false
        }

        override fun drawPage(canvas: Canvas, type: PageType) {
            // 清屏操作
            canvas.drawColor(
                context.resources.getColor(R.color.colorTransparent),
                PorterDuff.Mode.CLEAR
            )
            // 绘制文本内容
            mTextProcessor?.draw(canvas, type)

            // TODO:页面预加载逻辑(暂时先不处理)

/*            // 如果绘制的是当前页，预加载下一页
            if (type == PageType.CURRENT && hasPage(PageType.NEXT)) {
                mSingleExecutor.execute {
                    mTextProcessor.preparePage(PageType.NEXT)
                }
            }*/
        }
    }
}