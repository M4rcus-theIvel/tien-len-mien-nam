package com.m4rcus.tienlenmiennam;
import java.util.ArrayList;
import java.util.List;

public class Rule {
    public enum ComboType {
        INVALID,
        SINGLE, 
        PAIR, 
        TRIPLE, 
        FOUR_OF_A_KIND, 
        STRAIGHT, // sảnh
        THREE_PAIR, // ba đôi thông
        FOUR_PAIR, // bốn đôi thông
       
        
    }

    public static class Move {
        public ComboType type;
        public List<Card> cards;
        public Card highestCard;

        public Move(ComboType type, List<Card> cards) {
            this.type = type;
            this.cards = new ArrayList<>(cards);
            java.util.Collections.sort(this.cards);
            this.highestCard = this.cards.isEmpty() ? null : this.cards.get(this.cards.size() - 1);
        }
    }

        // Nhận diện bộ bài người chơi chọn
        public static Move identifyMove(List<Card> selectedCards) {
            if (selectedCards == null || selectedCards.isEmpty()) {
                return new Move(ComboType.INVALID, List.of());
            }
            List<Card> sortedCards = new ArrayList<>(selectedCards);
            java.util.Collections.sort(sortedCards);
            int n = sortedCards.size();
            // Kiểm tra từng loại bộ bài
            if (n == 1) return new Move(ComboType.SINGLE, sortedCards);

            if (n == 2 && isSameFace(sortedCards)) return new Move(ComboType.PAIR, sortedCards);
            if (n == 3 && isSameFace(sortedCards)) return new Move(ComboType.TRIPLE, sortedCards);
            if (n == 4 && isSameFace(sortedCards)) return new Move(ComboType.FOUR_OF_A_KIND, sortedCards);

            // Kiểm tra Đôi thông (6 lá = 3 đôi, 8 lá = 4 đôi)
            if (n >= 6 && n % 2 == 0 && isConsecutivePairs(sortedCards)) {
                if (n == 6) return new Move(ComboType.THREE_PAIR, sortedCards);
                if (n == 8) return new Move(ComboType.FOUR_PAIR, sortedCards);
            }
            if (n >= 3 && isStraight(sortedCards)) return new Move(ComboType.STRAIGHT, sortedCards);

            return new Move(ComboType.INVALID, sortedCards);
        }

        // Kiểm tra bài mới (next) có đánh đè được bài trên bàn (current) không
    public static boolean canBeat(Move current, Move next) {
        if (next.type == ComboType.INVALID) return false;
        if (current == null) return true; // Bàn trống, đánh bộ hợp lệ nào cũng được

        // 1. Cùng loại bộ và cùng số lượng lá -> So lá cao nhất
        if (current.type == next.type && current.cards.size() == next.cards.size()) {
            return next.highestCard.compareTo(current.highestCard) > 0;
        }

        // 2. Luật chặt Heo và hàng đặc biệt
        boolean isSingleTwo = (current.type == ComboType.SINGLE && current.highestCard.getFaceValue() == 15);
        boolean isPairTwo = (current.type == ComboType.PAIR && current.highestCard.getFaceValue() == 15);

        if (isSingleTwo) {
            return next.type == ComboType.THREE_PAIR || next.type == ComboType.FOUR_OF_A_KIND || next.type == ComboType.FOUR_PAIR;
        }
        if (isPairTwo) {
            return next.type == ComboType.FOUR_OF_A_KIND || next.type == ComboType.FOUR_PAIR;
        }
        if (current.type == ComboType.THREE_PAIR) {
            return next.type == ComboType.FOUR_OF_A_KIND || next.type == ComboType.FOUR_PAIR;
        }
        if (current.type == ComboType.FOUR_OF_A_KIND) {
            return next.type == ComboType.FOUR_PAIR;
        }

        return false;
    }








    private static boolean isSameFace(List<Card> cards) {
        int val = cards.get(0).getFaceValue();
        for (Card c : cards) {
            if (c.getFaceValue() != val) return false;
        }
        return true;
    }

    private static boolean isStraight(List<Card> sorted) {
        // Sảnh không được chứa Heo (faceValue == 15)
        if (sorted.get(sorted.size() - 1).getFaceValue() == 15) return false;
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i + 1).getFaceValue() - sorted.get(i).getFaceValue() != 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean isConsecutivePairs(List<Card> sorted) {
        if (sorted.get(sorted.size() - 1).getFaceValue() == 15) return false;
        for (int i = 0; i < sorted.size(); i += 2) {
            if (sorted.get(i).getFaceValue() != sorted.get(i + 1).getFaceValue()) return false;
            if (i + 2 < sorted.size() && sorted.get(i + 2).getFaceValue() - sorted.get(i).getFaceValue() != 1) {
                return false;
            }
        }
        return true;
    }

}

