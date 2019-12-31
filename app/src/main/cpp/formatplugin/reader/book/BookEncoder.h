// author : newbiechen
// date : 2019-12-30 16:17
// description : 书籍编码器
//

#ifndef NBREADER_BOOKENCODER_H
#define NBREADER_BOOKENCODER_H

#include "../text/tag/TextStyleType.h"
#include "../text/entity/TextParagraph.h"
#include <string>
#include <vector>
#include <stack>
#include <reader/text/TextEncoder.h>

class BookEncoder {
public:
    BookEncoder() {
    }

    ~BookEncoder() {
    }

    void open();

    size_t close(char **outBuffer);

    // 标签样式标记入栈
    void pushTextStyle(TextStyleType type);

    // 标签样式标记出栈
    bool popTextStyle();

    // 开始处理段落，传入参数指定段落段落
    void beginParagraph(TextParagraph::Type type = TextParagraph::TEXT_PARAGRAPH);

    // 结束段落处理
    void endParagraph();

    // 添加文本数据
    void addText(const std::string &text);

    // 添加标题文本
    void addTitleText(const std::string &text);

    // 插入 section paragraph 结束标记
    void insertEndOfSectionParagraph();

    // 启动标题段落
    void beginTitleParagraph(int paragraphIndex = -1);

    // 结束标题段落
    void endTitleParagraph();

    // 是否正在处理段落标签
    bool hasParagraphOpen() {
        return isParagraphOpen;
    }

    // 是否正在处理标题段落标签
    bool hasTitleParagraphOpen() {
        return isTitleParagraphOpen;
    }

private:
    void insertEndParagraph(TextParagraph::Type type);

    // 将段落缓冲输出到 textModel 中
    void flushParagraphBuffer();

private:
    // 文本编码器
    TextEncoder mTextEncoder;
    // 文本样式栈
    std::vector<TextStyleType> mTextStyleStack;
    // 段落文本列表
    std::vector<std::string> mParagraphTextList;
    // 是否已经存在打开的段落
    bool isParagraphOpen;
    bool isTitleParagraphOpen;
    // 是否区域包含纯文本内容
    bool isSectionContainsRegularContents;
};

#endif //NBREADER_BOOKENCODER_H