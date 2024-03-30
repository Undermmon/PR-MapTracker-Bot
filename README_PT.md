# PR Map Logging Discord Bot
Bot para Discord que cataloga os mapas rodados em servidores de Project Reality e permite fazer buscas neles.

## Rodando o bot

**Requer Java 17 ou mais recente**

Vá até a pasta na qual você deseja instalar o bot e faça o download dele
> wget https://github.com/Undermmon/PRMapLoggerBot/releases/download/v24.3.30/maplogger-24.3.30.zip

Então extraia o arquivo zip baixado.
> unzip maplogger-24.3.30.zip

Vá até a pasta *bin* dentro da pasta recém extraída
> cd maplogger-24.3.30/bin

Edite o arquiivo de configuração chamado *config.json*, para isso usaremos o *nano* um popular editor de arquivos para terminal.
> nano config.json

A configuração consiste do seguinte:
- realitymod_api -> link para a api rest da reality mod que retorna ServerInfo.json. Você provavelmente não precisa mudar este valor.
- token -> O token do seu bot do Discord, **você precisa substituir \<YOUR BOT TOKEN\> com o seu próprio**.
- fetch_interval -> Quão frequentemente **em minutos** o bot vai pegar informações da api da reality mod.
- monitored_servers -> Define quais servidores devem ter seus mapas catalogados, **deve haver um ou mais** servidores monitorados. O primeiro é o padrão, ele será usado quando nenhum servidor for especificado em um comando.


\<SERVER NAME\> é o nome do servidor que será apresentado para os usuários via comandos, por exemplo em autocompletações, etc. Ele não precisa ser igual ao nome do servidor em jogo.

\<SERVER IDENTIFIER\> é o identificador único do servidor. Você pode encontrar ele no ServerInfo.json retornado pela api da reality mod. 


```JSON
{
	"realitymod_api": "https://servers.realitymod.com/api/ServerInfo",
	"token": "<YOUR BOT TOKEN>",
	"fetch_interval": 5,
	"monitored_servers": {
		"<SERVER NAME>": "<SERVER IDENTIFIER>"
	}
}
```
Para salvar modificações no *nano* pressione **CTRL+O** depois **ENTER**, agora saia do editor com **CTRL+X**. 

Agora execute o bot. Se você está no *Windows* use *MapLogger.bat*

> ./MapLogger

Se você receber o erro *permission denied* dê o arquivo permissão para ser executado por seu usuário

> chmod u+x MapLogger

Pode demorar alguns segundos para seu bot ficar online. Uma vez iniciado você receberá o link para convidar o bot.

> Started sucessfully, you can invite the bot with: ...

Se o seu *config.json* não estiver correto você verá isso

> [main] ERROR - me.undermon.maplogger.configuration.InvalidConfigurationException: ...

Se o token do seu bot não for válido você verá isso

> [WritingThread] INFO  - Websocket closed with reason 'Authentication failed.' and code AUTHENTICATION_FAILED (4004) by server!

## Logs e Banco de Dados

Todas as mensagens de logos do console também são anexadas ao arquivo *logs* dentro do diretório *bin*.

Os mapas rodados são persistidos em um banco de dados SQlite chamado *maps.db* dentro do diretório *bin*.


Tanto *logs* como *maps.db* são criados automaticamente caso não existam.

## Atualizando

Para atualizar basta baixar e extrair a nova versão, então mova *config.json*, *maps.db*, e opcionalmente *logs* do diretório da antiga versão para o diretório da nova versão.

O projeto usa *Calendar Versioning*, sendo assim *v24.3.29* siginifica que a atualização foi lançada dia 29 de fevereiro de 2024. Em caso de múltiplas atualizações no mesmo dia será colocado um sufixo numérico, por exemplo *v24.2.29-1* seria a segunda atualização do dia.

Qualquer versão que quebre a compatibilidade será explicitamente marcada como tal. 