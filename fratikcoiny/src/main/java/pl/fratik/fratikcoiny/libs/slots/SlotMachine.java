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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SlotMachine {
    private final int size;
    private final List<SlotSymbol> symbols;
    private final Callable<Number> losujacaFunkcja;

    public SlotMachine(int size, List<SlotSymbol> symbols) {
        this(size, symbols, Math::random);
    }

    private SlotMachine(int size, List<SlotSymbol> symbols, Callable<Number> losujacaFunkcja) {
        if (size % 2 == 0 || size < 3) throw new NumberFormatException("Slot machine size must be an odd number, 3 or higher.");
        if (symbols == null || symbols.isEmpty()) throw new NumberFormatException("There must be at least one symbol.");
        this.size = size;
        this.symbols = symbols;
        this.losujacaFunkcja = Objects.requireNonNull(losujacaFunkcja);
    }

    public Results play() {
        List<SlotSymbol> wybrane = new ArrayList<>();
        int lacznaWaga = symbols.stream().reduce(0, (total, symbol) -> total + symbol.getWeight(), Integer::sum);

        for (int i = 0; i < Math.pow(size, 2); i++) {
            double rand;
            try {
                rand = losujacaFunkcja.call().doubleValue() * lacznaWaga;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int sum = 0;

            for (SlotSymbol symbol : symbols) {
                sum += symbol.getWeight();
                if (rand <= sum) {
                    wybrane.add(symbol);
                    break;
                }
            }
        }

        List<List<SlotSymbol>> lines = new ArrayList<>();

        for (int i = 0; i < wybrane.size() / size; i++) {
            lines.add(wybrane.subList(i * this.size, (i + 1) * this.size));
        }

        {
            List<SlotSymbol> temp = new ArrayList<>();
            for (int i = 0; i < wybrane.size(); i++) {
                if ((i + this.size + 1) % (this.size + 1) == 0) temp.add(wybrane.get(i));
            }
            lines.add(temp);
        }

        {
            List<SlotSymbol> temp = new ArrayList<>();
            List<SlotSymbol> ss = wybrane.subList(this.size - 1, wybrane.size());
            for (int i = 0; i < ss.size(); i++) {
                if (i % (this.size - 1) == 0) temp.add(ss.get(i));
//            lines.add(wybrane.stream().filter((s, i) -> (i + this.size + 1) % (this.size + 1) === 0)))
            }
            lines.add(temp.subList(0, temp.size() - 1));
        }

        AtomicInteger i = new AtomicInteger();
        return new Results(lines.stream().map(line -> {
            Results.Line lajn = new Results.Line(line, i.get() == lines.size() - 1 || i.get() == lines.size() - 2);
            i.getAndIncrement();
            return lajn;
        }).collect(Collectors.toList()));

    }
}
