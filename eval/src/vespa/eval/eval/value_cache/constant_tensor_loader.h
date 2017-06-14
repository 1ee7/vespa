// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "constant_value.h"
#include <vespa/eval/eval/tensor_engine.h>
#include <vespa/vespalib/stllike/string.h>

namespace vespalib {
namespace eval {

/**
 * A ConstantValueFactory that will load constant tensor values from
 * file. The file is expected to be in json format with the same
 * structure used when feeding. The tensor is created by first
 * building a generic TensorSpec object and then converting it to a
 * specific tensor using the TensorEngine interface.
 **/
class ConstantTensorLoader : public ConstantValueFactory
{
private:
    const TensorEngine &_engine;
public:
    ConstantTensorLoader(const TensorEngine &engine) : _engine(engine) {}
    ConstantValue::UP create(const vespalib::string &path, const vespalib::string &type) const override;
};

} // namespace vespalib::eval
} // namespace vespalib
