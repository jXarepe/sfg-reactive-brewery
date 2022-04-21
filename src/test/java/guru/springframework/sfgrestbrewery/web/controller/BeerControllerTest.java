package guru.springframework.sfgrestbrewery.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebFluxTest(BeerController.class)
public class BeerControllerTest {

    @MockBean
    BeerService beerService;

    @Autowired
    WebTestClient mockMvc;

    BeerDto validBeer;

    @BeforeEach
    public void setUp() {
        validBeer = BeerDto.builder().id(1)
                .beerName("Beer1")
                .beerStyle("PALE_ALE")
                .upc(BeerLoader.BEER_2_UPC)
                .build();
    }

    @Test
    public void getBeer() throws Exception {
        given(beerService.getById(any(Integer.class), any())).willReturn(Mono.just(validBeer));

        mockMvc.get().uri("/api/v1/beer/" + validBeer.getId().toString()).accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectBody(BeerDto.class)
                .value(BeerDto::getBeerName, equalTo(validBeer.getBeerName()))
                .value(BeerDto::getId, equalTo(validBeer.getId()));
    }

    @Test
    public void getBeerByUpc() throws Exception {
        given(beerService.getByUpc(any())).willReturn(Mono.just(validBeer));

        mockMvc.get().uri("/api/v1/beerUpc/" + validBeer.getUpc()).accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectBody(BeerDto.class)
                .value(BeerDto::getBeerName, equalTo(validBeer.getBeerName()))
                .value(BeerDto::getId, equalTo(validBeer.getId()));
    }

    @Test
    public void getBeers() throws Exception {
        given(beerService.listBeers(any(), any(),any(),any())).willReturn(Mono.just(new BeerPagedList(List.of(validBeer))));

        mockMvc.get().uri("/api/v1/beer").accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectBody(BeerPagedList.class)
                .value(PageImpl::getTotalElements, equalTo(1L));
    }

    @Test
    public void handlePost() throws Exception {
        //given
        BeerDto beerDto = validBeer;
        beerDto.setId(null);
        BeerDto savedDto = BeerDto.builder().id(1).beerName("New Beer").build();

        given(beerService.saveNewBeer(any(BeerDto.class))).willReturn(Mono.just(savedDto));

        mockMvc.post().uri("/api/v1/beer/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(beerDto), BeerDto.class).exchange()
                .expectStatus().isCreated();

    }

    @Test
    public void handleUpdate() throws Exception {
        //given
        BeerDto beerDto = validBeer;
        beerDto.setId(null);

        //when
        mockMvc.put().uri("/api/v1/beer/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(beerDto), BeerDto.class).exchange()
                .expectStatus().isNoContent();

        then(beerService).should().updateBeer(any(), any());

    }
}