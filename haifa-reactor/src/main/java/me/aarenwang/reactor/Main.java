package me.aarenwang.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) {
        //testFlux();

        testFlux2();

        //testMono();
    }

    static void testFlux(){
        Flux<String> seq1 = Flux.just("foo", "bar", "foobar");

        seq1.subscribe(c -> {
            System.out.println(c);
        });

        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
        Flux<String> seq2 = Flux.fromIterable(iterable);

        Flux<Integer> range = Flux.range(1,100);
        AtomicInteger sum = new AtomicInteger();
        range.subscribe(i -> {
            sum.set(sum.get() + i);
        });
        System.out.println(sum.get());
    }


    static void testFlux2(){
        Flux<String> seq1 = Flux.just("one", "four" ,null,"sixth","eleven");
//        seq1.subscribe(e -> {
//            System.out.println(e.length());
//        });
        seq1.map(i -> i.length())
                .onErrorReturn(NullPointerException.class,-1)

                .subscribe(e -> System.out.println(e));

    }

    static void  testMono(){
        Mono<Integer> mono =  Flux.range(1,10)
                .filter(i -> i % 2 == 0)
                .doOnEach(i -> {
                 System.out.println("filter i = "+i);
                })
                .map(i -> i * i).reduce(0,(a, b) -> { return a+b; });

        System.out.println(mono.subscribe(i ->{
            System.out.println(i);
        }));
    }
}
