package sk.tuke.gamestudio.service.rest;

import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.service.ScoreException;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScoreServiceRestClient implements ScoreService {
    private static final String URL = "http://localhost:8081/api/score";
    private final RestTemplate restTemplate;

    public ScoreServiceRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void addScore(Score score) throws ScoreException {
        try {
            restTemplate.postForEntity(URL, score, Void.class);
        } catch (Exception e) {
            throw new ScoreException("Cannot add score via REST.", e);
        }
    }

    @Override
    public List<Score> getTopScores(String game) throws ScoreException {
        try {
            Score[] scores = restTemplate.getForObject(URL + "/" + game, Score[].class);
            return scores == null ? Collections.emptyList() : Arrays.asList(scores);
        } catch (Exception e) {
            throw new ScoreException("Cannot read scores via REST.", e);
        }
    }

    @Override
    public void reset() throws ScoreException {
        throw new UnsupportedOperationException("Reset is not supported via REST");
    }
}
