# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring Boot 4 REST service (Java 25, Maven) that resolves a Norwegian street address, given as
kommunenummer / adressekode / husnummer / bokstav, to its *seksjoner* (condominium sections) with
owners, ownership fractions and dwelling-unit numbers (`bruksenhetsnummer`).

The only external source is the **Matrikkelen SOAP API** (Kartverket): `AdresseServiceWS` +
`StoreServiceWS` on `prodtest.matrikkel.no`, used to look up the address ID, its bubble objects and
owner persons.

The REST API, its JSON field names and the Matrikkel-facing code are in Norwegian. Java type names in
`core` are English (`Section`, `Owner`, `Fraction`) and mapped to Norwegian JSON with
`@JsonProperty` (`fraction` → `brøk`, `owners` → `eiere`). Match that split when adding types.

## Build & run

```bash
./mvnw spring-boot:run          # runs on http://localhost:8081
./mvnw clean package
```

**The build requires network access to `prodtest.matrikkel.no`.** The `jaxws-maven-plugin`
`wsimport` goal downloads the two WSDLs at `generate-sources` and generates the
`no.statkart.matrikkel.matrikkelapi.wsapi.v1.*` classes into `target/generated-sources/`. Those
classes are *not* in the repo, so a clean checkout will not compile unless that fetch succeeds. If
imports under `no.statkart.*` appear unresolved, run `./mvnw generate-sources` first.

Source files use UTF-8 and contain non-ASCII identifiers and JSON names (`transformBrøk`, `"brøk"`).
Don't "fix" them to ASCII.

Actuator is on the classpath; `/actuator/health` (with liveness/readiness probes) is used by the
container healthcheck.

### Container & release

`Dockerfile` is a two-stage build (Temurin 25 JDK → Temurin 25 JRE alpine). The build stage needs
the same network access for `wsimport`, and runs `package -DskipTests`. Credentials are passed at run
time as environment variables, never baked in. `.dockerignore` mirrors the `.gitignore` credentials
exclusion. Keep them in sync.

`.github/workflows/docker-publish.yml` builds and pushes to GHCR only on a bare semver tag
(`1.0.0`, not `v1.0.0` or `1.0.0-rc1`).

## Tests

```bash
./mvnw test                                  # runs the unit tests (ControllerTest, MatrikkelClientTest)
./mvnw test -Dtest=MatrikkelAddressServiceIT # runs the live integration test
```

- **`integration.MatrikkelClientTest`** is a plain Mockito test (no Spring context) that mocks
  `StoreService`, `AdresseService` and `MatrikkelContext`. It builds real generated SOAP objects
  (`Seksjon`, `Bruksenhet`, `PersonTinglystEierforhold`, …) as fixtures with small helper methods.
  Generated types don't implement `equals`, so stubs match ids with `argThat(id -> id.getValue() == …)`.

- **`api.ControllerTest`** is a `@WebMvcTest(Controller.class)` slice with
  `@MockitoBean MatrikkelService`, using `MockMvcTester`. It needs no network or credentials. In
  Spring Boot 4, `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure` and
  comes from the `spring-boot-starter-webmvc-test` test dependency, not `spring-boot-starter-test`.
- **`integration.MatrikkelAddressServiceIT`** is a **live** test. It boots `MatrikkelConfig`,
  `MatrikkelClient` and `MatrikkelServiceImpl` and calls the Matrikkel prodtest SOAP endpoints, so it
  fails without network access or valid credentials. Surefire's default includes don't match the
  `*IT` suffix and no Failsafe execution is configured, so it only runs when named explicitly or
  from the IDE.

**Test naming convention:** name test methods `test1`, `test2`, … and describe each test with
`@DisplayName("...")`. Don't use descriptive method names.

## Credentials

**`src/main/resources/matrikkel.properties` is git-ignored and must stay that way.** It holds the
Matrikkel SOAP username/password. `MatrikkelConfig` loads it with
`@PropertySource(ignoreResourceNotFound = true)` and injects the values into the JAX-WS
`BindingProvider` request context as HTTP basic auth.

You can supply them in two ways. Spring's normal precedence applies, so the environment wins:

```bash
# 1. local file: copy the template and fill it in
cp src/main/resources/matrikkel.properties.example src/main/resources/matrikkel.properties

# 2. environment: works with no properties file at all (relaxed binding maps the names)
export MATRIKKEL_WS_USERNAME=... MATRIKKEL_WS_PASSWORD=...
```

`matrikkel.properties.example` is the committed template. Keep it valueless. If startup fails with
`Could not resolve placeholder 'matrikkel.ws.username'`, neither source was provided.

Never paste credential values into CLAUDE.md, the example template, test fixtures or commit
messages.

## Architecture

Three packages under `no.xcello.matrikkel`:

- **`core`**: the domain, as records (`Section`, `Owner`, `Person`, `Fraction`) plus the
  `MatrikkelService` port (`List<Section> listSections(String matrikkelId)`). No framework or SOAP
  types here. This is what the REST layer serializes.
- **`api`**: `Controller`, which depends only on the `core.MatrikkelService` port.
- **`integration`**: the Matrikkel SOAP adapter.
  - `MatrikkelServiceImpl` is the single public implementation of `MatrikkelService` and delegates
    to `MatrikkelClient`.
  - `MatrikkelClient` is package-private on purpose. Everything outside `integration` should go
    through the `core.MatrikkelService` interface.
  - `MatrikkelConfig` supplies the three SOAP-side beans: the generated `AdresseService` port,
    `StoreService`, and a shared `MatrikkelContext` (locale `no_NO_B`, koordinatsystem 10, client
    id `xcello`, systemVersion `4.18`). Every SOAP call must receive the `MatrikkelContext`.

Generated SOAP types collide with domain names (`Seksjon`, `Person`, `LocalDate`), so
`MatrikkelClient` refers to them by fully qualified name.

### The matrikkel id

The service takes a single string `knr/veinr/husnr[/bokstav]`, e.g. `4601/10900/1/A` or
`4601/10900/1`. `Controller` builds it from path variables. `veinr` and `husnr` are bound as
`Integer`, so leading zeros are dropped (`010900` → `10900`), while `knr` is a `String` and keeps
them (`0301`). `MatrikkelClient.getAdresseId` splits it on `/`. With 3 parts, `bokstav` is sent as
`null`.

### The SOAP lookup flow (`MatrikkelClient.listSections`)

1. Build a `VegadresseIdent` from the id → `findAdresseIdForIdent` → `AdresseId`.
2. `findObjekterForAdresse` returns an `AdresseInfoTransfer` containing a flat, untyped
   `bubbleObjects` list. Everything downstream pattern-matches over that list with `instanceof`.
   `filterSections` extracts `Seksjon` and `Bruksenhet` into maps and joins them. The result is
   sorted by seksjonsnummer.
3. Owners: `transformEierforhold` filters each `Seksjon.eierforhold` down to the *highest*
   `eierforholdKodeId` value. It assumes the highest code is the real owner, e.g. a *festerett*
   holder outranks the *eiendomsrett* hjemmelshaver. Each owner's `PersonId` is then resolved with
   a separate `storeService.getObject` round trip.
4. `Bruksenhet` numbers are formatted as `<etasjeplan><etasjenummer 2d><løpenummer 2d>` (e.g.
   `H0101`), where etasjeplan codes 1–4 map to `H`/`K`/`L`/`U`.

Sealed-ish SOAP hierarchies (`Eierforhold` and its `Tinglyst`/`IkkeTinglyst`/`Kontaktinstans`
subtypes) are handled with `switch` pattern matching that throws `IllegalStateException` on an
unknown subtype. An unmapped subtype or etasjeplan code therefore fails loudly rather than silently.

### Endpoints

```
GET /api/adresse/{knr}/{veinr}/{husnr}/{bokstav}   -> List<core.Section>
GET /api/adresse/{knr}/{veinr}/{husnr}/            -> List<core.Section>  (trailing slash required; without it: 404)
```

A non-numeric `veinr` or `husnr` gives 400 before the service is called.

JSON shape of a section:

```json
{ "nummer": 1, "brøk": { "teller": 3, "nevner": 10 },
  "eiere": [ { "dato": "2020-01-15", "brøk": { "teller": 1, "nevner": 2 },
               "person": { "navn": "...", "id": "..." } } ],
  "bruksenhetNummer": "H0101" }
```

## Known rough edges

- `MatrikkelClient.listSections` still has two `System.out.println` debug loops over the bubble
  objects before the real work. Remove them if you touch that method.
- `filterSections` joins `Seksjon.getId()` against `Bruksenhet.getMatrikkelenhetId()`. Verify that
  key pairing against real data before relying on `bruksenhetNummer`. A miss passes `null` to
  `getBruksenhetNummer`, which throws an NPE.
- `transformEierforhold` uses `parallelStream()` over a list whose mapping makes one blocking SOAP
  call per element. It works, but parallelism isn't why it's written that way.
- `MatrikkelClient` catches `ServiceException` and rethrows it as a bare `RuntimeException`, so SOAP
  failures (including an unknown address) surface as 500s. There is no error mapping in `api`.
- `spring.mvc.hiddenmethod.filter.enabled=true` is set in `application.properties` but unused (GET
  only).
