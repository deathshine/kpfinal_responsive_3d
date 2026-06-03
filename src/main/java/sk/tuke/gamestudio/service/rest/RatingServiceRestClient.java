package sk.tuke.gamestudio.service.rest;

import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.service.RatingException;
import sk.tuke.gamestudio.service.RatingService;

public class RatingServiceRestClient implements RatingService {
    private static final String URL = "http://localhost:8081/api/rating";
    private final RestTemplate restTemplate;

    public RatingServiceRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void setRating(Rating rating) throws RatingException {
        try {
            restTemplate.postForEntity(URL, rating, Void.class);
        } catch (Exception e) {
            throw new RatingException("Cannot set rating via REST.", e);
        }
    }

    @Override
    public int getAverageRating(String game) throws RatingException {
        try {
            Integer value = restTemplate.getForObject(URL + "/" + game, Integer.class);
            return value == null ? 0 : value;
        } catch (Exception e) {
            throw new RatingException("Cannot read average rating via REST.", e);
        }
    }

    @Override
    public int getRating(String game, String player) throws RatingException {
        try {
            Integer value = restTemplate.getForObject(URL + "/" + game + "/" + player, Integer.class);
            return value == null ? 0 : value;
        } catch (Exception e) {
            throw new RatingException("Cannot read player rating via REST.", e);
        }
    }

    @Override
    public void reset() throws RatingException {
        throw new UnsupportedOperationException("Reset is not supported via REST");
    }
}
