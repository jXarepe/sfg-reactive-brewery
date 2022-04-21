package guru.springframework.sfgrestbrewery.functional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class BeerRouterConfiguration {

    public static final String BEER_ROUTE_V2="api/v2/beer";
    public static final String BEER_PATH_ID_V2 ="beerId";
    public static final String BEER_ROUTE_BEER_ID = BEER_ROUTE_V2 + "/{" + BEER_PATH_ID_V2 + "}";
    public static final String BEER_PARAM_SHOW_INVENTORY_V2="showInventory";
    public static final String BEER_UPC_ROUTE_V2="api/v2/beerUpc";
    public static final String BEER_PATH_UPC_V2 ="upc";

    @Bean
    public RouterFunction<ServerResponse> beerRouter(BeerHandler beerHandler){
        return route()
                .GET(BEER_ROUTE_BEER_ID, accept(MediaType.APPLICATION_JSON), beerHandler::getBeerById)
                .GET(BEER_UPC_ROUTE_V2 +  "/{" + BEER_PATH_UPC_V2 + "}", accept(MediaType.APPLICATION_JSON), beerHandler::getBeerByUpc)
                .POST(BEER_ROUTE_V2, accept(MediaType.APPLICATION_JSON), beerHandler::addBeer)
                .PUT(BEER_ROUTE_BEER_ID, accept(MediaType.APPLICATION_JSON), beerHandler::updateBeer)
                .DELETE(BEER_ROUTE_BEER_ID, accept(MediaType.APPLICATION_JSON), beerHandler::deleteBeer)
                .build();
    }
}
