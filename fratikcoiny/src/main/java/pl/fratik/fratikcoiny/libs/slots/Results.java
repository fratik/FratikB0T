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

package pl.fratik.fratikcoiny.libs.slots;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Results {

    private final List<Line> linie;

    public Results(List<Line> linie) {
        this.linie = linie;
    }

    public int totalPoints() {
        return linie.stream().reduce(0, (total, line) -> total + line.getPoints(), Integer::sum);
    }

    public int winCount() {
        return (int) linie.stream().filter(Line::isWon).count();
    }

    public String visualize() {
        return visualize(false);
    }

    private String visualize(boolean includeDiagonals) {
        List<Line> lines = linie.stream().filter(line -> !line.diagonal).collect(Collectors.toList());
        String visual = lines.stream().map(line -> line.symbols.stream().map(SlotSymbol::getDisplay).collect(Collectors.joining(" "))).collect(Collectors.joining("\n"));

        if (includeDiagonals) {
            List<Line> diagonals = linie.stream().filter(line -> line.diagonal).collect(Collectors.toList());
            visual += "\n\n";
            visual += diagonals.stream().map(line -> line.symbols.stream().map(SlotSymbol::getDisplay).collect(Collectors.joining(" "))).collect(Collectors.joining("\n"));
        }

        return visual;
    }

    public static class Line {

        private final List<SlotSymbol> symbols;
        private final boolean diagonal;

        public Line(List<SlotSymbol> symbols) {
            this(symbols, false);
        }

        public Line(List<SlotSymbol> symbols, boolean diagonal) {
            this.symbols = symbols;
            this.diagonal = diagonal;
        }

        boolean isWon() {
            Optional<SlotSymbol> symboleNieWildy = symbols.stream().filter(s -> !s.isWildcard()).findFirst();
            if (!symboleNieWildy.isPresent()) return true;

            List<SlotSymbol> reszta = symbols.stream().filter(s -> !s.isWildcard() &&
                    !s.getName().equals(symboleNieWildy.get().getName())).collect(Collectors.toList());
            return reszta.isEmpty();
        }

        int getPoints() {
            if (!this.isWon()) return 0;
            return this.symbols.stream().reduce(0, (integer, ssymbol) -> integer + ssymbol.getPoints(), Integer::sum);
        }
    }
}
