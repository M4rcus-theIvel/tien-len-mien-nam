package com.m4rcus.tienlenmiennam;

import java.util.Objects;

public class Card implements Comparable<Card> {
    private String suit;
    private String face;

    public Card(String suit, String face) {
        this.suit = suit;
        this.face = face;
    }

    public Card() {
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public int getFaceValue() {
        return switch (face) {
            case "2" -> 15;
            case "Ace" -> 14;
            case "King" -> 13;
            case "Queen" -> 12;
            case "Jack" -> 11;
            default -> Integer.parseInt(face);
        };
    }

    public int getSuitValue() {
        return switch (suit) {
            case "Hearts" -> 4;
            case "Diamonds" -> 3;
            case "Clubs" -> 2;
            case "Spades" -> 1;
            default -> 0;
        };
    }

    public String getDisplayName() {
        return switch (String.valueOf(face)) {
            case "1" -> "Ace";
            case "11" -> "Jack";
            case "12" -> "Queen";
            case "13" -> "King";
            default -> String.valueOf(face);
        }; 
    }

    @Override
    public int compareTo(Card other) {
        int faceComparison = Integer.compare(this.getFaceValue(), other.getFaceValue());
        if (faceComparison != 0) {
            return faceComparison;
        } else {
            return Integer.compare(this.getSuitValue(), other.getSuitValue());
        }
    }

    @Override
    public String toString() {
        return face + " of " + suit;
    }

    @Override public int hashCode() {
        return Objects.hash(suit, face);
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Card)) return false;
        Card other = (Card) obj;
        return this.face.equals(other.face) && this.suit.equals(other.suit);
    }
}
