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

import org.jetbrains.annotations.NotNull
import org.lastrix.rscan.api.model.Disposable
import org.lastrix.rscan.vfs.source.Source

sealed class VirtualFile
(
  val name: String,
  val source: Source,
  var parent: VirtualFolder = null,
) extends Disposable
  with Comparable[VirtualFile] {

  /**
   * File absolute name, returns name if parent is null
   *
   * @return String
   */
  @NotNull
  def absoluteName: String = if (parent == null) name else parent.absoluteName + name

  @NotNull
  lazy val virtualPath: VirtualPath = VirtualPath.forFolderAndName(parent, name)

  def extension: String = {
    val idx = name.lastIndexOf('.')
    if (idx == -1) "" else name.substring(idx + 1)
  }

  def hasExtension(@NotNull ext: String): Boolean = name.toLowerCase.endsWith(ext)

  /**
   * Dispose all data assigned for this object
   */
  override def dispose(): Unit = {
    source match {
      case disposable: Disposable => disposable.dispose()
      case _ =>
    }
    custom = Map.empty
  }

  override def compareTo(t: VirtualFile): Int = absoluteName.compareTo(t.absoluteName)

  override def toString: String = absoluteName

  ///////////////////////////////// Custom Data operations ///////////////////////////////////////////////////
  private var custom: Map[String, Object] = Map.empty

  def customData(): Map[String, Object] = this.synchronized {
    custom
  }

  /**
   * Set custom data value
   *
   * @param key   the key, system properties starts with $, you should not use this symbol as first one
   * @param value the kryo-serializable value
   * @param force value overwrite is not permitted by default, if you set force to true, than
   *              all invocations of this method will overwrite stored value
   */
  def customData(key: String, value: Object, force: Boolean = false): Unit = this.synchronized {
    if (!force && custom.contains(key)) {
      if (custom.get(key) == value) return
      throw new IllegalArgumentException("Unable to overwrite value")
    }
    custom += (key -> value)
  }

  /**
   * Get custom data
   *
   * @param key the key
   * @return
   */
  def getCustomData(key: String): Option[Object] = this.synchronized {
    custom.get(key)
  }

  ///////////////////////////////// File flags /////////////////////////////////////////////////////////////////////////
  private var flags: Int = 0

  def consume(): Unit = flags = flags | 1

  def isConsumed: Boolean = (flags & 1) != 0
}
