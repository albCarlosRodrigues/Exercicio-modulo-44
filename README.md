# Exercício 44 — Acesso temporário com Spring

A API cadastra usuários por e-mail e senha em um banco H2 salvo na pasta `dados`. Após o login, o servidor cria uma sessão e envia o cookie `JSESSIONID`. O acesso expira 60 segundos depois do login, mesmo quando a rota protegida é consultada durante esse período. Para obter outro minuto de acesso, faça login novamente.

## Executar

Com Java 21 e Maven instalados, abra o terminal na pasta do projeto e execute `mvn spring-boot:run`. A API atende em `http://localhost:8080`.

## Testar no PowerShell

Em outro terminal, execute os comandos abaixo. O parâmetro `-WebSession` envia o cookie recebido no login.

```powershell
$api = 'http://localhost:8080/api'
$dados = @{ email = 'aluno@example.com'; senha = 'Exemplo-44' } | ConvertTo-Json
Invoke-RestMethod "$api/usuarios" -Method Post -ContentType 'application/json' -Body $dados
Invoke-RestMethod "$api/login" -Method Post -ContentType 'application/json' -Body $dados -SessionVariable sessao
Invoke-RestMethod "$api/protegida" -WebSession $sessao
Start-Sleep -Seconds 61
Invoke-RestMethod "$api/protegida" -WebSession $sessao
```

O cadastro retorna HTTP 201. A primeira consulta autenticada retorna HTTP 200 com “Acesso garantido”. A última retorna HTTP 401 com “Usuário não está logado.”. O PowerShell apresenta o 401 como erro. Um segundo cadastro com o mesmo e-mail retorna HTTP 409.
