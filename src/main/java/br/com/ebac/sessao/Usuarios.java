package br.com.ebac.sessao;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class Usuarios {
    private final JdbcTemplate banco;
    private final SecureRandom aleatorio = new SecureRandom();

    Usuarios(JdbcTemplate banco) {
        this.banco = banco;
    }

    void cadastrar(String email, String senha) {
        byte[] sal = new byte[16];
        aleatorio.nextBytes(sal);
        banco.update("INSERT INTO usuarios (email, sal, senha_hash) VALUES (?, ?, ?)",
                email, sal, calcularHash(senha, sal));
    }

    boolean autenticar(String email, String senha) {
        return banco.query("SELECT sal, senha_hash FROM usuarios WHERE email = ?",
                rs -> rs.next() && MessageDigest.isEqual(rs.getBytes("senha_hash"),
                        calcularHash(senha, rs.getBytes("sal"))), email);
    }

    private byte[] calcularHash(String senha, byte[] sal) {
        var parametros = new PBEKeySpec(senha.toCharArray(), sal, 600_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(parametros).getEncoded();
        } catch (GeneralSecurityException erro) {
            throw new IllegalStateException("Falha ao processar a senha.", erro);
        } finally {
            parametros.clearPassword();
        }
    }
}
