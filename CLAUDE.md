# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring Boot 4 REST service (Java 25, Maven) that resolves a Norwegian street address to its
*seksjoner* (condominium sections) with owners, ownership fractions and dwelling-unit numbers
(`bruksenhetsnummer`). It combines two external sources:

1. **Geonorge address search** — public REST API (`https://ws.geonorge.no/adresser/v1/sok`), used to
   turn free text into a structured address (kommunenummer / adressekode / nummer / bokstav).
2. **Matrikkelen SOAP API** (Kartverket) — `AdresseServiceWS` + `StoreServiceWS` on
   `prodtest.matrikkel.no`, used to look up the address ID, its bubble objects, and owner persons.

Domain names and the REST API are in Norwegian; that is intentional — keep new domain types in
Norwegian (`Seksjon`, `Eier`, `Brøk`) and match the surrounding style.

## Build & run

```bash
./mvnw spring-boot:run          # runs on http://localhost:8081
./mvnw clean package
```

**The build requires network access to `prodtest.matrikkel.no`.** The `jaxws-maven-plugin`
`wsimport` goal downloads the two WSDLs at `generate-sources` and generates the
`no.statkart.matrikkel.matrikkelapi.wsapi.v1.*` classes into `target/generated-sources/`. Those
classes are *not* in the repo — a clean checkout will not compile without that fetch succeeding. If
imports under `no.statkart.*` appear unresolved, run `./mvnw generate-sources` first.

Source files use UTF-8 and contain non-ASCII identifiers (`Brøk`, `søk`). Don't "fix" them to ASCII.

## Tests

```bash
./mvnw test                                              # runs nothing today (see below)
./mvnw test -Dtest=MatrikkelAddressServiceIT             # runs the integration test
```

`MatrikkelAddressServiceIT` is the only test. Its `*IT` suffix is not matched by Surefire's default
includes and no Failsafe execution is configured, so it only runs when named explicitly (or from the
IDE). It is a **live** test: it calls Geonorge and the Matrikkel prodtest SOAP endpoints and will
fail without network or valid credentials.

`src/test/resources/search-result.json` is a captured Geonorge response, currently unused — useful
if you want to add an offline test of `MatrikkelSearchClient`/`SearchResult` deserialization.

## Credentials

**`src/main/resources/matrikkel.properties` is git-ignored and must stay that way.** It holds the
Matrikkel SOAP username/password, loaded via `@PropertySource(ignoreResourceNotFound = true)` in
`MatrikkelConfig` and injected into the JAX-WS `BindingProvider` request context as HTTP basic auth.

Two ways to supply them, in Spring's normal precedence order (environment wins):

```bash
# 1. local file — copy the template and fill it in
cp src/main/resources/matrikkel.properties.example src/main/resources/matrikkel.properties

# 2. environment — works with no properties file at all (relaxed binding maps the names)
export MATRIKKEL_WS_USERNAME=... MATRIKKEL_WS_PASSWORD=...
```

`matrikkel.properties.example` is the committed template; keep it valueless. If startup fails with
`Could not resolve placeholder 'matrikkel.ws.username'`, neither source was provided.

Never paste credential values into CLAUDE.md, the example template, test fixtures, or commit
messages.

## Architecture

Three layers, deliberately separated by package:

- **`core`** — the domain, expressed as records plus the `AdresseService` port
  (`Adresse`, `Seksjon`, `Eier`, `Person`, `Brøk`). No framework or external-API types here; this is
  what the REST layer serializes.
- **`model`** — DTOs for the *Geonorge JSON* only. Norwegian wire field names are mapped with
  `@JsonAlias` onto English record components (`poststed` → `postalName`). Do not let these leak
  past `MatrikkelSearchClient.transform`.
- **`api`** — `AdresseRestController`, depends only on the `core` port.

`MatrikkelAddressService` is the single implementation of `core.AdresseService` and just fans out to
two package-private clients: `MatrikkelSearchClient` (Geonorge, `RestClient`) and
`MatrikkelWebServiceClient` (SOAP). Both clients are package-private on purpose — everything outside
`no.xcello.matrikkel` should go through the `core.AdresseService` interface.

`MatrikkelConfig` supplies the three SOAP-side beans: `AdresseService` (generated port — note the
name collision with `core.AdresseService`), `StoreService`, and a shared `MatrikkelContext`
(locale `no_NO_B`, koordinatsystem 10, client id `xcello`, systemVersion `4.18`). The
`MatrikkelContext` must be passed on every SOAP call.

### The SOAP lookup flow (`MatrikkelWebServiceClient.hentSeksjoner`)

1. Build a `VegadresseIdent` from the domain `Adresse` → `findAdresseIdForIdent` → `AdresseId`.
   A blank `bokstav` must be sent as `null`, not `""`.
2. `findObjekterForAdresse` returns an `AdresseInfoTransfer` containing a flat, untyped
   `bubbleObjects` list. Everything downstream is `instanceof` pattern-matching over that list —
   `Seksjon` and `Bruksenhet` are extracted into maps and joined in `filterSections`.
3. Owners: each `Seksjon.eierforhold` is filtered to only the *highest* `eierforholdKodeId` value
   (`transformEierforhold`), on the assumption that the highest code is the real owner — e.g. a
   *festerett* holder outranks the *eiendomsrett* hjemmelshaver. Then each owner's `PersonId` is
   resolved with a separate `storeService.getObject` round trip.
4. `Bruksenhet` numbers are formatted as `<etasjeplan><etasjenummer 2d><løpenummer 2d>` (e.g.
   `H0101`), where etasjeplan code 1–4 maps to `H`/`K`/`L`/`U`.

Sealed-ish SOAP hierarchies (`Eierforhold`, and its `Tinglyst`/`IkkeTinglyst`/`Kontaktinstans`
subtypes) are handled with `switch` pattern matching that throws `IllegalStateException` on unknown
subtypes — so an unmapped code or etasjeplan value fails loudly rather than silently.

### Endpoints

```
GET /api/adresse?query=Dokkeveien+1A&size=5        -> List<core.Adresse>
GET /api/adresse/{knr}/{veinr}/{husnr}/{bokstav}   -> List<core.Seksjon>
GET /api/adresse/{knr}/{veinr}/{husnr}/            -> List<core.Seksjon>  (trailing slash required)
```

The seksjon endpoints construct a partial `core.Adresse` with `null` postadresse/postnummer/navn —
only kommunenummer, veinummer (adressekode), husnummer and bokstav are used by the SOAP path.

## Known rough edges

- `hentSeksjoner` still has two `System.out.println` debug loops over the bubble objects before the
  real work; remove them if you touch that method.
- `filterSections` joins `Seksjon.getId()` against `Bruksenhet.getMatrikkelenhetId()`. Verify that
  key pairing against real data before relying on `bruksenhetNummer`; a miss yields an NPE in
  `getBruksenhetNummer`.
- `transformEierforhold` uses `parallelStream()` over a list whose mapping does a blocking SOAP call
  per element — fine functionally, but not the reason it's parallel.
- Not a git repository yet. When running `git init`, confirm `.gitignore` is in place first so
  `src/main/resources/matrikkel.properties` is never staged (`git check-ignore -v` to verify).
