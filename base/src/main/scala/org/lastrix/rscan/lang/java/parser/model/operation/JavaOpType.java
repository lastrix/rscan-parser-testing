/*
 * Copyright (C) 2019-2020.  rscan-parser-testing project
 *
 * This file is part of rscan-parser-testing project.
 *
 * rscan-parser-testing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * rscan-parser-testing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rscan-parser-testing.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.lastrix.rscan.lang.java.parser.model.operation;

import org.lastrix.rscan.model.operation.ROpType;

public enum JavaOpType implements ROpType {
    RAW_DECL_PACKAGE,
    RAW_DECL_ANNOTATION_ELEMENT,
    RAW_DECL_RECEIVED_PARAMETER,
    RAW_DECL_CATCH_PARAMETER,
    THROWS,
    RESOURCE,
    TYPE_WILDCARD,
    BOUND,
    TO,
    WITH,
    RAW_TYPE_INSTANTIATION,
    RAW_CLASS_REFERENCE,
    RAW_EXPLICIT_CTOR_INVOKE,
    RAW_ARRAY_DIM_EXPR,
    RAW_ARRAY_INSTANTIATION,
    RAW_TYPE_CAST,
    RAW_METHOD_ACCESS,
    RAW_METHOD_REFERENCE,
    RAW_DIRECTIVE_REQUIRES,
    RAW_DIRECTIVE_EXPORTS,
    RAW_DIRECTIVE_OPENS,
    RAW_DIRECTIVE_USES,
    RAW_DIRECTIVE_PROVIDES,
    DIRECTIVE_REQUIRES,
    DIRECTIVE_EXPORTS,
    DIRECTIVE_OPENS,
    DIRECTIVE_USES,
    DIRECTIVE_PROVIDES,
    INITIALIZER,
    STATIC_INITIALIZER,
    RAW_NEW_QUALIFIED;


    @Override
    public boolean isRaw() {
        return name().startsWith("RAW_");
    }

    @Override
    public boolean hasOwnScope() {
        return false;
    }

    @Override
    public boolean isDecl() {
        return false;
    }

    @Override
    public boolean isDiscarded() {
        return false;
    }
}
