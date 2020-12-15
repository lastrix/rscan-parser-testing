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

package org.lastrix.rscan.api.util

import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}

object PathUtil {
  def relativeName(basePath: File, file: File): String = {
    val a = file.getAbsolutePath
    val b = basePath.getAbsolutePath
    if (!a.startsWith(b))
      throw new IllegalArgumentException(s"File $file does not belong to $basePath")
    a.substring(b.length + 1)
  }

  def assertFile(file: File): Unit =
    if (!file.exists() || file.isDirectory)
      throw new IllegalArgumentException(s"Must exist and not directory: ${file.getAbsolutePath}")

  def assertDirectory(file: File): Unit =
    if (!file.exists() || !file.isDirectory)
      throw new IllegalArgumentException(s"Must exist and be directory: ${file.getAbsolutePath}")

  def ensureDirectory(file: File): Unit =
    if (!file.exists() && !file.mkdirs() || !file.isDirectory)
      throw new IllegalArgumentException(s"Unable to mkdirs or not directory: ${file.getAbsolutePath}")

  def collectFilesRecursive(folder: File, filter: File => Boolean = _ => true): List[File] = {
    var result = List.empty[File]
    val files = folder.listFiles()
    if (files != null) files.foreach(file => {
      if (file.isDirectory) result ++= collectFilesRecursive(file, filter)
      else if (filter(file)) result :+= file
    })
    result
  }

  def updateFileContent(file: File, content: String, charset: Charset = StandardCharsets.UTF_8): Boolean = {
    if (file.exists()) {
      if (file.isDirectory) throw new IllegalArgumentException(s"Is directory: ${file.getAbsolutePath}")
      val prev = FileUtils.readFileToString(file, charset);
      if (prev.equals(content)) return false
    }
    FileUtils.writeStringToFile(file, content, charset);
    true
  }
}
