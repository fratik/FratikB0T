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

import java.util.ArrayList;
import java.util.Random;

class Deck {
    private ArrayList<Card> deckContents;
    private int count;

    public Deck() {
        deckContents = new ArrayList<>();
        //Clubs, Diamonds, Hearts, Spades
        String[] suits = {"C", "D", "H", "S"};
        for (String suit : suits) {
            String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
            for (String rank : ranks) {
                deckContents.add(new Card(suit,rank));
            }
        }
        count = deckContents.size();
    }

    public void deal(Hand player) {
        Card dealtCard = deckContents.get(0);
        deckContents.remove(0);
        player.take(dealtCard);
        count = deckContents.size();
    }

    public void shuffle() {
        ArrayList<Card> newDeckContents = new ArrayList<>();
        Random rand = new Random();
        int count = deckContents.size();
        int track;
        for (int i=0; i < count; i++){
            track = count - i;
            int index = rand.nextInt(track);
            Card a = deckContents.get(index);
            deckContents.remove(index);
            newDeckContents.add(i, a);
        }
        deckContents = newDeckContents;
    }

    public void displayDeck() {
        for (Card card: deckContents ){
            System.out.println(card.display());
        }
    }


}
