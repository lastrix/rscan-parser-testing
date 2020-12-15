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

import org.antlr.v4.runtime.Token
import org.apache.commons.lang3.StringUtils
import org.lastrix.rscan.api.parser.antlr4.TokenTypeSupport
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.std.{RFoldOp, RNodeOp, StdOpType}
import org.lastrix.rscan.model.operation.{ROp, ROpType}
import org.lastrix.rscan.vfs.VirtualFile

import java.util
import scala.jdk.javaapi.CollectionConverters

trait RScanParser extends TokenTypeSupport {
  /**
   * Returns language used by this parser,
   * Each parser expected to serve single language.
   *
   * @return LanguageRef
   */
  def language: LanguageRef

  /**
   * Returns default rule executed if no value provided by caller
   *
   * @return String
   * @see #invokeDefaultRule
   */
  def defaultRule: String

  /**
   * Returns template rule for processing TemplateLiterals.
   *
   * @return String
   * @see #invokeTemplateRule
   * @throws UnsupportedOperationException if template strings not supported for this language
   */
  def templateRule: String

  /**
   * Returns rule name that must be used for specified operation type
   *
   * @param foldBlock the folded block operation
   * @return String
   * @see #invokeRuleFor
   */
  def ruleNameForFoldedBlock(foldBlock: RFoldOp): String

  /**
   * Returns rule name by its index
   *
   * @param ruleIndex the rule index
   * @return String
   */
  def ruleName(ruleIndex: Int): String

  /**
   * Default entry point for all parsers, this method should be called
   * when file parsing started.
   *
   * @tparam T the result type, should be RLangOperation in most cases
   * @return T
   * @see #defaultRule
   */
  def invokeDefaultRule[T]: T = invokeRule[T](defaultRule)

  /**
   * Parses templates (if supported by language)
   *
   * @tparam T the result type
   * @return operation in most cases
   */
  def invokeTemplateRule[T]: T = invokeRule[T](templateRule)

  /**
   * Invoke rule by name, will throw exception if there is no rule (MethodNotFoundException)
   *
   * @param ruleName the called rule name
   * @tparam T the result type
   * @return T
   */
  def invokeRule[T](ruleName: String): T = resultOfRule[T](ruleName)

  /**
   * Special method for folding support.
   * For example, when operation type is FOLD parser will use rule
   * for blocks in language (depends on actual implementation).
   * The rule name for operation type is resolved by #ruleNameForFoldedBlock .
   *
   * @param foldBlock the folded block operation with token containing list of folded tokens
   * @return the operation
   * @see #ruleNameForFoldedBlock
   */
  def invokeRuleFor(foldBlock: RFoldOp): ROp = resultOfRule[ROp](ruleNameForFoldedBlock(foldBlock))

  private def resultOfRule[T](ruleName: String): T = resultField[T](getClass.getDeclaredMethod(ruleName).invoke(this))

  private def resultField[T](value: AnyRef): T = value.getClass.getDeclaredField("result").get(value).asInstanceOf[T]

  def opNode(statement: Statement, `type`: ROpType, children: Seq[ROp] = Seq.empty): ROp = RNodeOp(`type`, statement, children)

  def opNode(statement: Statement, `type`: ROpType, list: java.util.List[ROp]): ROp = opNode(statement, `type`, asSeq(list))

  def opNode(statement: Statement, `type`: ROpType, op: ROp): ROp = opNode(statement, `type`, Seq(op))

  def opNode(`type`: ROpType, list: java.util.List[ROp]): ROp = opNode(evalStatement(list), `type`, asSeq(list))

  def opNode(`type`: ROpType, ops: Seq[ROp]): ROp = opNode(evalStatement(ops), `type`, ops)

  def opNode(`type`: ROpType, child: ROp): ROp = opNode(child.statement, `type`, Seq(child))

  def opNode(`type`: ROpType, token: Token): ROp = opNode(evalStatement(token), `type`)

  def opCondition(op: ROp): ROp = opNode(StdOpType.CONDITION, op)

  def opCondition(ops: java.util.List[ROp]): ROp = opNode(StdOpType.CONDITION, ops)

  def file: VirtualFile

  def evalStatement(list: java.util.List[ROp]): Statement =
    if (list.isEmpty) throw new IllegalArgumentException("At least single element expected")
    else evalStatement(list.get(0), list.get(list.size() - 1))

  def evalStatement(ops: Seq[ROp]): Statement =
    if (ops.isEmpty) throw new IllegalArgumentException("At least single element expected")
    else evalStatement(ops.head, ops.last)

  def evalStatement(start: ROp, stop: ROp): Statement = Statement(
    file.virtualPath,
    start.statement.startLine,
    start.statement.startLinePosition,
    stop.statement.endLine,
    stop.statement.endLinePosition
  )

  def evalStatement(token: Token): Statement = evalStatement(token, token)

  def evalStatement(startToken: Token, endToken: Token): Statement = Statement(
    file.virtualPath,
    startToken.getLine,
    startToken.getCharPositionInLine,
    endLine(endToken),
    endLinePosition(endToken)
  )

  def endLine(t: Token): Int = t.getLine + StringUtils.countMatches(t.getText, "\n")

  def endLinePosition(t: Token): Int = t.getText.lastIndexOf('\n') match {
    case -1 => t.getCharPositionInLine + t.getText.length
    case idx: Int => t.getText.length - idx - 1
  }

  def blockWrap(op: ROp): ROp = op.`type` match {
    case StdOpType.BLOCK | StdOpType.BLOCK_WRAP | StdOpType.FOLD => op
    case _ => RNodeOp(StdOpType.BLOCK_WRAP, op.statement, op)
  }

  def asSeq(list: util.List[ROp]): Seq[ROp] = CollectionConverters.asScala(list).toSeq

  def unimplemented(): Unit = throw new UnsupportedOperationException("Not implemented")
}
