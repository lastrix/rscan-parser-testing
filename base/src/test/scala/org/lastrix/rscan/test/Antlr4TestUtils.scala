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

import org.antlr.v4.runtime.Token
import org.junit.jupiter.params.provider.Arguments
import org.lastrix.rscan.api.util.ClassPathUtil
import org.lastrix.rscan.api.util.PathUtil.{collectFilesRecursive, relativeName}
import org.lastrix.rscan.lang.LanguageRef

import java.io.File
import scala.jdk.javaapi.CollectionConverters

object Antlr4TestUtils {
  val SuiteFileExt: String = ".td.json"
  val LexerSuiteDir: String = ".suite/lexer".replace('/', File.separatorChar)
  val ParserSuiteDir: String = ".suite/parser".replace('/', File.separatorChar)

  def testConfig(`type`: String, suite: Suite, file: File): File =
    new File(new File(suite.workDir, `type`), relativeName(suite.workDir, file) + SuiteFileExt)

  def filesFromResourceDirectory(language: LanguageRef, resourceDirectory: String, `class`: Class[_], extensions: Seq[String] = Seq("")): java.util.stream.Stream[Arguments] = {
    val workDir = ClassPathUtil.getResourceFolder(`class`, resourceDirectory)
    val suites = collectGroupSuites(language, workDir, extensions)
    CollectionConverters.asJava(suites.map(x => Arguments.of(x))).stream()
  }

  def tokenStatement(t: Token): String = s"${t.getLine}:${t.getCharPositionInLine}[${t.getText.length}]"

  def nullIfEmpty[T >: Null <: Seq[_]](arg: T): T =
    if (arg.isEmpty) null else arg

  def nullOrString(arg: Any): String =
    if (arg == null) null else String.valueOf(arg)

  def collectGroupSuites(language: LanguageRef, workDir: File, extensions: Seq[String]): Seq[Suite] = {
    val files = listCandidates(workDir, extensions)
    if (files == null || files.isEmpty) throw new IllegalStateException(s"No files or folders found inside: $workDir")
    else {
      var result = Seq.empty[Suite]
      files.foreach(file =>
        if (file.isDirectory) result ++= suitesFromGroupFolder(language, workDir, file, extensions)
        else result :+= suite(relativeName(workDir, file), language, file.getParentFile, workDir, Seq(file))
      )
      result
    }
  }

  def suite(name: String, language: LanguageRef, folder: File, workDir: File, files: Seq[File]): Suite = {
    val infos = for (file <- files) yield FileInfo(file, relativeName(folder, file))
    Suite(name, language, folder, workDir, infos)
  }

  private def suitesFromGroupFolder(language: LanguageRef, workDir: File, groupDir: File, extensions: Seq[String]): Seq[Suite] = {
    val files = listCandidates(groupDir, extensions)
    if (files == null || files.isEmpty) Seq.empty
    else {
      var result = Seq.empty[Suite]
      files.foreach(file =>
        if (file.isDirectory) result :+= suiteFromFolder(language, workDir, file, extensions)
        else result :+= suite(relativeName(workDir, file), language, file.getParentFile, workDir, Seq(file))
      )
      result
    }
  }

  private def listCandidates(workDir: File, extensions: Seq[String]): Seq[File] =
    workDir.listFiles(file => !file.getName.startsWith(".") && (file.isDirectory || hasExtension(file, extensions))).toSeq

  def hasExtension(file: File, extensions: Seq[String]): Boolean =
    extensions.exists(extension => file.getName.toLowerCase.endsWith(extension))

  private def suiteFromFolder(language: LanguageRef, workDir: File, folder: File, extensions: Seq[String]): Suite =
    suite(relativeName(workDir, folder), language, folder, workDir, collectFilesRecursive(folder, file => hasExtension(file, extensions)))

  sealed case class FileInfo(file: File, actualName: String)

  sealed case class Suite(name: String, language: LanguageRef, basePath: File, workDir: File, fileInfos: Seq[FileInfo])

}
