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

import org.jetbrains.annotations._
import org.lastrix.rscan.api.model.Disposable

class VirtualFolder(
                     val name: String,
                     var parent: VirtualFolder = null
                   ) extends Disposable {
  private[this] var files = Map.empty[String, VirtualFile]
  private[this] var folders = Map.empty[String, VirtualFolder]

  var assignedProjectKey: String = _

  /**
   * File absolute name, returns name if parent is null
   *
   * @return String
   */
  @NotNull
  def absoluteName: String = if (parent == null) name + SEPARATOR_CHAR else parent.absoluteName + name + SEPARATOR_CHAR

  /**
   * Return VirtualPath object uniquely identifying this file
   *
   * @return VirtualPath
   */
  @NotNull
  lazy val virtualPath: VirtualPath = VirtualPath.forFolderAndName(parent, name)

  /**
   * Dispose all data assigned for this object
   */
  override def dispose(): Unit = {
    for (file <- files.values) file.dispose()
    for (folder <- folders.values) folder.dispose()
  }

  override def toString: String = absoluteName

  def insideProject: Boolean = assignedProjectKey != null || parent != null && parent.insideProject

  ///////////////////////////////// Files operations ///////////////////////////////////////////////////////

  /**
   * Check if this folder has any file
   *
   * @return Boolean
   */
  def hasFiles: Boolean = files.nonEmpty

  /**
   * Return list of all files
   *
   * @return List
   */
  def files(): Seq[VirtualFile] = files.values.toSeq

  /**
   * Add file to this VirtualFolder
   *
   * @param file the file to add
   * @return this
   */
  @NotNull
  def add(@NotNull file: VirtualFile): VirtualFolder = {
    val name = file.name.toLowerCase()
    if (files.contains(name))
      throw new IllegalArgumentException(s"File $name already exist in $absoluteName")

    files += (name -> file)
    this
  }

  /**
   * List files in current directory
   *
   * @param condition filter condition
   * @return Iterable
   */
  @NotNull
  def listFiles(@NotNull condition: VirtualFile => Boolean): Seq[VirtualFile] = files.values.filter(condition).toSeq

  /**
   * List files inside this folder and children
   *
   * @param condition the callback for file filtering
   * @return list
   */
  @NotNull
  def listFilesRecursive(@NotNull condition: VirtualFile => Boolean): List[VirtualFile] = {
    var result = List.empty[VirtualFile]
    for (file <- files.values if condition(file)) result :+= file
    for (folder <- folders.values) result ++= folder.listFilesRecursive(condition)
    result
  }

  /**
   * Visit all files inside folder tree
   *
   * @param callback  the call back for each file to use
   * @param condition folder filter condition, use it to exclude/include only certain paths
   */
  def visitFileRecursive(@NotNull callback: VirtualFile => Unit, @NotNull condition: VirtualFolder => Boolean = _ => true): Unit = {
    for (file <- files.values) callback(file)
    for (folder <- folders.values if condition(folder))
      folder.visitFileRecursive(callback, condition)
  }

  def findFile(name: String): Option[VirtualFile] = {
    if (name == null) return Option.empty
    val cn = toVirtualPath(name)
    val idx = cn.indexOf(SEPARATOR_CHAR)
    if (idx == -1) files.get(cn)
    else folders.get(cn.substring(0, idx)) match {
      case Some(f) => f.findFile(cn.substring(idx + 1))
      case _ => Option.empty
    }
  }

  ///////////////////////////////// Folders operations /////////////////////////////////////////////////////
  /**
   * Check if this folder has any children folders
   *
   * @return Boolean
   */
  def hasFolders: Boolean = folders.nonEmpty

  /**
   * Get all children folders
   *
   * @return List
   */
  def folders(): List[VirtualFolder] = folders.values.toList

  def findFolder(nameOrSubPath: String): Option[VirtualFolder] = findFolderImpl(toVirtualPath(nameOrSubPath))

  @scala.annotation.tailrec
  private def findFolderImpl(nameOrSubPath: String): Option[VirtualFolder] = {
    val idx = nameOrSubPath.indexOf(SEPARATOR_CHAR)
    if (idx != -1) {
      val fName = nameOrSubPath.substring(0, idx)
      folders().find(_.name == fName) match {
        case Some(f) => f.findFolderImpl(nameOrSubPath.substring(idx + 1))
        case None => None
      }
    }
    else folders().find(_.name == nameOrSubPath)
  }

  /**
   * Create folders inside current
   *
   * @param path   the path with separator char
   * @param filter file filter for folder exclusion/inclusion
   * @return newly created folder or null
   */
  @Nullable
  def mkdirs(@NotNull path: String, @NotNull filter: FileFilter = EmptyFileFilter): Option[VirtualFolder] = {
    var tmp = path
    while (tmp.length > 1 && tmp.charAt(0) == SEPARATOR_CHAR)
      tmp = tmp.substring(1)
    while (tmp.length > 1 && tmp.charAt(tmp.length - 1) == SEPARATOR_CHAR)
      tmp = tmp.substring(0, tmp.length - 1)

    if (tmp.length == 1 && tmp.charAt(0) == SEPARATOR_CHAR) return Some(this)

    val idx = tmp.indexOf(SEPARATOR_CHAR)
    if (idx == -1) return mkdir(tmp, filter)
    mkdir(tmp.substring(0, idx), filter) match {
      case Some(folder) => folder.mkdirs(tmp.substring(idx + 1), filter)
      case None => None
    }
  }

  /**
   * Create new directory inside current one, if already exist - the value returned
   *
   * @param name   new folder name
   * @param filter file filter for conditional creation
   * @return created VirtualFolder or null
   */
  @Nullable
  def mkdir(@NotNull name: String, @NotNull filter: FileFilter = EmptyFileFilter): Option[VirtualFolder] = {
    val key = name.toLowerCase()
    if (!filter.isAllowed(absoluteName, key, directory = true)) return None
    folders.get(key) match {
      case Some(folder) => Some(folder)
      case None =>
        val child = new VirtualFolder(name, this)
        folders += (key -> child)
        Some(child)
    }
  }

  /**
   * Visit all folders inside current one
   *
   * @param condition the condition for entering folder
   */
  def visitRecursive(@NotNull condition: VirtualFolder => Boolean): Unit =
    for (folder <- folders.values if condition(folder)) folder.visitRecursive(condition)

  /**
   * Add file to this VirtualFolder
   *
   * @param folder the folder to add
   * @return this
   */
  @NotNull
  def add(@NotNull folder: VirtualFolder): VirtualFolder = {
    val name = folder.name.toLowerCase()
    if (folders.contains(name))
      throw new IllegalArgumentException(s"Folder $name already exist in $absoluteName")
    folders += (name -> folder)
    this
  }
}
