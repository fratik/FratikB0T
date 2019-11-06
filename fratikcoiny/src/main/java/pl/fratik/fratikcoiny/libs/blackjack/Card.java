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

import lombok.Getter;

@Getter
class Card {
    private final String suit;
    private final String rank;
    private final int value;

    Card(String inputSuit, String inputRank){
        suit = inputSuit;
        rank = inputRank;
        if (rank.equals("A")){
            value = 1; //This will be specified 1 or 11 in Hand.java
        }
        else if (rank.equals("K") || rank.equals("J") || rank.equals("Q")) {
            value = 10;
        }
        else {
            value = Integer.parseInt(rank);
        }

    }

    public String display() {
        String fullSuit;
        String fullRank;

        switch (suit) {
            case "C":
                fullSuit = "♠️";
                break;
            case "D":
                fullSuit = "♦️";
                break;
            case "H":
                fullSuit = "♥️";
                break;
            default:
                fullSuit = "♣️";
                break;
        }

        switch (rank) {
            case "A":
                fullRank = "A";
                break;
            case "K":
                fullRank = "K";
                break;
            case "J":
                fullRank = "J";
                break;
            case "Q":
                fullRank = "Q";
                break;
            default:
                fullRank = rank;
                break;
        }

        return fullRank + fullSuit;
    }

}
