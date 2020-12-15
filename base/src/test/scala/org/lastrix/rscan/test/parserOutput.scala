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

package org.lastrix.rscan.test

import org.lastrix.rscan.api.parser.{ParserService, antlr4}
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.std.RLangOp
import org.lastrix.rscan.test.Antlr4TestUtils.nullIfEmpty
import org.lastrix.rscan.vfs.VirtualFile
import org.lastrix.rscan.vfs.source.FileSource

import java.io.File

sealed case class ParserOutput(languageName: String, items: Seq[ParserOutputItem])

sealed case class ParserOutputItem(keyText: String, specText: String, children: Seq[ParserOutputItem])

object ParserOutput {
  def build(file: File, language: LanguageRef): ParserOutput = {
    implicit val ps: ParserService = ParserService(language)
    val op = antlr4.parseFile(new VirtualFile(file.getName, new FileSource(file, file.getName)))
    build(op)
  }

  def build(op: RLangOp): ParserOutput = {
    ParserOutput(op.language.name, nullIfEmpty(buildItems(op.children)))
  }

  private def buildItems(children: Seq[ROp]): Seq[ParserOutputItem] = for (child <- children)
    yield ParserOutputItem(child.key.toString, child.specText, nullIfEmpty(buildItems(child.children)))

}
