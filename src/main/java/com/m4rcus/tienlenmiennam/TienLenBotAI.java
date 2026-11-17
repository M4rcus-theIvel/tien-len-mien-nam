package com.m4rcus.tienlenmiennam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TienLenBotAI {

    public static Rule.Move chooseBestMove(List<Card> hand, Rule.Move tableMove) {
        if (hand == null || hand.isEmpty())
            return null;

        List<Card> sortedHand = new ArrayList<>(hand);
        Collections.sort(sortedHand);

        // 1. Sinh tất cả các tổ hợp bài có thể tạo ra từ bài trên tay Bot
        List<Rule.Move> allPossibleMoves = generateAllMoves(sortedHand);

        // 2. Trường hợp BÀN TRỐNG (Bot được mở vòng mới)
        if (tableMove == null) {
            return chooseOpeningMove(allPossibleMoves, sortedHand);
        }

        // 3. Trường hợp CHẶT / ĐÈ BÀI TRÊN BÀN
        List<Rule.Move> validCounters = new ArrayList<>();
        for (Rule.Move move : allPossibleMoves) {
            if (Rule.canBeat(tableMove, move)) {
                validCounters.add(move);
            }
        }

        if (validCounters.isEmpty())
            return null; // Không có bài chặt -> Bỏ lượt

        // Sắp xếp các nước chặt được theo độ mạnh tăng dần để ưu tiên dùng bài nhỏ nhất
        // có thể
        validCounters.sort((m1, m2) -> {
            // Ưu tiên đánh cùng loại bộ trước khi phải xé hàng đặc biệt (Tứ quý / Đôi
            // thông)
            if (m1.type == tableMove.type && m2.type != tableMove.type)
                return -1;
            if (m1.type != tableMove.type && m2.type == tableMove.type)
                return 1;
            // Nếu cùng loại, ưu tiên bộ có lá cao nhất nhỏ hơn (để giữ bài to lại)
            return m1.highestCard.compareTo(m2.highestCard);
        });

        // Chiến thuật giữ bài: Nếu đối thủ chỉ đánh rác/đôi thường (không phải
        // Heo/hàng),
        // Bot sẽ không xé Tứ quý hay Đôi thông ra đánh lẻ
        for (Rule.Move candidate : validCounters) {
            boolean isBomb = (candidate.type == Rule.ComboType.FOUR_OF_A_KIND ||
                    candidate.type == Rule.ComboType.THREE_PAIR ||
                    candidate.type == Rule.ComboType.FOUR_PAIR);
            boolean tableHasTwoOrBomb = (tableMove.highestCard.getFaceValue() == 15 ||
                    tableMove.type == Rule.ComboType.THREE_PAIR ||
                    tableMove.type == Rule.ComboType.FOUR_OF_A_KIND);

            // Nếu dùng hàng để chặt Heo hoặc chặt hàng -> Chặt ngay!
            if (isBomb && tableHasTwoOrBomb) {
                return candidate;
            }
            // Nếu đánh thường cùng loại -> Đánh
            if (!isBomb) {
                return candidate;
            }
        }

        // Nếu chỉ còn 1 bộ duy nhất trên tay để về nhất thì đánh luôn
        if (validCounters.get(0).cards.size() == sortedHand.size()) {
            return validCounters.get(0);
        }

        return null;
    }

    // Chiến thuật khi Bot được đi vòng mới (bàn trống)
    private static Rule.Move chooseOpeningMove(List<Rule.Move> allMoves, List<Card> hand) {
        // Nếu đánh 1 phát hết bài luôn -> Đánh ngay
        for (Rule.Move m : allMoves) {
            if (m.cards.size() == hand.size())
                return m;
        }

        // Ưu tiên xả các bộ dài (Sảnh dài, Tam cô, Đôi) có giá trị nhỏ trước, tránh xả
        // Heo (2) sớm
        allMoves.sort((m1, m2) -> {
            // Hạn chế mở vòng bằng Heo (15) nếu còn bài khác
            boolean m1HasTwo = (m1.highestCard.getFaceValue() == 15);
            boolean m2HasTwo = (m2.highestCard.getFaceValue() == 15);
            if (m1HasTwo != m2HasTwo)
                return m1HasTwo ? 1 : -1;

            // Ưu tiên đánh bộ nhiều lá trước (Sảnh > Tam cô > Đôi > Rác)
            if (m1.cards.size() != m2.cards.size()) {
                return Integer.compare(m2.cards.size(), m1.cards.size());
            }
            // Cùng độ dài thì đánh bộ nhỏ nhất trước
            return m1.highestCard.compareTo(m2.highestCard);
        });

        return allMoves.get(0);
    }

    // =========================================================================
    // BỘ SINH TỔ HỢP BÀI (Rác, Đôi, Tam cô, Tứ quý, Sảnh, 3 Đôi thông, 4 Đôi thông)
    // =========================================================================
    private static List<Rule.Move> generateAllMoves(List<Card> sortedHand) {
        List<Rule.Move> moves = new ArrayList<>();

        // 1. Rác (Single)
        for (Card c : sortedHand) {
            moves.add(new Rule.Move(Rule.ComboType.SINGLE, List.of(c)));
        }

        // Nhóm bài theo giá trị mặt (3 -> 2)
        Map<Integer, List<Card>> byFace = new TreeMap<>();
        for (Card c : sortedHand) {
            byFace.computeIfAbsent(c.getFaceValue(), k -> new ArrayList<>()).add(c);
        }

        // 2. Đôi, Tam cô (Sám), Tứ quý
        for (List<Card> group : byFace.values()) {
            if (group.size() >= 2) {
                // Lấy 2 lá làm Đôi (nếu có 2 đôi trong cùng 1 bộ, ưu tiên đôi có chất cao nhất
                // khi chặt)
                moves.add(new Rule.Move(Rule.ComboType.PAIR, group.subList(0, 2)));
                if (group.size() > 2) {
                    moves.add(new Rule.Move(Rule.ComboType.PAIR, group.subList(group.size() - 2, group.size())));
                }
            }
            if (group.size() >= 3) {
                moves.add(new Rule.Move(Rule.ComboType.TRIPLE, group.subList(0, 3)));
                if (group.size() == 4) {
                    moves.add(new Rule.Move(Rule.ComboType.TRIPLE, group.subList(1, 4)));
                }
            }
            if (group.size() == 4) {
                moves.add(new Rule.Move(Rule.ComboType.FOUR_OF_A_KIND, group));
            }
        }

        // 3. Sảnh (Straight - từ 3 lá trở lên, không tính 2)
        List<Integer> distinctFaces = new ArrayList<>(byFace.keySet());
        distinctFaces.remove(Integer.valueOf(15)); // Loại bỏ Heo (2) khỏi sảnh

        for (int i = 0; i < distinctFaces.size(); i++) {
            List<Card> straightCards = new ArrayList<>();
            // Lấy lá chất nhỏ nhất ở giữa sảnh, nhưng ở lá cuối cùng của sảnh sẽ thử cả lá
            // chất cao nhất
            straightCards.add(byFace.get(distinctFaces.get(i)).get(0));

            for (int j = i + 1; j < distinctFaces.size(); j++) {
                if (distinctFaces.get(j) - distinctFaces.get(j - 1) == 1) {
                    List<Card> nextGroup = byFace.get(distinctFaces.get(j));

                    // Thêm bản sảnh dùng lá chất nhỏ nhất ở đuôi
                    List<Card> s1 = new ArrayList<>(straightCards);
                    s1.add(nextGroup.get(0));
                    if (s1.size() >= 3) {
                        moves.add(new Rule.Move(Rule.ComboType.STRAIGHT, s1));
                    }

                    // Nếu ở đuôi có lá chất cao hơn (ví dụ sảnh tới Át Cơ để đè Át Bích), thêm bản
                    // sảnh chất cao
                    if (nextGroup.size() > 1) {
                        List<Card> s2 = new ArrayList<>(straightCards);
                        s2.add(nextGroup.get(nextGroup.size() - 1));
                        if (s2.size() >= 3) {
                            moves.add(new Rule.Move(Rule.ComboType.STRAIGHT, s2));
                        }
                    }

                    // Tiếp tục nối dài sảnh bằng lá chất nhỏ nhất
                    straightCards.add(nextGroup.get(0));
                } else {
                    break;
                }
            }
        }

        // 4. Đôi thông (3 đôi thông & 4 đôi thông, không tính 2)
        for (int i = 0; i < distinctFaces.size(); i++) {
            if (byFace.get(distinctFaces.get(i)).size() < 2)
                continue;

            List<Card> pairChain = new ArrayList<>(byFace.get(distinctFaces.get(i)).subList(0, 2));
            int pairCount = 1;

            for (int j = i + 1; j < distinctFaces.size(); j++) {
                List<Card> nextGroup = byFace.get(distinctFaces.get(j));
                if (distinctFaces.get(j) - distinctFaces.get(j - 1) == 1 && nextGroup.size() >= 2) {
                    pairCount++;
                    if (pairCount == 3) {
                        // Lấy 2 lá chất cao nhất ở đôi cuối để tối đa khả năng đè 3 đôi thông khác
                        List<Card> threePairs = new ArrayList<>(pairChain);
                        threePairs.addAll(nextGroup.subList(nextGroup.size() - 2, nextGroup.size()));
                        moves.add(new Rule.Move(Rule.ComboType.THREE_PAIR, threePairs));
                    } else if (pairCount == 4) {
                        List<Card> fourPairs = new ArrayList<>(pairChain);
                        fourPairs.addAll(nextGroup.subList(nextGroup.size() - 2, nextGroup.size()));
                        moves.add(new Rule.Move(Rule.ComboType.FOUR_PAIR, fourPairs));
                    }
                    pairChain.addAll(nextGroup.subList(0, 2));
                } else {
                    break;
                }
            }
        }

        return moves;
    }
}