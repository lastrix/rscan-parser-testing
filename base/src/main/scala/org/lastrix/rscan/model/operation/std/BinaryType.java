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

package org.lastrix.rscan.model.operation.std;

public enum BinaryType {
    OR,
    AND,
    BW_OR,
    BW_XOR,
    BW_AND,
    EQ,
    NEQ,
    EQ_ID,
    NEQ_ID,
    LT,
    LE,
    GT,
    GE,
    IN,
    INSTANCEOF,
    SHR,
    SHR_LOGICAL,
    SHL,
    ADD,
    SUB,
    MUL,
    MUL_MAT,
    DIV,
    IDIV,
    MOD,
    POW,
    COALESCE,
    AS,
    NOT_IN,
    IS,
    IS_NOT
}
