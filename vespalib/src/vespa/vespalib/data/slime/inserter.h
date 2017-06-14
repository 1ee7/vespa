// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "type.h"
#include <vespa/vespalib/data/memory.h>
#include "symbol.h"

namespace vespalib {

class Slime;

namespace slime {

class Cursor;

//-----------------------------------------------------------------------------

/**
 * Interface for inserting a value while hiding how/where it is
 * inserted.
 **/
struct Inserter {
    virtual Cursor &insertNix() const = 0;
    virtual Cursor &insertBool(bool value) const = 0;
    virtual Cursor &insertLong(int64_t value) const = 0;
    virtual Cursor &insertDouble(double value) const = 0;
    virtual Cursor &insertString(Memory value) const = 0;
    virtual Cursor &insertData(Memory value) const = 0;
    virtual Cursor &insertArray() const = 0;
    virtual Cursor &insertObject() const = 0;
    virtual ~Inserter() {}
};

//-----------------------------------------------------------------------------

struct SlimeInserter : Inserter {
    Slime &slime;
    explicit SlimeInserter(Slime &s) : slime(s) {}

    Cursor &insertNix() const override;
    Cursor &insertBool(bool value) const override;
    Cursor &insertLong(int64_t value) const override;
    Cursor &insertDouble(double value) const override;
    Cursor &insertString(Memory value) const override;
    Cursor &insertData(Memory value) const override;
    Cursor &insertArray() const override;
    Cursor &insertObject() const override;
};

struct ArrayInserter : Inserter {
    Cursor &cursor;
    explicit ArrayInserter(Cursor &c) : cursor(c) {}

    Cursor &insertNix() const override;
    Cursor &insertBool(bool value) const override;
    Cursor &insertLong(int64_t value) const override;
    Cursor &insertDouble(double value) const override;
    Cursor &insertString(Memory value) const override;
    Cursor &insertData(Memory value) const override;
    Cursor &insertArray() const override;
    Cursor &insertObject() const override;
};

struct ObjectSymbolInserter : Inserter {
    Cursor &cursor;
    Symbol symbol;
    ObjectSymbolInserter(Cursor &c, const Symbol &s) : cursor(c), symbol(s) {}

    Cursor &insertNix() const override;
    Cursor &insertBool(bool value) const override;
    Cursor &insertLong(int64_t value) const override;
    Cursor &insertDouble(double value) const override;
    Cursor &insertString(Memory value) const override;
    Cursor &insertData(Memory value) const override;
    Cursor &insertArray() const override;
    Cursor &insertObject() const override;
};

struct ObjectInserter : Inserter {
    Cursor &cursor;
    Memory name;
    ObjectInserter(Cursor &c, const Memory &n) : cursor(c), name(n) {}

    Cursor &insertNix() const override;
    Cursor &insertBool(bool value) const override;
    Cursor &insertLong(int64_t value) const override;
    Cursor &insertDouble(double value) const override;
    Cursor &insertString(Memory value) const override;
    Cursor &insertData(Memory value) const override;
    Cursor &insertArray() const override;
    Cursor &insertObject() const override;
};

} // namespace slime
} // namespace vespalib

