package com.m4rcus.tienlenmiennam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardPlayer {
    private String name;
    private List<Card> hand;
    private boolean passed;
    private boolean isBot;

    public CardPlayer(String name, boolean isBot) {
        this.name = name;
        this.isBot = isBot;
        this.hand = new ArrayList<>();
        this.passed = false;
    }

    public void getCard(Card card) {
        if (card != null) {
            hand.add(card);
        }
    }

    public void sortCards() {
        Collections.sort(hand);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void removeCard(List<Card> playerCards) {
        hand.removeAll(playerCards);
    }

    public List<Card> getHand() {
        return hand;
    }

    public String getName() {
        return name;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isBot() {
        return isBot;
    }

}
