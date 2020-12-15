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

package antlr4


import java.util.stream.Collectors

class GrammarDetector {
    static final String EXTENSION = ".g4"

    static List<GrammarItem> locate(File targetFolder, File folder) {
        List<File> files = new ArrayList<>()
        findGrammars(folder, files)
        return files.stream()
                .map { e -> new GrammarItem(targetFolder, e) }
                .collect(Collectors.toList())
    }

    static findGrammars(File folder, List<File> output) {
        def files = folder.listFiles()
        if (files == null)
            return

        files.each { file ->
            if (file.isDirectory())
                findGrammars(file, output)
            else if (hasExtension(file, EXTENSION))
                output.add(file)
        }
    }

    static boolean hasExtension(File file, String ext) {
        def name = file.getName()
        return name.toLowerCase().endsWith(ext.toLowerCase())
    }
}
