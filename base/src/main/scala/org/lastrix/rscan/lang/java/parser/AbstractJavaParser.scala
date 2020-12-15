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

package org.lastrix.rscan.lang.java.parser

import org.antlr.v4.runtime.{Token, TokenStream}
import org.lastrix.rscan.api.parser.antlr4.parser.{AbstractRScanParser, FoldBlockSupport}
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.lang.java.meta.JavaLanguage
import org.lastrix.rscan.model.literal._
import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.raw.RawOpType
import org.lastrix.rscan.model.operation.std.StdOpType
import org.lastrix.rscan.vfs._

abstract class AbstractJavaParser
(stream: TokenStream, file: VirtualFile)
  extends AbstractRScanParser(stream, file)
    with FoldBlockSupport {

  override def language: LanguageRef = JavaLanguage.ref

  override final def defaultRule: String = file.extension match {
    case "java" => "startJava"
    case _ => throw new UnsupportedOperationException
  }

  override final def templateRule: String = "startTemplate"

  def checkNoWs(count: Int): Boolean = {
    var i = 1
    while (i != -1 && i < count) {
      val a = _input.LT(i)
      val b = _input.LT(i + 1)
      if (a.getCharPositionInLine + a.getText.length != b.getCharPositionInLine)
        i = -1
      else
        i += 1
    }
    i != -1
  }

  def booleanLiteral(token: Token): ROp =
    if ("true".equalsIgnoreCase(token.getText)) trueLiteral(token)
    else falseLiteral(token)

  def stringLiteral(token: Token): ROp = opLiteral(token, new RStringLiteral(token.getText))

  def characterLiteral(token: Token): ROp =
    opLiteral(token, new RCharacterLiteral(token.getText.charAt(1)))

  def intLiteral(negate: Boolean, token: Token, radix: Int = 10): ROp = {
    val text = token.getText.replaceAll("_", "")
      .replaceAll("[lL]", "")
    integerLiteral(negate, if (radix == 10) text else text.substring(2), token, radix)
  }

  def floatLiteral(negate: Boolean, token: Token): ROp =
    floatPointLiteral(negate, token.getText, token)

  def floatHexLiteral(negate: Boolean, token: Token): ROp = ???

  def rawTypeWrap(op: ROp): ROp = op.`type` match {
    case RawOpType.RAW_TYPE => op
    case StdOpType.KEY | StdOpType.DECL => opNode(StdOpType.TYPE, op);
    case _ => opNode(RawOpType.RAW_TYPE, op)
  }

  private def floatPointLiteral(negate: Boolean, text: String, token: Token): ROp = {
    val value = java.lang.Double.parseDouble(text)
    opLiteral(token, new RDoubleLiteral(if (negate) -value else value))
  }

  private def integerLiteral(negate: Boolean, text: String, token: Token, radix: Int = 10): ROp = {
    val value = java.lang.Long.parseLong(text, radix)
    opLiteral(token, new RLongLiteral(if (negate) -value else value))
  }
}
