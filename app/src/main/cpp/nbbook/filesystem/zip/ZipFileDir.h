// author : newbiechen
// date : 2019-09-25 16:34
// description : Zip 文件目录
//

#ifndef NBREADER_ZIPFILEDIR_H
#define NBREADER_ZIPFILEDIR_H


#include "../FileDir.h"

class ZipFileDir : public FileDir {
public:
    ZipFileDir(const std::string &path) : FileDir(path) {
    }

    ~ZipFileDir() {
    }

protected:
    virtual void readFilePaths(std::vector<std::string> &paths, bool fullPath) const;

    virtual std::string getSeparator() const;
};


#endif //NBREADER_ZIPFILEDIR_H
