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

package org.lastrix.rscan.api.parser.antlr4

import org.lastrix.rscan.api.parser.antlr4.lexer.AbstractRScanLexer

/**
 * Allows to configure and tune parser process for specific needs
 *
 * @param offsetLine         the initial offset line for source
 * @param offsetLinePosition the initial offset line position for source
 * @param onLexerComplete    this method will be called once tokenizing complete
 */
sealed case class Antlr4Config
(
  offsetLine: Int = 0,
  offsetLinePosition: Int = 0,
  onLexerComplete: AbstractRScanLexer => Unit = { _ => },
  initialRule: Option[String] = None,
  additionalSuccessMsg: Option[String] = None,
)
