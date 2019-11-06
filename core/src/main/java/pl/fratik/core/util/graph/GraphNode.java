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

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * It represents the node of the graph. It holds a user value that is passed
 * back to the user when a node gets the chance to be evaluated.
 *
 * @param <T>
 * @author nicolae caralicea
 */
@Getter
final class GraphNode<T> {
    @Setter
    private T value;
    private List<GraphNode<T>> comingInNodes;
    private List<GraphNode<T>> goingOutNodes;

    /**
     * Adds an incoming node to the current node
     *
     * @param node The incoming node
     */
    public void addComingInNode(GraphNode<T> node) {
        if (comingInNodes == null)
            comingInNodes = new ArrayList<>();
        comingInNodes.add(node);
    }

    /**
     * Adds an outgoing node from the current node
     *
     * @param node The outgoing node
     */
    public void addGoingOutNode(GraphNode<T> node) {
        if (goingOutNodes == null)
            goingOutNodes = new ArrayList<>();
        goingOutNodes.add(node);
    }
}