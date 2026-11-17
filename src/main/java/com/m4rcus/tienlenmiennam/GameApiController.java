package com.m4rcus.tienlenmiennam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*") // Cho phép React/Vue ở port khác gọi API này
public class GameApiController {

    private Deck deck;
    private CardPlayer player;

    public GameApiController() {
        reset();
    }

    // API lấy trạng thái hiện tại: GET http://localhost:8080/api/game/status
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("remainingCards", deck.remainingCards());
        response.put("hand", player.getHand());
        return response;
    }

    // API rút 1 lá bài: POST http://localhost:8080/api/game/deal
    @PostMapping("/deal")
    public Map<String, Object> deal() {
        Card card = deck.dealCard();
        if (card != null) {
            player.getCard(card);
        }
        return getStatus(); // Trả thẳng dữ liệu JSON mới nhất về
    }

    // API xếp bài: POST http://localhost:8080/api/game/sort
    @PostMapping("/sort")
    public List<Card> sort() {
        player.sortCards();
        return player.getHand();
    }

    // API chơi lại: POST http://localhost:8080/api/game/reset
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        deck = new Deck();
        deck.shuffle();
        player = new CardPlayer("Player 1", false);
        return getStatus();
    }
}
