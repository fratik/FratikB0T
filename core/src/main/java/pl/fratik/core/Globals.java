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

package pl.fratik.core;

@SuppressWarnings("all")
public class Globals {
    private Globals() {}
    public static volatile long clientId = 0;
    public static volatile int shardCount = 1;
    public static volatile boolean production = false;
    public static volatile boolean inFratikDev = false;
    public static volatile boolean inDiscordBotsServer = false;
    public static volatile boolean debug = false;
    public static volatile boolean noVoteLock = false;
    public static volatile long ownerId;
    public static volatile String owner;
    public static volatile long permissions;
    public static boolean wylaczanie = false;
}
