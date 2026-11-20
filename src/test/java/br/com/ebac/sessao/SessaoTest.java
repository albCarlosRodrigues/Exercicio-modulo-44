package br.com.ebac.sessao;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:exercicio44")
class SessaoTest {
    @Autowired WebApplicationContext contexto;
    @MockitoBean Clock relogio;

    @Test
    void acessoExpiraUmMinutoAposLoginMesmoComConsultas() throws Exception {
        var api = MockMvcBuilders.webAppContextSetup(contexto).build();
        var inicio = Instant.parse("2026-01-01T12:00:00Z");
        when(relogio.instant()).thenReturn(inicio);
        var dados = """
                {"email":"teste@example.com","senha":"senha-do-teste"}
                """;
        api.perform(get("/api/protegida")).andExpect(status().isUnauthorized());
        api.perform(post("/api/usuarios").contentType("application/json").content(dados))
                .andExpect(status().isCreated());
        api.perform(post("/api/usuarios").contentType("application/json").content(dados))
                .andExpect(status().isConflict());
        api.perform(post("/api/login").contentType("application/json").content(dados.replace("senha-do-teste", "errada")))
                .andExpect(status().isUnauthorized());
        var login = api.perform(post("/api/login").contentType("application/json").content(dados))
                .andExpect(status().isOk()).andReturn();
        var sessao = (MockHttpSession) login.getRequest().getSession(false);
        when(relogio.instant()).thenReturn(inicio.plusSeconds(59));
        api.perform(get("/api/protegida").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Acesso garantido"));
        when(relogio.instant()).thenReturn(inicio.plusSeconds(60));
        api.perform(get("/api/protegida").session(sessao)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário não está logado."));
    }
}
