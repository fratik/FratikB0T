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

package pl.fratik.core.util.graph;

/**
 * The main mechanism used for notifying the outside of the fact that a node
 * just got its evaluation
 *
 * @author nicolae caralicea
 *
 * @param <T>
 */
public interface NodeValueListener<T> {
    /**
     *
     * The callback method used to notify the fact that a node that has assigned
     * the nodeValue value just got its evaluation
     *
     * @param nodeValue
     *            The user set value of the node that just got the evaluation
     */
    void evaluating(T nodeValue);
}
