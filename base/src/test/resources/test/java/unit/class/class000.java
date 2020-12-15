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

package example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.*;

class Example {
    private List<String> names = new ArrayList<>();

    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public void addName(String name) {
        this.names.add(name);
    }
}
