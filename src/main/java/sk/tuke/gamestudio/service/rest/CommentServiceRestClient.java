package sk.tuke.gamestudio.service.rest;

import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.service.CommentException;
import sk.tuke.gamestudio.service.CommentService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommentServiceRestClient implements CommentService {
    private static final String URL = "http://localhost:8081/api/comment";
    private final RestTemplate restTemplate;

    public CommentServiceRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void addComment(Comment comment) throws CommentException {
        try {
            restTemplate.postForEntity(URL, comment, Void.class);
        } catch (Exception e) {
            throw new CommentException("Cannot add comment via REST.", e);
        }
    }

    @Override
    public List<Comment> getComments(String game) throws CommentException {
        try {
            Comment[] comments = restTemplate.getForObject(URL + "/" + game, Comment[].class);
            return comments == null ? Collections.emptyList() : Arrays.asList(comments);
        } catch (Exception e) {
            throw new CommentException("Cannot read comments via REST.", e);
        }
    }

    @Override
    public void reset() throws CommentException {
        throw new UnsupportedOperationException("Reset is not supported via REST");
    }
}
