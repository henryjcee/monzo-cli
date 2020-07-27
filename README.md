# monzo-cli
Fairly functional command line interface for the Monzo API. Built with [Kotlin](https://kotlinlang.org/docs/reference/), [Arrow](https://arrow-kt.io/) and [Fuel](https://github.com/kittinunf/fuel).
## Requirements
- Java 8+
- Bash
## Build and Installation
```bash
./gradlew fatJar && mkdir -p ~/.monzo && cp build/libs/monzo-cli-0.0.1.jar ~/.monzo/ && cp src/main/sh/monz ~/.monzo/ && chmod +x ~/.monzo/monz
```
## Usage
- Add `~/.monzo/monz` to path 
- `$ monz (balance|tx|stats|pot)`
- Click the link to begin the auth flow (requires port 8050)

`monz balance` - List account, pot and total balances.

`monz tx {optional_search_term}` - List transactions for the last 30 days with simple text search in the transaction description.

`monz stats` - Spending stats for the month to date.

`monz pot` - List pots.

`monz pot {add|remove} {pot_name} {amount} {desposit/funding account}` - Add or remove money from pots.

