package guru.springframework.sfgrestbrewery.web.controller;


import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by jt on 3/7/21.
 */
public class WebClientIT {

    public static final String BASE_URL = "http://localhost:8080";
    private static final String BEER_NAME = "updated beer";

    WebClient webClient;

    @BeforeEach
    void setUp() {
       webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
                .build();
    }

    @Test
    void getBeerById() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri("/api/v1/beer/1")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(dto ->{
            assertThat(dto).isNotNull();
            assertThat(dto.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUpc() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri("/api/v2/beerUpc/" + BeerLoader.BEER_1_UPC)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(dto ->{
            assertThat(dto).isNotNull();
            assertThat(dto.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUpcError404() {
        BeerDto beer = null;
        try {
            beer = webClient.get().uri("/api/v2/beerUpc/" + 1)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(BeerDto.class).block();
        } catch (WebClientResponseException ex){
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
        assertThat(beer).isNull();
    }

    @Test
    void addBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        BeerDto beerDto = BeerDto.builder()
                .beerName("Sagres")
                .beerStyle("PALE_ALE")
                .upc(BeerLoader.BEER_2_UPC)
                .price(BigDecimal.ONE)
                .build();

        Mono<ResponseEntity<Void>> responseMono = webClient.post().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();

        responseMono.publishOn(Schedulers.parallel()).subscribe(response -> {
            assertThat(response.getStatusCode().is2xxSuccessful());
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    public void updateBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        Mono<BeerPagedList> beerPagedListMono = webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);


        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {
            BeerDto payloadBeer = beerPagedList.getContent().get(0);
            Integer id = payloadBeer.getId();
            payloadBeer.setBeerName(BEER_NAME);
            payloadBeer.setId(null);
            countDownLatch.countDown();

            Mono<ResponseEntity<Void>> responseMono = webClient.put().uri("/api/v1/beer/"+id)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(payloadBeer))
                    .retrieve().toBodilessEntity();

            responseMono.publishOn(Schedulers.parallel()).subscribe(response -> {
                assertThat(response.getStatusCode().is2xxSuccessful());
                countDownLatch.countDown();

                Mono<BeerDto> beerDtoMono = webClient.get().uri("/api/v1/beer/"+id)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(BeerDto.class);

                beerDtoMono.subscribe(dto ->{
                    assertThat(dto).isNotNull();
                    assertThat(dto.getBeerName()).isEqualTo(BEER_NAME);

                    countDownLatch.countDown();
                });
            });
        });

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    public void deleteBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);

        Mono<BeerPagedList> beerPagedListMono = webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);


        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {
            BeerDto payloadBeer = beerPagedList.getContent().get(0);
            Integer id = payloadBeer.getId();
            Long totalElement = beerPagedList.getTotalElements();
            countDownLatch.countDown();

            Mono<ResponseEntity<Void>> responseMono = webClient.delete().uri("/api/v1/beer/"+id)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().toBodilessEntity();

            responseMono.publishOn(Schedulers.parallel()).subscribe(response -> {
                assertThat(response.getStatusCode().is2xxSuccessful());
                countDownLatch.countDown();

                Mono<BeerPagedList> beerPagedListMono2 = webClient.get().uri("/api/v1/beer")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(BeerPagedList.class);


                beerPagedListMono2.publishOn(Schedulers.parallel()).subscribe(beerPagedListFinal -> {
                    assertThat(beerPagedListFinal.getTotalElements()).isNotEqualTo(totalElement);
                });
            });
        });

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testListBeers() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);


        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {

            beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));

            countDownLatch.countDown();
        });

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }
}
