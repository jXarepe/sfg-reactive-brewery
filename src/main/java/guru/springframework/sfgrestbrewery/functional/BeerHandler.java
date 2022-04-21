package guru.springframework.sfgrestbrewery.functional;

import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.controller.NotFoundException;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;


import static guru.springframework.sfgrestbrewery.functional.BeerRouterConfiguration.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerHandler {
    private final BeerService beerService;
    private final Validator validator;

    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        Integer beerId = Integer.valueOf(request.pathVariable(BEER_PATH_ID_V2));
        Boolean showInventory = Boolean.valueOf(request.queryParam(BEER_PARAM_SHOW_INVENTORY_V2).orElse("false"));

        return beerService.getById(beerId, showInventory)
                .flatMap(beerDto -> {
                    return ServerResponse.ok().bodyValue(beerDto);
                }).switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getBeerByUpc(ServerRequest request) {
        String upc = request.pathVariable(BEER_PATH_UPC_V2);

        return beerService.getByUpc(upc)
                .flatMap(beerDto -> {
                    return ServerResponse.ok().bodyValue(beerDto);
                }).switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> addBeer(ServerRequest request) {
        Mono<BeerDto> monoBeer = request.bodyToMono(BeerDto.class).doOnNext(this::validate);

        return beerService.saveNewBeer(monoBeer)
                .flatMap(beerDto -> {
                    return ServerResponse.created(UriComponentsBuilder
                            .fromHttpUrl("http://localhost:8080/api/v2/beer/" + beerDto.getId())
                            .build().toUri()).build();
                });
    }

    public Mono<ServerResponse> updateBeer(ServerRequest request){
        return request.bodyToMono(BeerDto.class).doOnNext(this::validate)
                .flatMap(beerDto -> beerService.updateBeer(Integer.valueOf(request.pathVariable(BEER_PATH_ID_V2)), beerDto))
                .flatMap(savedBeerDto -> {
                    if(savedBeerDto.getId() != null){
                        log.info("saved id: {}", savedBeerDto.getId());
                        return ServerResponse.noContent().build();
                    }
                    return ServerResponse.notFound().build();
                });
    }

    public Mono<ServerResponse> deleteBeer(ServerRequest request){
        return beerService.reactiveDeleteBeerById(Integer.valueOf(request.pathVariable(BEER_PATH_ID_V2)))
                .flatMap(voidMano -> ServerResponse.noContent().build())
                .onErrorResume(e -> e instanceof NotFoundException, e -> ServerResponse.notFound().build());
    }

    private void validate(BeerDto beerDto){
        Errors errors = new BeanPropertyBindingResult(beerDto, "beerDto");
        validator.validate(beerDto, errors);

        if (errors.hasErrors()){
            throw new ServerWebInputException(errors.toString());
        }
    }
}
