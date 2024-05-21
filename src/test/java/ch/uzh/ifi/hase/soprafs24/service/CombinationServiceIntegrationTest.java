package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Combination;
import ch.uzh.ifi.hase.soprafs24.entity.Word;
import ch.uzh.ifi.hase.soprafs24.repository.CombinationRepository;
import ch.uzh.ifi.hase.soprafs24.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
class CombinationServiceIntegrationTest {

    @Qualifier("combinationRepository")
    @Autowired
    private CombinationRepository combinationRepository;

    @Qualifier("wordRepository")
    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private WordService wordService;

    @Autowired
    private CombinationService combinationService;

    @BeforeEach
    public void setup() {
        combinationRepository.deleteAll();
        wordRepository.deleteAll();
    }

    @Test
    void getCombination_manyCombinations_success() {
        Combination combo1 = combinationService.getCombination(new Word("fire"), new Word("water"));
        Combination combo2 = combinationService.getCombination(new Word("fire"), new Word("water"));
        Combination combo3 = combinationService.getCombination(new Word("fire"), new Word("earth"));

        assertEquals(combo1.getWord1().getName(), combo2.getWord1().getName());
        assertEquals(combo1.getWord2().getName(), combo2.getWord2().getName());
        assertEquals(combo1.getResult().getName(), combo2.getResult().getName());

        assertEquals(combo1.getWord1().getName(), combo3.getWord1().getName());
        assertNotEquals(combo1.getWord2().getName(), combo3.getWord2().getName());
    }

    @Test
    void makeCombinations_multipleCombinations_success() {
        ArrayList<Word> startingWords = new ArrayList<>(Arrays.asList(new Word("water"), new Word("earth"),
                new Word("fire"), new Word("air")));

        for (Word word : startingWords) {
            Word foundWord = wordService.getWord(word);
            foundWord.setDepth(0);
            foundWord.setReachability(1e6);
            wordService.saveWord(foundWord);
        }

        combinationService.makeCombinations(5);

        assertEquals(5, combinationRepository.findAll().size());
    }

    @Test
    void makeCombinations_manyCombinations_testVertexAPI() {
        ArrayList<Word> startingWords = new ArrayList<>(Arrays.asList(new Word("water"), new Word("earth"),
                new Word("fire"), new Word("air")));

        for (Word word : startingWords) {
            Word foundWord = wordService.getWord(word);
            foundWord.setDepth(0);
            foundWord.setReachability(1e6);
            wordService.saveWord(foundWord);
        }

        combinationService.makeCombinations(150);
        int warningCount = 0;
        int max = 0;
        HashMap<String, Integer> wordCounts = new HashMap<>();

        for (Combination combination : combinationRepository.findAll()) {
            String resultWord = combination.getResult().getName();
            if (resultWord.contains(combination.getWord1().getName()) || resultWord.contains(combination.getWord2().getName())) {
                System.out.println("##### WARNING");
                warningCount++;
            }

            if(resultWord.length()>max){
                max = resultWord.length();
            }

            if(wordCounts.containsKey(resultWord)){
                wordCounts.put(resultWord, wordCounts.get(resultWord)+1);
            }

            if(!wordCounts.containsKey(resultWord)){
                wordCounts.put(resultWord, 1);
            }

            else
                System.out.println("#####");
            System.out.println("i1: " + combination.getWord1().getName());
            System.out.println("i2: " + combination.getWord2().getName());
            System.out.println("res: " + resultWord);
        }
        System.out.println("Number of Result Words that used Input Words: " + warningCount);
        System.out.println("Max Number of Chars in Result Word: " + max);
        System.out.println("Number of Unique Words: " + wordCounts.size());
        System.out.println("Word Counts:");
        for (String key : wordCounts.keySet()) {
            System.out.println(key + ": " + wordCounts.get(key));
        }
    }
}
