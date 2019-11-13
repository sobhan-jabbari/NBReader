package com.example.newbiechen.nbreader.ui.component.widget.page

import com.example.newbiechen.nbreader.data.entity.BookEntity
import com.example.newbiechen.nbreader.data.local.room.dao.BookDao
import com.example.newbiechen.nbreader.ui.component.book.BookManager
import com.example.newbiechen.nbreader.ui.component.book.OnBookListener
import com.example.newbiechen.nbreader.ui.component.book.text.processor.TextProcessor

/**
 *  author : newbiechen
 *  date : 2019-11-03 20:55
 *  description :PageView 的页面控制器
 */

class PageController(private val pageView: PageView) {
    // 是否初始化
    var isInitialize: Boolean = false
        private set

    private var mBookManager: BookManager? = null
    private var mTextProcessor: TextProcessor = pageView.getTextProcessor()

    /**
     * 初始化操作
     * @param bookDao：书籍数据库
     */
    fun init(bookDao: BookDao) {
        // 创建书籍管理器，并传入文本处理器给 BookDao
        if (mBookManager == null) {
            isInitialize = true
            mBookManager = BookManager(bookDao, mTextProcessor)
        }
    }

    /**
     * 设置书籍监听
     */
    fun setBookListener(bookListener: OnBookListener) {
        mBookManager!!.setOnBookListener(bookListener)
    }

    /**
     * 打开书籍
     */
    fun openBook(bookEntity: BookEntity) {
        if (!isInitialize) {
            return
        }

        // 打开书籍 ==> 添加一个返回 Book
        mBookManager!!.openBook(pageView.context, bookEntity)
    }

    /**
     * 添加点击事件监听器
     */
    fun setActionListener(pageActionListener: PageActionListener) {
        pageView.setPageActionListener(pageActionListener)
    }
}