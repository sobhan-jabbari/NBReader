package com.example.newbiechen.nbreader.ui.component.book.text.entity

/**
 *  author : newbiechen
 *  date : 2019-12-25 18:18
 *  description :章节信息数据
 */

data class TextChapter(
    val chapterTitle: String, // 章节标题
    val startIndex: Int, // 在源文件的起始偏移值
    val endIndex: Int // 在源文件的终止偏移值
)