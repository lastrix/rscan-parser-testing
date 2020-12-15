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

import org.apache.commons.lang3.StringUtils
import org.lastrix.rscan.api.util.Json
import org.lastrix.rscan.api.util.PathUtil.{ensureDirectory, relativeName, updateFileContent}
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.test.Antlr4TestUtils._
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.time.{Duration, Instant}
import scala.annotation.tailrec

object ParserTestSuiteGenerator {
  private val log: Logger = LoggerFactory.getLogger("app.suite")

  def main(args: Array[String]): Unit = {
    val cfg = parseArguments(args.toList)
    val p = new Processor(cfg.language)
    parseParserSuites(collectSuites(cfg), p)
  }

  def parseParserSuites(suites: Seq[Suite], processor: Processor): Unit = for (suite <- suites) {
    val start = Instant.now
    for (fi <- suite.fileInfos) processor.forFile(suite, fi)
    val elapsed = Duration.between(start, Instant.now)
    log.info(s"(Parser) Suite '${suite.name}' parsed successfully in ${elapsed.toMillis} ms")
  }

  def collectSuites(suiteConfig: SuiteConfig): Seq[Suite] = {
    val suites = collectGroupSuites(suiteConfig.language, suiteConfig.workDir, suiteConfig.extensions)
    suiteConfig.selected match {
      case Some(name) =>
        log.info(s"Selected suite: $name")
        suites.filter(_.name == name)
      case None => suites
    }
  }

  def parseArguments(args: List[String]): SuiteConfig = {
    if (args.isEmpty) {
      throw new IllegalStateException("Arguments missing")
    }
    var extensions: Seq[String] = Seq.empty
    var language: LanguageRef = null
    var workDir: File = null
    var selected: Option[String] = None

    @tailrec
    def parseImpl(a: List[String]): Unit = if (a.nonEmpty) a match {
      case "-d" :: dirPath :: tail =>
        workDir = new File(dirPath)
        parseImpl(tail)

      case "-l" :: langName :: tail =>
        language = LanguageRef(langName)
        parseImpl(tail)

      case "-s" :: selectedName :: tail =>
        selected = Some(selectedName)
        parseImpl(tail)

      case "-e" :: extList :: tail =>
        extensions ++= StringUtils.split(extList, ';')
        parseImpl(tail)

      case _ => throw new UnsupportedOperationException
    }

    parseImpl(args)
    SuiteConfig(language, workDir, extensions, selected)
  }

  class Processor(val language: LanguageRef) {
    def forFile(suite: Suite, fi: FileInfo): Unit = {
      val lexerFolder = new File(suite.workDir, LexerSuiteDir)
      ensureDirectory(lexerFolder)
      val parserFolder = new File(suite.workDir, ParserSuiteDir)
      ensureDirectory(parserFolder)
      val workName = relativeName(suite.workDir, fi.file)
      updateFileContent(new File(lexerFolder, workName + SuiteFileExt), Json(LexerOutput.build(fi.file, language)))
      updateFileContent(new File(parserFolder, workName + SuiteFileExt), Json(ParserOutput.build(fi.file, language)))
    }
  }

  case class SuiteConfig(language: LanguageRef, workDir: File, extensions: Seq[String], selected: Option[String])

}
