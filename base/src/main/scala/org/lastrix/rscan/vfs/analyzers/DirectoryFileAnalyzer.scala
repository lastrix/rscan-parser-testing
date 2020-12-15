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

package org.lastrix.rscan.vfs.analyzers

import org.lastrix.rscan.vfs.source.FileSource
import org.lastrix.rscan.vfs.{VirtualFile, VirtualFileSystem, VirtualFolder}
import org.slf4j.LoggerFactory

import java.io.File

object DirectoryFileAnalyzer extends FileAnalyzer {
  private val log = LoggerFactory.getLogger(this.getClass)

  override val priority: Int = Int.MaxValue - 1

  override def isApplicable(file: File, actualName: String): Boolean = file.isDirectory

  override def analyze(vfs: VirtualFileSystem, file: File, actualName: String): Unit = {
    if (!file.isDirectory)
      throw new IllegalStateException("Unable to handle: " + file)
    log.debug("Registering directory: {}", file.getAbsolutePath)
    collectFilesRecursive(vfs, file, vfs.root)
  }

  private def collectFilesRecursive(vfs: VirtualFileSystem, directory: File, folder: VirtualFolder): Unit = {
    val files = directory.listFiles
    if (files == null) return
    val folderPath = folder.absoluteName
    for (file <- files) {
      if (file.isDirectory) {
        folder.mkdirs(file.getName, vfs.fileFilter) match {
          case Some(vf) => collectFilesRecursive(vfs, file, vf)
          case None =>
        }
      } else if (vfs.fileFilter.isAllowed(folderPath, file.getName)) {
        val source = new FileSource(file, folderPath + org.lastrix.rscan.vfs.SEPARATOR_CHAR + file.getName)
        folder.add(new VirtualFile(file.getName, source, folder))
      }
    }
  }
}

sealed class DirectoryFileAnalyzerFactory extends FileAnalyzerFactory {
  override def newInstance(): FileAnalyzer = DirectoryFileAnalyzer
}
