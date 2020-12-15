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

import java.io.{File, FileNotFoundException}

object ClassPathUtil {
  def getClassJarFileOrFolder(`class`: Class[_]): File = {
    val classNameResource = `class`.getTypeName.replace('.', '/') + ".class"
    val url = `class`.getClassLoader.getResource(classNameResource)
    if (url == null) throw new IllegalStateException(s"Unable to find resource: $classNameResource")
    val path = url.getPath
    val root = path.substring(0, path.length - classNameResource.length)
    val rootFolder = new File(root.replace('/', File.separatorChar))
    PathUtil.assertDirectory(rootFolder)
    rootFolder
  }

  def getResourceFolder(`class`: Class[_], folderName: String): File = {
    val url = `class`.getResource(folderName)
    if (url == null) throw new FileNotFoundException(folderName)
    val folder = new File(url.getPath)
    PathUtil.assertDirectory(folder)
    folder
  }
}
