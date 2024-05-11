package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Instruction;
import ch.uzh.ifi.hase.soprafs24.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.websocket.InstructionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class LobbyService {
    private final Logger log = LoggerFactory.getLogger(LobbyService.class);

    private final LobbyRepository lobbyRepository;

    private final PlayerService playerService;

    private final SimpMessagingTemplate messagingTemplate;

    private final PlatformTransactionManager transactionManager;

    private static final String MESSAGE_LOBBY_BASE = "/topic/lobbies";
    private static final String MESSAGE_LOBBY_GAME = "/topic/lobbies/%d/game";

    @Autowired
    public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository, PlayerService playerService,
                        SimpMessagingTemplate messagingTemplate, PlatformTransactionManager transactionManager) {
        this.lobbyRepository = lobbyRepository;
        this.playerService = playerService;
        this.messagingTemplate = messagingTemplate;
        this.transactionManager = transactionManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleCheckLobbyStillActiveStartup() {
        scheduleCheckLobbyStillActive(1, 3); // change values here to adjust timings
    }

    public void scheduleCheckLobbyStillActive(long thresholdMinutes, long periodMinutes) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        Runnable checkLobbies = () -> checkAndRemoveInactiveLobbies(thresholdMinutes);

        long initialDelay = 0;
        executorService.scheduleAtFixedRate(checkLobbies, initialDelay, periodMinutes, TimeUnit.SECONDS);
    }

    public void checkAndRemoveInactiveLobbies(long thresholdMinutes) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                List<Lobby> allLobbies = lobbyRepository.findAll();
                for (Lobby lobby : allLobbies) {
                    if (lobby.getLastModified() == null) continue;
                    long minutesDifference = ChronoUnit.MINUTES.between(lobby.getLastModified(), LocalDateTime.now());
                    if (minutesDifference >= thresholdMinutes) {
                        removeLobby(lobby);
                        messagingTemplate.convertAndSend(String.format(MESSAGE_LOBBY_GAME, lobby.getCode()),
                                new InstructionDTO(Instruction.KICK, "The lobby was closed due to inactivity"));
                        log.debug("Lobby with code {} was last active on {} and was closed due to inactivity", lobby.getCode(), lobby.getLastModified());
                    }
                }
                return null;
            });
            transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                messagingTemplate.convertAndSend(MESSAGE_LOBBY_BASE, getPublicLobbies().stream().map(DTOMapper.INSTANCE::convertEntityToLobbyGetDTO).toList());
                return null;
            });
        } catch(Exception e) {
            log.error("Could not check lobbies for inactivity: ", e);
        }
    }

    public List<Lobby> getPublicLobbies() {
        return lobbyRepository.findAllByPublicAccess(true);
    }

    public boolean allPlayersReady(Lobby lobby) {
        return lobby.getPlayers().stream().allMatch(player -> player.getStatus() == PlayerStatus.READY);
    }

    public boolean allPlayersLost(Lobby lobby) {
        return lobby.getPlayers().stream().allMatch(player -> player.getStatus() == PlayerStatus.LOST);
    }

    public Lobby getLobbyByCode(long code) {
        Lobby foundLobby = lobbyRepository.findByCode(code);
        if (foundLobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("lobby with code %d does not exist", code));
        }
        return foundLobby;
    }

    public Player createLobbyFromUser(User user, Boolean publicAccess) {
        String lobbyName = user.getUsername() + "'s Lobby";
        Lobby lobby = new Lobby(generateLobbyCode(), lobbyName);
        lobby.setStatus(LobbyStatus.PREGAME);
        Player player = new Player(UUID.randomUUID().toString(), user.getUsername(), lobby);

        player.setOwnedLobby(lobby);
        lobby.setOwner(player);
        lobby.setPlayers(List.of(player));
        lobby.setPublicAccess(Objects.requireNonNullElse(publicAccess, true));
        lobby.setGameTime(0);

        Lobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        user.setPlayer(savedLobby.getOwner());
        savedLobby.getOwner().setUser(user);

        log.debug("created new lobby {}", lobby);
        log.debug("created new player from user and set as lobby owner{}", player);
        return savedLobby.getOwner();
    }

    public Player joinLobbyFromUser(User user, long lobbyCode) {
        Lobby foundLobby = getLobbyByCode(lobbyCode);
        if (foundLobby.getStatus() != LobbyStatus.PREGAME) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "this lobby does not accept new players, wait until the game is finished");
        }

        Player player = new Player(UUID.randomUUID().toString(), user.getUsername(), foundLobby);
        player.setUser(user);
        foundLobby.addPlayer(player);
        user.setPlayer(player);

        log.debug("user joined -> updated lobby {}", foundLobby);
        log.debug("created new player from user {}", player);
        return player;
    }

    public Player joinLobbyAnonymous(String playerName,long lobbyCode) {
        Lobby foundLobby = getLobbyByCode(lobbyCode);
        if (foundLobby.getStatus() != LobbyStatus.PREGAME) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "this lobby does not accept new players, wait until the game is finished");
        }

        Player player = new Player(UUID.randomUUID().toString(), playerName, foundLobby);
        foundLobby.addPlayer(player);
        log.debug("updated lobby {}", foundLobby);
        log.debug("created new anonymous player from player name {}", player);
        return player;
    }

    public Map<String, Boolean> updateLobby(Lobby lobby, LobbyPutDTO lobbyPutDTO) {
        Map<String, Boolean> updates = new HashMap<>();
        updates.put("mode", false);
        updates.put("name", false);
        updates.put("publicAccess", false);
        updates.put("gameTime", false);

        if (lobbyPutDTO.getMode() != null && !lobbyPutDTO.getMode().equals(lobby.getMode())) {
            lobby.setMode(lobbyPutDTO.getMode());
            updates.put("mode", true);
        }
        if (lobbyPutDTO.getName() != null && !Objects.equals(lobbyPutDTO.getName(), lobby.getName())) {
            lobby.setName(lobbyPutDTO.getName());
            updates.put("name", true);
        }
        if (lobbyPutDTO.getPublicAccess() != null && !Objects.equals(lobbyPutDTO.getPublicAccess(), lobby.getPublicAccess())) {
            lobby.setPublicAccess(lobbyPutDTO.getPublicAccess());
            updates.put("publicAccess", true);
        }

        if (lobbyPutDTO.getGameTime()!=null && !Objects.equals(lobbyPutDTO.getGameTime(), lobby.getGameTime())) {
            lobby.setGameTime(lobbyPutDTO.getGameTime());
            updates.put("gameTime", true);
        }
        return updates;
    }

    public void removeLobby(Lobby lobby) {
        if (lobby.getOwner() != null) {
            lobby.getOwner().setOwnedLobby(null);
            lobby.setOwner(null);
        }
        if (lobby.getPlayers() != null) {
            List<Player> playerList = lobby.getPlayers();
            for (int i = playerList.toArray().length-1; i>=0; i--) {
                playerService.removePlayer(playerList.get(i));
            }
        }
        lobbyRepository.delete(lobby);
        log.debug("successfully deleted lobby {}", lobby);
    }

    private long generateLobbyCode() {
        long code = ThreadLocalRandom.current().nextLong(1000, 10000);
        while (lobbyRepository.existsByCode(code)) {
            code = ThreadLocalRandom.current().nextLong(1000, 10000);
        }
        return code;
    }
}
