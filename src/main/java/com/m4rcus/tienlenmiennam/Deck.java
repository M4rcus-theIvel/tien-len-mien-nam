package com.m4rcus.tienlenmiennam;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    public List<Card> deckCards;

    public Deck() {
        deckCards = new ArrayList<>();
        String[] suits = { "Hearts", "Diamonds", "Clubs", "Spades" };
        String[] faces = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace" };

        for (String suit : suits) {
            for (String face : faces) {
                deckCards.add(new Card(suit, face));
            }
        }
    }

    public void shuffle() {
        java.util.Collections.shuffle(deckCards);
    }

    public Card dealCard() {
        if (!deckCards.isEmpty()) {
            return deckCards.remove(deckCards.size() - 1);
        } else {
            return null;
        }
    }

    public int remainingCards() {
        return deckCards.size();
    }

}
