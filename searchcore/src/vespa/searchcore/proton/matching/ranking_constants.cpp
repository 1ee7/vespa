// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "ranking_constants.h"

namespace proton {
namespace matching {

RankingConstants::Constant::Constant(const vespalib::string &name_in,
                                     const vespalib::string &type_in,
                                     const vespalib::string &filePath_in)
    : name(name_in),
      type(type_in),
      filePath(filePath_in)
{
}

RankingConstants::Constant::~Constant() {}

bool
RankingConstants::Constant::operator==(const Constant &rhs) const
{
    return (name == rhs.name) &&
           (type == rhs.type) &&
           (filePath == rhs.filePath);
}

RankingConstants::RankingConstants()
    : _constants()
{
}

RankingConstants::~RankingConstants() {}

RankingConstants::RankingConstants(const Vector &constants)
    : _constants()
{
    for (const auto &constant : constants) {
        _constants.insert(std::make_pair(constant.name, constant));
    }
}

bool
RankingConstants::operator==(const RankingConstants &rhs) const
{
    return _constants == rhs._constants;
}

const RankingConstants::Constant *
RankingConstants::getConstant(const vespalib::string &name) const
{
    auto itr = _constants.find(name);
    if (itr != _constants.end()) {
        return &itr->second;
    }
    return nullptr;
}

}
}

