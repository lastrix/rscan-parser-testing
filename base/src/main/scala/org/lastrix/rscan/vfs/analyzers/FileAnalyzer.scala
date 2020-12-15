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

import org.jetbrains.annotations.Nullable
import org.lastrix.rscan.vfs.VirtualFileSystem

import java.io.File

trait FileAnalyzer extends Comparable[FileAnalyzer] {
  val priority: Int

  def isApplicable(file: File): Boolean = isApplicable(file, file.getName)

  def isApplicable(file: File, @Nullable actualName: String = null): Boolean

  def analyze(vfs: VirtualFileSystem, file: File): Unit = analyze(vfs, file, file.getName)

  def analyze(vfs: VirtualFileSystem, file: File, @Nullable actualName: String = null): Unit

  override def compareTo(t: FileAnalyzer): Int = Integer.compare(priority, t.priority)
}
