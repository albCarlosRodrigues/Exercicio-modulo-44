package br.com.ebac.sessao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication
public class Aplicacao {
    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(Aplicacao.class, args);
    }
}
