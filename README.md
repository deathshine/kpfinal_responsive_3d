# CubeRol

git commit -m "Update README"
git commit -m "Update README"
git commit -m "Update README"
git commit -m "Update README"

CubeRoll is a GameStudio puzzle game for the final KP submission. The player rolls a cube on a board, uses one-time painter tiles, opens matching color gates and reaches the finish tile.

## Final submission 3 status

Implemented functionality for the browser demo:

- fully playable CubeRoll game in a graphical web interface,
- login/logout by nickname stored in the HTTP session,
- anonymous users can view top scores, comments and average rating,
- only logged users can add comments, set/update their own rating and save score,
- score is saved automatically after a solved level,
- top score table is visible from the game page,
- comments are visible from the game page and can be added by logged users,
- rating can be added/updated by logged users; own rating and average rating are shown,
- keyboard controls are supported in the web UI (`W/A/S/D` and arrow keys),
- REST endpoints for score, comment and rating are still available for the previous submission.

## How to run the web game

Start the Spring Boot server:

```bash
mvn spring-boot:run -Dspring-boot.run.main-class=sk.tuke.gamestudio.server.GameStudioServer
```

Open the web game:

```text
http://localhost:8081/cuberoll
```

The local H2 console is available at:

```text
http://localhost:8081/h2-console
```

Default H2 settings:

```text
JDBC URL: jdbc:h2:file:./gamestudio;MODE=PostgreSQL;AUTO_SERVER=TRUE
User: sa
Password: <empty>
```

## Demo flow for the 3-minute presentation

1. Open `/cuberoll` as an anonymous user and show that top scores, comments and average rating are visible.
2. Log in with a nickname.
3. Play level 1 and solve it. One solution is:

```text
S S E E W W S E N E E E
```

4. Show that the score was saved automatically after solving the level.
5. Add a comment.
6. Add a rating, then change the rating to demonstrate rating update.
7. Show the same services through REST requests from `requests.http` if needed.

## REST service examples

Prepared requests are in:

```text
requests.http
```

Main endpoints:

```text
GET  /api/score/{game}
POST /api/score
GET  /api/comment/{game}
POST /api/comment
GET  /api/rating/{game}
GET  /api/rating/{game}/{player}
POST /api/rating
```

## Project structure

```text
sk.tuke.gamestudio.entity                 JPA entities: Score, Comment, Rating
sk.tuke.gamestudio.game.cuberoll.core     CubeRoll game logic
sk.tuke.gamestudio.game.cuberoll.consoleui Console UI from previous submissions
sk.tuke.gamestudio.service.jdbc           JDBC services
sk.tuke.gamestudio.service.jpa            JPA services used by the web server
sk.tuke.gamestudio.service.rest           REST clients used by the console client
sk.tuke.gamestudio.server                 Spring Boot server
sk.tuke.gamestudio.server.web             Browser GUI controller/session
sk.tuke.gamestudio.server.webservice      REST controllers
```

## Tests

Run:

```bash
mvn test
```

Included tests:

- core cube/field tests,
- JDBC service tests,
- JPA service tests,
- REST service tests,
- web GUI controller tests for login, rating/comment services and automatic score saving.

## Video

Add the final 3-minute video link here before uploading to GitLab:

```text
PASTE_VIDEO_LINK_HERE
```

## Author

Daniil Zhylenko
