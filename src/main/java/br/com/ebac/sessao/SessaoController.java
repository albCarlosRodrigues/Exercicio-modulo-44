package br.com.ebac.sessao;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class SessaoController {
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private final Usuarios usuarios;

    private final Clock relogio;

    SessaoController(Usuarios usuarios, Clock relogio) {
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    record Credenciais(String email, String senha) {
        Credenciais {
            email = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        }

        boolean validas() {
            return email.length() <= 254 && EMAIL.matcher(email).matches()
                    && senha != null && !senha.isBlank() && senha.length() <= 256;
        }
    }

    record Mensagem(String mensagem) {}

    @PostMapping("/usuarios")
    ResponseEntity<Mensagem> cadastrar(@RequestBody Credenciais dados) {
        if (!dados.validas()) {
            return resposta(HttpStatus.BAD_REQUEST, "Informe um e-mail válido e uma senha de até 256 caracteres.");
        }
        try {
            usuarios.cadastrar(dados.email(), dados.senha());
        } catch (DuplicateKeyException erro) {
            return resposta(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        return resposta(HttpStatus.CREATED, "Usuário cadastrado.");
    }

    @PostMapping("/login")
    ResponseEntity<Mensagem> entrar(@RequestBody Credenciais dados, HttpServletRequest requisicao) {
        if (!dados.validas() || !usuarios.autenticar(dados.email(), dados.senha())) {
            return resposta(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        var anterior = requisicao.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        var sessao = requisicao.getSession(true);
        sessao.setAttribute("email", dados.email());
        sessao.setAttribute("expiraEm", relogio.instant().plusSeconds(60));
        return resposta(HttpStatus.OK, "Login realizado. Sessão válida por um minuto.");
    }

    @GetMapping("/protegida")
    ResponseEntity<Mensagem> acessar(HttpServletRequest requisicao) {
        var sessao = requisicao.getSession(false);
        if (sessao != null && sessao.getAttribute("expiraEm") instanceof Instant limite
                && relogio.instant().isBefore(limite)) {
            return resposta(HttpStatus.OK, "Acesso garantido");
        }
        if (sessao != null) {
            sessao.invalidate();
        }
        return resposta(HttpStatus.UNAUTHORIZED, "Usuário não está logado.");
    }

    private ResponseEntity<Mensagem> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new Mensagem(mensagem));
    }
}
