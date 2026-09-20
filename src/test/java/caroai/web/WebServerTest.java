package caroai.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebServerTest {
    private static WebServer server;
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll static void start() throws Exception { server = new WebServer("127.0.0.1", 0); }
    @AfterAll static void stop() { if (server != null) server.close(); }

    private HttpResponse<String> post(String body) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(uri("/api/game"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + server.port() + path); }

    @Test void servesBundledWebAssetsAndRejectsUnknownPaths() throws Exception {
        for (String path : new String[]{"/", "/app.js", "/style.css", "/game-file.js", "/sound.js"}) {
            var response = CLIENT.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isNotBlank();
            assertThat(response.headers().firstValue("Content-Security-Policy")).isPresent();
        }
        var response = CLIENT.send(HttpRequest.newBuilder(uri("/pom.xml")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test void humanOReceivesExactlyOneComputerOpening() throws Exception {
        var response = post("mode=ai&human=O&difficulty=EASY&moves=");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"moves\":[[25,25]]", "\"turn\":\"O\"");
    }

    @Test void servesHealthAndOriginalBinaryMoveSound() throws Exception {
        var health = CLIENT.send(HttpRequest.newBuilder(uri("/healthz")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).isEqualTo("ok");
        var audio = CLIENT.send(HttpRequest.newBuilder(uri("/move.wav")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(audio.statusCode()).isEqualTo(200);
        assertThat(audio.headers().firstValue("Content-Type")).contains("audio/wav");
        try (var original = WebServer.class.getResourceAsStream("/caroai/audio/move.wav")) {
            assertThat(audio.body()).isEqualTo(original.readAllBytes());
        }
    }

    @Test void humanXReceivesOneLegalReplyWithoutChangingTheirStone() throws Exception {
        var response = post("mode=ai&human=X&difficulty=EASY&moves=25,25");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"moves\":[[25,25],[", "\"turn\":\"X\"");
    }

    @Test void localGameAlternatesWithoutComputerReply() throws Exception {
        var response = post("mode=local&moves=0,0;49,49;1,0");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"moves\":[[0,0],[49,49],[1,0]]", "\"turn\":\"O\"");
    }

    @Test void rejectsDuplicateOutOfBoundsMalformedAndInvalidSettings() throws Exception {
        for (String body : new String[]{"moves=0,0;0,0", "moves=-1,0", "moves=50,0", "moves=nope",
                "moves=1,1;", "human=Z", "difficulty=SUPER", "mode=online", "action=invalid", "human=X&human=O"}) {
            assertThat(post(body).statusCode()).as(body).isEqualTo(400);
        }
    }

    @Test void detectsWinnerAndRejectsAnyMovesAfterWin() {
        String win = "0,0;0,1;1,0;1,1;2,0;2,1;3,0;3,1;4,0";
        String response = WebGame.respond(Map.of("mode", "local", "moves", win));
        assertThat(response).contains("\"winner\":\"X\"", "\"winningLine\":[[");
        assertThatThrownBy(() -> WebGame.respond(Map.of("moves", win + ";4,1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void computerTakesWinningMoveForEitherColour() {
        String xWin = WebGame.respond(Map.of("human", "O", "difficulty", "EASY", "moves",
                "0,0;0,2;1,0;2,2;2,0;4,2;3,0;6,2"));
        assertThat(xWin).contains("\"winner\":\"X\"", "[4,0]");
        String oWin = WebGame.respond(Map.of("human", "X", "difficulty", "EASY", "moves",
                "0,2;0,0;2,2;1,0;4,2;2,0;6,2;3,0;8,2"));
        assertThat(oWin).contains("\"winner\":\"O\"", "[4,0]");
    }

    @Test void replayAfterUndoClearsWinnerAndPreservesOpeningForHumanO() {
        String response = WebGame.respond(Map.of("human", "O", "action", "state", "moves", "25,25"));
        assertThat(response).contains("\"moves\":[[25,25]]", "\"winner\":null", "\"turn\":\"O\"");
    }

    @Test void simultaneousGamesHaveNoSharedBoard() throws Exception {
        var first = CLIENT.sendAsync(HttpRequest.newBuilder(uri("/api/game"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("human=O&difficulty=HARD&moves=25,25;26,25"))
                .build(), HttpResponse.BodyHandlers.ofString());
        var second = post("mode=local&moves=0,0");
        assertThat(second.body()).contains("\"moves\":[[0,0]]");
        assertThat(first.get().body()).contains("\"moves\":[[25,25],[26,25],[");
        assertThat(post("mode=local&moves=").body()).contains("\"moves\":[]");
    }

    @Test void rejectsCrossOriginRequestsAndOversizedBodies() throws Exception {
        var response = CLIENT.send(HttpRequest.newBuilder(uri("/api/game"))
                .header("Origin", "https://unrelated.example")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("moves=")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(post("moves=" + "x".repeat(32768)).statusCode()).isEqualTo(413);
    }

    @Test void hintUsesCurrentSideWithoutAddingAStone() throws Exception {
        var response = post("action=hint&mode=local&difficulty=EASY&moves=0,2;0,0;2,2;1,0;4,2;2,0;6,2;3,0;8,2");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"hint\":[4,0]", "\"winner\":null", "\"turn\":\"O\"",
                "\"moves\":[[0,2],[0,0],[2,2],[1,0],[4,2],[2,0],[6,2],[3,0],[8,2]]");
    }

    @Test void hintForHumanOBlocksXAndDoesNotUseTheWrongPerspective() throws Exception {
        var response = post("action=hint&human=O&difficulty=EASY&moves=0,0;0,2;1,0;2,2;2,0;4,2;3,0");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"hint\":[4,0]", "\"turn\":\"O\"", "\"winner\":null");
    }

    @Test void hintsHandleEmptyFinishedAndComputerTurn() throws Exception {
        assertThat(post("action=hint&human=X&moves=").body()).contains("\"hint\":[25,25]", "\"moves\":[]");
        assertThat(post("action=hint&human=O&moves=").statusCode()).isEqualTo(400);
        assertThat(post("action=hint&mode=local&moves=0,0;0,1;1,0;1,1;2,0;2,1;3,0;3,1;4,0").body())
                .contains("\"hint\":null", "\"winner\":\"X\"");
    }

    @Test void restoredComputerTurnIsValidatedBeforeContinuing() throws Exception {
        var validated = post("action=state&human=X&difficulty=EASY&moves=25,25");
        assertThat(validated.body()).contains("\"moves\":[[25,25]]", "\"turn\":\"O\"");
        var continued = post("action=reply&human=X&difficulty=EASY&moves=25,25");
        assertThat(continued.body()).contains("\"moves\":[[25,25],[", "\"turn\":\"X\"");
    }
}
