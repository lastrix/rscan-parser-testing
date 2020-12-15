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

package org.lastrix.rscan.api.parser.antlr4.parser

import org.antlr.v4.runtime.{Parser, TokenStream}
import org.lastrix.rscan.model.operation.std._
import org.lastrix.rscan.vfs.VirtualFile

abstract class AbstractRScanParser
(stream: TokenStream, override val file: VirtualFile)
  extends Parser(stream)
    with RScanParser
    with StdOpSupport
    with StmtOpSupport
    with ExprOpSupport
    with LiteralOpSupport {

  import FoldBlockSupport._

  override def ruleNameForFoldedBlock(foldBlock: RFoldOp): String =
    option(FoldRuleOption) match {
      case Some(rule) => rule
      case None => DefaultFoldRuleName
    }

  override def ruleName(ruleIndex: Int): String = getRuleNames()(ruleIndex)

  override final def getTokenNames: Array[String] = throw new UnsupportedOperationException

  ////////////////////////////////////// Attributes ////////////////////////////////////////////////////////////////////

  protected var _options = Map.empty[String, String]

  def options(vals: Seq[(String, String)]): Unit = for (item <- vals) _options += item

  def option(key: String): Option[String] = _options.get(key)

  def boolOption(key: String): Boolean =
    option(key) match {
      case Some(value) => java.lang.Boolean.parseBoolean(value)
      case None => false
    }
}
