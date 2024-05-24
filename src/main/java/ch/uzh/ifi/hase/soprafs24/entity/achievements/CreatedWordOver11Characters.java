package ch.uzh.ifi.hase.soprafs24.entity.achievements;

import ch.uzh.ifi.hase.soprafs24.entity.Combination;
import ch.uzh.ifi.hase.soprafs24.entity.Player;

import javax.persistence.Entity;


@Entity
public class CreatedWordOver11Characters extends Achievement {

    public CreatedWordOver11Characters() {
        setTitle("Supercalifragilisticexpialidocious");
        setDescription("Unlock this achievement by creating a word almost as long as Mary Poppins' favorite. That's right, a word longer than 11 characters!");
        setProfilePicture("mary_poppins");
        setHidden(true);
    }

    public boolean unlockConditionFulfilled(Player player, Combination combination) {
        return combination.getResult().getName().length() > 11;
    }
}
