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

package pl.fratik.fratikcoiny.libs.blackjack;

import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Hand {
    private final String nameOfPlayer;
    private final Member player;
    private final ArrayList<Card> handContents;
    private int netValue;

    public Hand(String player) {
        nameOfPlayer = player;
        this.player = null;
        handContents = new ArrayList<>();
        netValue = 0;
    }

    public Hand(Member player) {
        nameOfPlayer = player.getUser().getAsTag();
        this.player = player;
        handContents = new ArrayList<>();
        netValue = 0;
    }

    public String display() {
        return handContents.stream().map(Card::display).collect(Collectors.joining(" - "));
    }

    public String hiddenDisplay() {
        return handContents.get(0).display();
    }

    public void take(Card dealtCard) {
        handContents.add(dealtCard);
        getNetValue(dealtCard);
    }

    private void getNetValue(Card dealtCard) {
        netValue += calculateNetValue(dealtCard);
    }

    private int calculateNetValue(Card dealtCard) {
        int value = 0;
        if (dealtCard.getRank().equals("A")) {
            if (netValue <= 10) {
                value += 11;
            } else {
                value += 1;
            }
        } else {
            value += dealtCard.getValue();
        }
        return value;
    }

    public List<Card> getHandContents() {
        return handContents;
    }

    public int getNetValue() {
        return getNetValue(false);
    }

    public int getNetValue(boolean withoutHidden) {
        return withoutHidden ? calculateNetValue(handContents.get(0)) : netValue;
    }
}
