package ch.uzh.ifi.hase.soprafs24.game;

import ch.uzh.ifi.hase.soprafs24.entity.Combination;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.Word;
import ch.uzh.ifi.hase.soprafs24.service.CombinationService;
import ch.uzh.ifi.hase.soprafs24.service.PlayerService;
import ch.uzh.ifi.hase.soprafs24.service.WordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public class FusionFrenzyGame extends Game {

    public FusionFrenzyGame(PlayerService playerService, CombinationService combinationService, WordService wordService) {
        super(playerService, combinationService, wordService);
        setup();
    }

    public void setupPlayers(List<Player> players) {
        Word targetWord = wordService.getRandomWordWithinReachability(0.1, 0.3);
        for (Player player : players) {
            player = playerService.resetPlayer(player);
            player.addWords(startingWords);
            player.setTargetWord(targetWord);
        }
    }

    public Word makeCombination(Player player, List<Word> words) {
        if (words.size() == 2) {
            Combination combination = combinationService.getCombination(words.get(0), words.get(1));
            Word result = combination.getResult();
            player.addWord(result);
            return result;
        }

        String errorMessage = "Fusion Frenzy only allows combination of exactly two words!";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
    }

    public boolean winConditionReached(Player player) {
        if (player.getWords().contains(player.getTargetWord())) {
            player.addPoints(1000);
            return true;
        };
        return false;
    }
}
