package ch.uzh.ifi.hase.soprafs24.constant;

public enum Instruction {
    START, STOP, KICK, UPDATE_LOBBY_LIST, UPDATE_LOBBY, UPDATE_PLAYERS, UPDATE_TIMER, ACHIEVEMENT, ABORT_GAME;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
