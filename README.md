# WOMBO COMBO

Inspired by the widely popular browser game [Infinite Craft](https://neal.fun/infinite-craft/), we wanted to develop an online multiplayer word-merging game. Infinite Craft is a sandbox game using generative AI software where the player is given four starting elements: fire, wind, water, and earth, which can be combined to create new elements. We adapted and expanded this idea by adding objectives, a scoring system, and multiplayer capabilities.

## Technologies used

* Java
* Gradle
* Spring Boot
* PostgreSQL
* Websockets
* Google Cloud (App Engine)
* Google Vertex API (external API)

## Structure and Components

In the implementation, elements that can be combined are also called "words". So, Wombo Combo is an element-merging/word-merging game.

### Lobby management

Responsible for:

* managing players (joining, leaving)
* lobby settings (name, visibility)
* triggering gmae start and coordinating the gameplay
* broadcasting the game result to the players.

Main class:

* [LobbyService](https://github.com/sopra-fs24-group-41/sopra-fs24-group-41-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/LobbyService.java): implements the lobby management logic.

Important helper classes:

* LobbyController (REST controller)
* PlayerService (logic used for managing individual players)

### User management

Responsible for:

* User registration
* User login
* User profile information (favorite word, number of wins and other statistics)
* Editing user data

Main class:

* [UserService](https://github.com/sopra-fs24-group-41/sopra-fs24-group-41-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/UserService.java): implements the user management logic.

Other important classes:

* UserController (REST controller)

### Game logic

Responsible for:

* Initializing the game
* Playing moves (making combinations)
* Conditions for ending the game, criteria for determining the winner
* Game timer (how much time is left for in the current round)
* Updating player statistics (number of wins, combinations made etc.)
* Different game modes available

Main class:

* [GameService](https://github.com/sopra-fs24-group-41/sopra-fs24-group-41-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameService.java): implements the higher-level game logic.

Other important classes:

* WordService: Manages words, selecting the target word (word that needs to be reached by the players in order to win)
* CombinationService: Generates word combinations (e.g., `word1` + `word2` = `resultWord`), saves them to the database.
* APIService: Manages calls to the external API for determining the resulting word for a combination (e.g., fetches the result "steam" for "water" + "fire").

These three components work together to allow for playing games: LobbyService manages the players and triggers the game start; GameService initializes the player information relevant for the gameplay; GameService processes the higher-level logic (such as the `play` primitive), which uses lower-level logic implemented using WordService and CombinationService. Registered users are managed with UserService, and they have statistics tracking how many wins and losses they have after games, as well as achievements triggered during gameplay.

## Launch & Deployment

### Prerequisites

#### Spring Boot

Spring Boot is the backbone technology in this project.

Getting started with Spring Boot:
-   Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/index.html
-   Guides: http://spring.io/guides
    -   Building a RESTful Web Service: http://spring.io/guides/gs/rest-service/
    -   Building REST services with Spring: https://spring.io/guides/tutorials/rest/

#### Google Vertex API

Google Cloud and Vertex API:
* Set up a Google Cloud Account to use Google's VertexAI API
* [Here a guide for the setup](https://cloud.google.com/vertex-ai/docs/start/introduction-unified-platform)
* [Here documentation on VertexAI](https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/text-chat)
* In general, you have to first setup a Google Cloud Service Account, which you have to anyway whenever you do projects with Google Cloud. Setup your service account and download the required credentials. [Guide](https://cloud.google.com/iam/docs/service-account-creds). It will be a JSON file that you will have to store as an environment variable. If that is done, then you can treat VertexAI like any other library you import for Java, add the depedency in your `build.gradle` with:
```java
implementation 'com.google.cloud:google-cloud-aiplatform:3.35.0'
```
Then the relevant methods will automatically verify your credentials in your environment and run. If you this for deployment, set it up via GitHub secrets and the `main.yml`

Note: the LLM used by our application can be changed in `APIService.java`.

### Development

After setting up the Vertex API credentials, you can now build and run the application.

#### Build

You can use the local Gradle Wrapper to build the application.
-   macOS: `./gradlew`
-   Linux: `./gradlew`
-   Windows: `./gradlew.bat`

More Information about [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) and [Gradle](https://gradle.org/docs/).


Then, you can build the project using:

```bash
./gradlew build
```

#### Run

```bash
./gradlew bootRun
```

You can verify that the server is running by visiting `localhost:8080` in your browser.

#### Test

```bash
./gradlew test
```

Useful guide on testing: [link](https://www.baeldung.com/spring-boot-testing).

#### Development Mode

You can start the backend in development mode, this will automatically trigger a new build and reload the application once the content of a file has been changed.

Start two terminal windows and run:

`./gradlew build --continuous`

and in the other one:

`./gradlew bootRun`

If you want to avoid running all tests with every change, use the following command instead:

`./gradlew build --continuous -xtest`

#### API Endpoint Testing with Postman

We recommend using [Postman](https://www.getpostman.com) to test your API Endpoints.

#### Debugging

If something is not working and/or you don't know what is going on, we recommend using a debugger and step-through the process step-by-step.

To configure a debugger for SpringBoot's Tomcat servlet (i.e. the process you start with `./gradlew bootRun` command), do the following:

1. Open Tab: **Run**/Edit Configurations
2. Add a new Remote Configuration and name it properly
3. Start the Server in Debug mode: `./gradlew bootRun --debug-jvm`
4. Press `Shift + F9` or the use **Run**/Debug "Name of your task"
5. Set breakpoints in the application where you need it
6. Step through the process one step at a time

#### How to do releases

We have set up a Github Actions Workflow that automatically deploys whenever code is pushed on the main branch.

* Workflow file for main commits: `main.yml`
* Workflow file for pull requests (for running tests): `pr.yml`

## Roadmap

Further versions of Wombo Combo could include these functionalities:

* Spectator mode, where clients can spectate games
* Further achievements
* Difficulty setting for target words

## Authors

* **Timon Leupp** - [Tmmn](https://github.com/Tmmn)
* **Lucas Timothy Leo Bär** - [Grizzlytron](https://github.com/Grizzlytron)
* **Jacqueline Ulken** - [JacquelineUlken](https://github.com/JacquelineUlken)
* **Rosan Shanmuganathan** - [na50r](https://github.com/na50r)
* **Alexandru-Mihai Hurjui** - [Aquamarine-12](https://github.com/Aquamarine-12)

See also the list of [contributors](https://github.com/sopra-fs24-group-41/sopra-fs24-group-41-server/graphs/contributors) who participated in this project.

### Acknowledgments

* Inspired by [Infinite Craft](https://neal.fun/infinite-craft/) made by [Neil Agarwal](https://nealagarwal.me/)
* And last but not least, thanks to the Team of SOPRA for enabling this opportunity and providing us with the means to develop **WOMBO COMBO!**

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/sopra-fs24-group-41/sopra-fs24-group-41-server/blob/main/LICENSE) file for details.
