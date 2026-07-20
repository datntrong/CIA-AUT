# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`cia-aut` (groupId `uet.fit`) is a research/teaching tool from UET-FIT that analyzes and tests **C/C++ projects**. It combines two capabilities behind one server:

- **CIA (Change Impact Analysis)** — given a C/C++ project's git history, diff two commits/branches and compute which functions/structs are impacted by the change. Engine lives in `cia/cia-api` (package `uet.fit.cia.cpp`).
- **AUT (Automated Unit Testing)** — parse C/C++ source, instrument it, generate test data, compile & run it, and collect coverage. Engine lives in `aut` (package `uet.fit.aut`).

The tool operates on **external** C/C++ codebases, so at runtime it shells out to a real C++ toolchain (`g++`, `qmake`, `make`) whose paths come from config. It is not self-contained Java.

## Architecture

Maven multi-module, **Java 11**, deployed as a **client/server** pair.

```
config   (jar)  Global config singleton. Reads config.properties via CIAUT_CONFIG.
common   (jar)  Shared DTOs / util / cia data models (lombok, gson).
aut      (jar)  AUT engine: parser (Eclipse CDT) → instrument → testdata/autogen
                → execution/testdriver → coverage. Also stub_manager, env, ter.
cia      (pom)  ├─ cia-api     (jar)  CIA engine over C/C++ (depends on `cpp`, cia-display)
                └─ cia-display (jar)  CIA result rendering
report   (jar)  Report building (depends on common).
server   (war)  REST backend. Orchestrates aut + cia-api + report. → Tomcat 9.
client   (jar)  JavaFX desktop GUI. Talks to server over HTTP (RESTEasy client).
```

Dependency direction: `server` depends on `aut`, `cia-api`, `report`, `common`, `config`. `client` depends only on `common` + `cia-display` and reaches the server over REST — the two are deployed and run separately.

### Server (the backend)
- JAX-RS on **RESTEasy**, mounted at `/*` (see `server/src/main/webapp/WEB-INF/web.xml`). The `javax.ws.rs.Application` is `uet.fit.server.app.HelloApplication`, which registers every REST resource singleton — **add new endpoints there** or they won't be wired up.
- Resources are under `uet.fit.server.rest.resource` (`@Path`): `/repo`, `/user`, `/func`, `/config`, `/log`, `/usercode`, `/env`, `/test`, `/cia`. Existing `/cia` endpoints are documented in `server/README.md`.
- Persistence via **MyBatis** over **MySQL/MariaDB** (`uet.fit.server.DAO`). Git operations via **JGit**.

### Client (the GUI)
- JavaFX; entry point `uet.fit.client.ui.Main` → `AppStart extends Application`. Views are FXML in `client/src/main/resources/fxml`.
- The server base URL is resolved in this order: command-line arg → saved config file → interactive prompt (`AppStart.parseArgsForServerUrl` / `readAndSetServerUrl` / `inputServerUrl`).

## Build

```bash
mvn clean package          # build everything (server.war + client fat jar)
mvn -pl aut -am package    # build one module (aut) and its dependencies
mvn -pl server -am package # build the server war and what it needs
```

Non-obvious build facts — read before touching build config:
- The root pom, in the `initialize` phase, **installs a bundled Eclipse CDT jar** (`org.eclipse.cdt.core` 7.4.200-SNAPSHOT) into the local Maven repo from `lib/`. This is a custom artifact not on Maven Central; a clean build depends on those `lib/*.jar` files being present.
- **Build output is not in `target/`.** The root pom redirects it to `build/target-<finalName>` per module; the `server` module redirects further to the repo-root `build/`. `build/` is gitignored.
- Artifacts: server war under the server module's build dir; client fat jar at `client/build/.../client-1.0-SNAPSHOT-fat.jar` (main class `uet.fit.client.ui.Main`, JavaFX shaded in).
- Maven ≥ 3.2.5 is enforced. Root pom carries a "DON'T CHANGE THE GLOBAL POM" warning — keep shared build config out of it unless necessary.

## Run

**Client** (after building):
```bash
java -jar client/build/target-client-1.0-SNAPSHOT/client-1.0-SNAPSHOT-fat.jar <server-url>
```

**Server** — deploy `server.war` to Tomcat 9 with `CIAUT_CONFIG` pointing at a `config.properties`, or use Docker (below). The `docker/` image bundles MariaDB + Qt 5.14.2 + Tomcat and hot-deploys any `server.war` copied to `/data/server.war` (see `docker/docker-startup.sh`); it auto-generates `/data/config.properties` on first start.

## Configuration

Config is a singleton `Config.INSTANCE` (module `config`), loaded from a `config.properties` file. The file path comes from the `CIAUT_CONFIG` system property or env var; if unset, the server falls back to `WEB-INF/classes/config.properties` in the deployed war (`config/.../ConfigLoader.java`, a `@WebListener`).

Keys that matter (toolchain + DB): `GPP_PATH`, `QMAKE_PATH`, `MAKE_PATH`, `MAKE_JOBS_COUNT`, `RUN_TEST_TIMEOUT`, `FUNCTION_CALL_ANALYZE`, `CIAUT_HOME`, `TOMCAT_LOG`, `SQL_HOST`, `SQL_PORT`, `SQL_USER`, `SQL_PASSWORD`.

## Tests

There is currently **no unit-test suite** in the repo (`src/test` is empty across modules; only `client` declares JUnit). "Testing" in this codebase means the AUT engine generating and running tests against a *target* C/C++ project, not JUnit tests of this Java code. Don't assume `mvn test` verifies anything meaningful yet.

## Conventions

- Keep anything not meant to be public under `local/` (gitignored), per the note in `.gitignore`.
- Active development happens on many `dev-*` / `dev/*` branches (see `git branch -r`); `master` is the mainline. Branches diverge significantly — confirm merge state with `git log A..B` before assuming a feature is present.
