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

package org.lastrix.rscan.vfs

import org.jetbrains.annotations.{NotNull, Nullable}
import org.lastrix.rscan.vfs.VirtualFileSystem.analyzerFor
import org.lastrix.rscan.vfs.analyzers.{FileAnalyzer, FileAnalyzerFactory}

import java.io.{Closeable, File}
import java.util.ServiceLoader
import scala.jdk.javaapi.CollectionConverters

class VirtualFileSystem(val fileFilter: FileFilter = EmptyFileFilter) extends Closeable {
  val root = new VirtualFolder("")

  def register(@NotNull file: File, @Nullable actualName: String = null): Unit = {
    val tmp = if (actualName == null) file.getName else actualName
    analyzerFor(file, tmp).analyze(this, file, tmp)
  }

  override def close(): Unit = root.dispose()
}

object VirtualFileSystem {
  val Analyzers: Seq[FileAnalyzer] = CollectionConverters
    .asScala(ServiceLoader.load[FileAnalyzerFactory](classOf[FileAnalyzerFactory], getClass.getClassLoader))
    .map(_.newInstance())
    .toSeq
    .sorted

  def analyzerFor(file: File, actualName: String): FileAnalyzer = Analyzers.find(_.isApplicable(file, actualName)) match {
    case Some(a) => a
    case None => throw new IllegalArgumentException(s"No analyzer found for file: '$file' with actualName: '$actualName'")
  }
}
