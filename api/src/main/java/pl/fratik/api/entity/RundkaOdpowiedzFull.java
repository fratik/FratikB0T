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

package pl.fratik.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RundkaOdpowiedzFull implements RundkaOdpowiedz {
    private final String userId;
    private final int rundka;
    private final String obowiazki;
    private final Plec plec;
    private final String konfiguracja;
    private final String ustawPowitaniePozegnanie;
    private final String usunPowitaniePozegnanie;
    private final Integer pozUpr;
    private final CzestotliwoscPrzebywania czestotliwoscPrzebywania;
    private final Boolean aktywnoscFdev;
    private final String dlaczegoGa;
    private final String dlaczegoWybrac;
    private String messageId;
    private Oceny oceny;
}
