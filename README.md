# monzo-cli
Fairly functional command line interface for the Monzo API. Built with Kotlin and Ktor client.
## Requirements
- Java 8+
- Bash
## Build and Installation
```bash
./gradlew fatJar && mkdir -p ~/.monzo && cp build/libs/monzo-cli-0.0.1.jar ~/.monzo/ && cp src/main/sh/monz ~/.monzo/ && chmod +x ~/.monzo/monz
```
## Usage
Monzo API rules mean that unfortunately you'll have to generate your own client id and secret to use this application as third party apps can now only run against whitelisted clients.

- Generate a Monzo client [here](https://developers.monzo.com) and add  `MONZO_CLIENT_ID` and `MONZO_CLIENT_SECRET` env vars.
- Add `~/.monzo/monz` to path.
- `$ monz (balance|tx|stats)`.
- Click the link to begin the auth flow (requires port 8050).

`monz balance` - List account, pot and total balances.

`monz tx {optional_search_term}` - List transactions for the last 30 days with simple text search in the transaction description.

`monz stats` - Spending stats for the month to date.
