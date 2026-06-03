package sk.tuke.gamestudio;
//mll
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.game.cuberoll.consoleui.ConsoleUI;
import sk.tuke.gamestudio.game.cuberoll.core.Field;
import sk.tuke.gamestudio.game.cuberoll.core.Levels;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;
import sk.tuke.gamestudio.service.rest.CommentServiceRestClient;
import sk.tuke.gamestudio.service.rest.RatingServiceRestClient;
import sk.tuke.gamestudio.service.rest.ScoreServiceRestClient;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Configuration
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
    pattern = "sk.tuke.gamestudio.server.*"))
public class SpringClient {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringClient.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
    @Bean
    public CommandLineRunner runner(ConsoleUI consoleUI) {
        return args -> consoleUI.play(new Field(Levels.getLevel(0)));
    }

    @Bean
    public ConsoleUI consoleUI(ScoreService scoreService, CommentService commentService, RatingService ratingService) {
        return new ConsoleUI(scoreService, commentService, ratingService);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ScoreService scoreService(RestTemplate restTemplate) {
        return new ScoreServiceRestClient(restTemplate);
    }

    @Bean
    public CommentService commentService(RestTemplate restTemplate) {
        return new CommentServiceRestClient(restTemplate);
    }

    @Bean
    public RatingService ratingService(RestTemplate restTemplate) {
        return new RatingServiceRestClient(restTemplate);
    }
}
