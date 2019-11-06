/*
 * Copyright (C) 2019 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.core.manager;

import pl.fratik.core.moduly.Modul;
import pl.fratik.core.moduly.ModuleDescription;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public interface ManagerModulow {
    void loadModules();
    boolean startModule(String name);
    boolean stopModule(String name);
    void unload(String name, Boolean remove);
    void load(String path) throws Exception;
    boolean isLoaded(String name);
    boolean isStarted(String name);
    HashMap<String, Modul> getModules();
    ArrayList<String> getStarted();
    File getPath(String modul);
    ModuleDescription getDescription(File file);
}
