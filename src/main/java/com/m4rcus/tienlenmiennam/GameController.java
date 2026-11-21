package com.m4rcus.tienlenmiennam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.annotation.SessionScope;

@Controller
@SessionScope
public class GameController {
    private boolean justPlayed; // Chỉ true khi vừa có người đánh bài mới xuống bàn

    private List<CardPlayer> players;
    private Rule.Move currentTableMove; // Lưu bộ bài hiện tại trên bàn
    private int currentTurn;
    private int lastPlayedBy;
    private String message;
    private String winner;
    private LinkedList<String> logs;
    private boolean isFirstTurn;

    public GameController() {
        startNewGame();
    }

    private void startNewGame() {
        justPlayed = false;
        Deck deck = new Deck();
        deck.shuffle();
        players = new ArrayList<>();
        players.add(new CardPlayer("Player 1(you)", false));
        players.add(new CardPlayer("Player 2", true));
        players.add(new CardPlayer("Player 3", true));
        players.add(new CardPlayer("Player 4", true));

        for (int i = 0; i < 13; i++) {
            for (CardPlayer p : players) {
                p.getCard(deck.dealCard());
            }
        }
        for (CardPlayer p : players)
            p.sortCards();

        currentTableMove = null;
        logs = new LinkedList<>();
        winner = null;
        currentTurn = findThreeOfSpadesOwner();
        lastPlayedBy = currentTurn;
        message = "Game started! Player " + (currentTurn + 1) + " has the 3 of Spades and will start.";
        addLog(message);
    }

    private void addLog(String text) {
        logs.addFirst(text);
        if (logs.size() > 6) {
            logs.removeLast(); // Giữ 6 dòng nhật ký mới nhất
        }
    }

    private int findThreeOfSpadesOwner() {
        for (int i = 0; i < players.size(); i++) {
            for (Card c : players.get(i).getHand()) {
                if (c.getFace().equals("3") && c.getSuit().equals("Spades"))
                    return i;
            }
        }
        return 0;
    }

    private boolean containsThreeOfSpades(List<Card> cards) {
        for (Card c : cards) {
            if (c.getFace().equals("3") && c.getSuit().equals("Spades")) {
                return true;
            }
        }
        return false;
    }

    // Endpoint cho 1 Bot thực hiện lượt đánh của mình
    @RequestMapping("/bot-step")
    public String botStep() {
        justPlayed = false;
        if (winner != null || !players.get(currentTurn).isBot()) {
            return "redirect:/";
        }

        CardPlayer bot = players.get(currentTurn);
        Rule.Move botMove = TienLenBotAI.chooseBestMove(bot.getHand(), currentTableMove);

        // Nếu là lượt đầu tiên của ván và Bot cầm 3 Bích, đảm bảo nước đi của Bot có
        // chứa 3 Bích
        if (isFirstTurn && botMove != null && !containsThreeOfSpades(botMove.cards)) {
            botMove = Rule.identifyMove(List.of(bot.getHand().get(0))); // Lá đầu tiên sau khi sort luôn là 3 Bích
        }

        if (botMove != null) {
            executeMove(currentTurn, botMove);
        } else {
            bot.setPassed(true);
            message = bot.getName() + " bỏ lượt.";
            addLog(message);
            advanceTurn();
        }
        return "redirect:/";
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("players", players);
        model.addAttribute("tableMove", currentTableMove);
        model.addAttribute("currentTurn", currentTurn);
        model.addAttribute("isBotTurn", winner == null && players.get(currentTurn).isBot());
        model.addAttribute("message", message);
        model.addAttribute("winner", winner);
        model.addAttribute("logs", logs);
        model.addAttribute("justPlayed", justPlayed); // Truyền cờ này sang index.html
        return "index"; // Trỏ tới file index.html
    }

    @PostMapping("/play")
    public String playCards(@RequestParam(value = "selectedIndices", required = false) List<Integer> indices) {
        justPlayed = false;
        if (winner != null || currentTurn != 0)
            return "redirect:/";
        if (indices == null || indices.isEmpty()) {
            message = "Bạn chưa chọn lá bài nào!";
            return "redirect:/";
        }

        CardPlayer human = players.get(0);
        List<Card> selectedCards = new ArrayList<>();
        for (int idx : indices) {
            if (idx >= 0 && idx < human.getHand().size()) {
                selectedCards.add(human.getHand().get(idx));
            }
        }

        Rule.Move move = Rule.identifyMove(selectedCards);
        if (!Rule.canBeat(currentTableMove, move)) {
            message = "Nước đi không hợp lệ hoặc không chặt được bài trên bàn!";
            return "redirect:/";
        }

        executeMove(0, move);
        return "redirect:/";
    }

    @PostMapping("/pass")
    public String passTurn() {
        justPlayed = false;
        if (winner != null || currentTurn != 0)
            return "redirect:/";
        if (currentTableMove == null) {
            message = "Bạn đang được đi vòng mới, không thể bỏ lượt!";
            return "redirect:/";
        }

        players.get(0).setPassed(true);
        message = "Bạn đã bỏ lượt.";
        addLog(message);
        advanceTurn();
        return "redirect:/";
    }

    @RequestMapping("/reset")
    public String reset() {
        startNewGame();
        return "redirect:/";
    }

    private void executeMove(int playerIdx, Rule.Move move) {
        CardPlayer p = players.get(playerIdx);
        p.removeCard(move.cards);
        currentTableMove = move;
        lastPlayedBy = playerIdx;
        justPlayed = true;
        message = p.getName() + " đánh [" + formatComboName(move.type) + "]: " + move.cards;
        addLog(message);
        if (p.getHand().isEmpty()) {
            winner = p.getName();
            message = "CHÚC MỪNG! " + winner + " ĐÃ TỚI NHẤT!";
            addLog(message);
            return;
        }
        advanceTurn();
    }

    private void advanceTurn() {
        do {
            currentTurn = (currentTurn + 1) % 4;
            // Nếu vòng quay lại đúng người đánh cuối cùng -> Xóa bàn, mở vòng mới
            if (currentTurn == lastPlayedBy) {
                currentTableMove = null;
                for (CardPlayer p : players)
                    p.setPassed(false);

                String roundMsg = "--- Tất cả bỏ lượt! " + players.get(currentTurn).getName() + " mở vòng mới ---";
                message = roundMsg;
                addLog(roundMsg);
                break;
            }
        } while (players.get(currentTurn).isPassed());
    }

    private String formatComboName(Rule.ComboType type) {
        return switch (type) {
            case SINGLE -> "Rác";
            case PAIR -> "Đôi";
            case TRIPLE -> "Tam cô";
            case STRAIGHT -> "Sảnh";
            case FOUR_OF_A_KIND -> "Tứ quý";
            case THREE_PAIR -> "3 đôi thông";
            case FOUR_PAIR -> "4 đôi thông";
            default -> "";
        };
    }

    // Gom toàn bộ trạng thái bàn đấu thành JSON
    private Map<String, Object> buildGameState() {
        Map<String, Object> state = new HashMap<>();
        state.put("players", players);
        state.put("tableCards", currentTableMove != null ? currentTableMove.cards : List.of());
        state.put("lastPlayedPlayer", players.get(lastPlayedBy).getName());
        state.put("currentTurn", currentTurn);
        state.put("isBotTurn", winner == null && players.get(currentTurn).isBot());
        state.put("message", message);
        state.put("winner", winner);
        state.put("logs", logs);
        state.put("justPlayed", justPlayed);
        return state;
    }

    @GetMapping("/api/state")
    @ResponseBody
    public Map<String, Object> getState() {
        justPlayed = false;
        return buildGameState();
    }

    @PostMapping("/api/play")
    @ResponseBody
    public Map<String, Object> apiPlay(@RequestBody List<Integer> indices) {
        justPlayed = false;
        if (winner != null || currentTurn != 0)
            return buildGameState();
        if (indices == null || indices.isEmpty()) {
            message = "Bạn chưa chọn lá bài nào!";
            return buildGameState();
        }

        CardPlayer human = players.get(0);
        List<Card> selectedCards = new ArrayList<>();
        for (int idx : indices) {
            if (idx >= 0 && idx < human.getHand().size()) {
                selectedCards.add(human.getHand().get(idx));
            }
        }

        if (isFirstTurn && !containsThreeOfSpades(selectedCards)) {
            message = "Lượt đầu tiên bắt buộc phải đánh bộ bài có chứa lá 3 Bích!";
            return buildGameState();
        }

        Rule.Move move = Rule.identifyMove(selectedCards);
        if (!Rule.canBeat(currentTableMove, move)) {
            message = "Nước đi không hợp lệ hoặc không chặt được bài trên bàn!";
            return buildGameState();
        }

        executeMove(0, move);
        return buildGameState();
    }

    @PostMapping("/api/pass")
    @ResponseBody
    public Map<String, Object> apiPass() {
        justPlayed = false;
        if (winner != null || currentTurn != 0)
            return buildGameState();
        if (currentTableMove == null) {
            message = "Bạn đang được đi vòng mới, không thể bỏ lượt!";
            return buildGameState();
        }

        players.get(0).setPassed(true);
        message = "Bạn đã bỏ lượt.";
        addLog(message);
        advanceTurn();
        return buildGameState();
    }

    @PostMapping("/api/bot-step")
    @ResponseBody
    public Map<String, Object> apiBotStep() {
        justPlayed = false;
        if (winner != null || !players.get(currentTurn).isBot())
            return buildGameState();

        CardPlayer bot = players.get(currentTurn);
        Rule.Move botMove = TienLenBotAI.chooseBestMove(bot.getHand(), currentTableMove);

        if (isFirstTurn && botMove != null && !containsThreeOfSpades(botMove.cards)) {
            botMove = Rule.identifyMove(List.of(bot.getHand().get(0)));
        }

        if (botMove != null) {
            executeMove(currentTurn, botMove);
        } else {
            bot.setPassed(true);
            message = bot.getName() + " bỏ lượt.";
            addLog(message);
            advanceTurn();
        }
        return buildGameState();
    }

    @PostMapping("/api/reset")
    @ResponseBody
    public Map<String, Object> apiReset() {
        startNewGame();
        justPlayed = false;
        return buildGameState();
    }

}