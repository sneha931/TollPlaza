# Toll Plaza Service

Spring Boot REST API that returns the toll plazas on the driving route between two Indian pincodes.

## Tech stack

- Java 17, Spring Boot 4.1
- H2 in-memory database (toll plaza reference data)
- Caffeine cache
- **Geocoding:** bundled India Post pincode directory (19,255 pincodes → lat/lng), with OpenStreetMap Nominatim as fallback
- **Routing:** Mappls Route Advanced API (lat/lng → road polyline), with OSRM as a key-free fallback
- **API docs:** springdoc-openapi (Swagger UI)
- **Deployment:** Docker, Render

## Configure the routing provider

| Provider | When it's used | Key |
|---|---|---|
| **Mappls** (default) | Follows India's national highways, the same routes Mappls' website shows | `MAPPLS_API_KEY` environment variable |
| OSRM | Used when no Mappls key is set, or when `ROUTING_PROVIDER=osrm` | none |

Get a free key at <https://about.mappls.com/api>: create a project, choose Cloud as the platform, and copy the **Static Key**. Never put it in `application.properties`.

**Recommended: a project-local `.env` file.** It's git-ignored, so the key is never committed.

```bash
cp .env.example .env          # then edit .env:  MAPPLS_API_KEY=<your-key>
```

**Or use an environment variable.** It overrides `.env`.

```powershell
# Windows PowerShell, current terminal only
$env:MAPPLS_API_KEY = "<your-key>"
# Windows, permanently for your user (open a new terminal afterwards)
setx MAPPLS_API_KEY "<your-key>"
```
```bash
# macOS / Linux
export MAPPLS_API_KEY=<your-key>
```

At startup the log shows which provider is active: `Routing provider: Mappls` or `Routing provider: OSRM`.

## Run

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The app starts on `http://localhost:8080`. On startup it loads `src/main/resources/data/toll_plaza_india.csv` into H2.

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>. The root URL `/` redirects there. On `POST /api/v1/toll-plazas`, click **Try it out** and pick a scenario from the **Examples** dropdown: the same 9 cases as the Postman collection.
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>
- **H2 console (local only):** `/h2-console`, JDBC URL `jdbc:h2:mem:tolldb`, user `sa`. The Docker image turns it off, because the console can run any SQL.

## Testing

**1. Automated tests** (no network needed: the external APIs are mocked)

```bash
./mvnw test                   # 70 tests; JaCoCo report at target/site/jacoco/index.html
```

| Layer | Test class | What it checks |
|---|---|---|
| Geometry | `GeoUtilsTest`, `RouteMatcherTest` | Distance maths, on-route matching, ordering, duplicate merging |
| CSV | `CsvLineParserTest` | Quoted commas, bad rows |
| Geocoding | `PincodeDirectoryGeocodingClientTest` | Directory lookup, fallback, bad lines, coverage of the bundled file |
| HTTP clients | `MapplsRoutingClientTest`, `OsrmRoutingClientTest`, `NominatimGeocodingClientTest` | Request format, response parsing, error mapping, the API key never leaking (`MockRestServiceServer`) |
| Service | `TollPlazaServiceTest` | Business rules and every edge case (Mockito) |
| API | `TollPlazaControllerTest` | Status codes and the JSON error format (`@WebMvcTest`) |
| Full app | `TollPlazaApplicationIntegrationTest` | Real CSV load, idempotent upsert, caching |

**2. Manual: Swagger or Postman** (app running): open `/swagger-ui.html` and use **Try it out**, or import `postman/toll-plaza-service.postman_collection.json` and run it with the Collection Runner. Every request has built-in pass/fail checks, covering the success path, cache speed, the no-tolls case, and all four error cases.

**3. Accuracy against Mappls**: open
<https://www.mappls.com/direction-from-560064,-bengaluru,-karnataka?-to-411045,-pune,-maharashtra>,
click the toll count on the fastest route, and compare its toll names with the response from Postman request 1.

## Deploy (Render, free)

The repo has a `Dockerfile` (a two-stage Maven build, run on Java 17 as a non-root user) and a `render.yaml` Blueprint.

1. Push the repo to GitHub. `.env` is git-ignored and `.dockerignore`d, so the key never leaves your machine.
2. On <https://render.com>, choose **New → Blueprint** and pick the repository. Render reads `render.yaml`.
3. When asked, paste your Mappls key as the value of `MAPPLS_API_KEY`.
4. Once the deploy finishes, open `https://<service>.onrender.com/`. It redirects to the Swagger UI.

**What to expect:**
- **Cold start:** the free tier sleeps after 15 minutes idle, so the first request after that takes about a minute.
- **Restricted key:** if your Mappls key only allows certain domains or IPs, add the Render domain. Otherwise routing fails with 502 and logs `HTTP 403`.

To try the container locally (needs Docker Desktop running):

```bash
docker build -t toll-plaza-service .
docker run -p 8080:8080 --env-file .env toll-plaza-service
```

## API

### `POST /api/v1/toll-plazas`

```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{"sourcePincode":"560064","destinationPincode":"411045"}'
```

**200 OK.** Distances are in km, and plazas are sorted by distance from the source.

```json
{
  "route": { "sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 848.77 },
  "tollPlazas": [
    { "name": "Banglaore-Nelamangala Toll Plaza", "latitude": 13.09062, "longitude": 77.400019, "distanceFromSource": 25.59 }
  ]
}
```

If no plazas lie on the route, the response is the same with `"tollPlazas": []`.

**Errors.** Every error body has the form `{"error": "<message>"}`.

| Case | Status | `error` |
|---|---|---|
| Pincode missing, not 6 digits, or starts with 0 | 400 | Invalid source or destination pincode |
| Pincode not in the directory and not found by the fallback geocoder | 400 | Invalid source or destination pincode |
| Source equals destination | 400 | Source and destination pincodes cannot be the same |
| Body missing or not valid JSON | 400 | Request body is missing or is not valid JSON |
| Wrong Content-Type | 415 | Content-Type must be application/json |
| No drivable route between the pincodes | 404 | No drivable route found between the given pincodes |
| Geocoding or routing provider down | 502 | Routing/Geocoding service is unavailable, please try again later |

A Postman collection is in [`postman/toll-plaza-service.postman_collection.json`](postman/toll-plaza-service.postman_collection.json).

## How it works

```
request ─► validate ─► cache? ─► geocode both pincodes ─► Mappls (or OSRM) route polyline
        ─► bounding box around route (+500 m) ─► indexed DB query for candidate plazas
        ─► for each candidate: distance to nearest polyline segment ≤ 500 m ?
        ─► distance along route to that point = distanceFromSource ─► sort ─► cache ─► respond
```

| Package | Responsibility |
|---|---|
| `controller` | REST endpoint and request validation |
| `service` | Orchestration and response caching (`@Cacheable`) |
| `client` | `GeocodingClient` / `RoutingClient` interfaces. Geocoding is the bundled directory decorating a Nominatim fallback; routing is Mappls or OSRM, picked at startup by `HttpClientConfig`. This is the Strategy pattern: to swap in Google Maps, add a new implementation. |
| `geo` | Pure geometry: haversine distance, point-to-segment projection, bounding box, `RouteMatcher` |
| `loader` | CSV → DB at startup |
| `repository` | Spring Data JPA |
| `exception` | Domain exceptions and a `@RestControllerAdvice` that maps them to the spec's error format |

### Design notes

- **Data loading.** The CSV has 2,385 rows but only 1,536 distinct plazas; the rest are exact duplicates. Rows are de-duplicated in memory, then written in a single batched `MERGE … KEY(name, latitude, longitude)` upsert. Restarting or reloading never creates duplicates. A unique constraint on the natural key enforces this. Plaza names alone aren't unique, so the name can't be the key.
- **Two-stage matching.** A bounding-box query on an indexed `(latitude, longitude)` column cuts about 1,500 plazas down to a few hundred. Exact point-to-polyline distance then runs only on those candidates.
- **Caching.** There are two Caffeine caches: `tollPlazaRoutes` stores full responses per (source → destination) pair, and `pincodeLocations` stores geocodes per pincode, which also keeps the Nominatim fallback within its 1 request/second limit. Repeat requests return in about 20 ms instead of about 2 s.
- **Geocoding without a network call.** `data/pincode_locations.csv` is built from the Department of Posts' All India Pincode Directory (via the MIT-licensed [pincodes-india](https://github.com/dropdevrahul/pincodes-india) mirror of data.gov.in). Each pincode uses the median location of its post offices, which ignores the directory's occasional wrongly placed office. Army Postal Service codes (9xxxxx) aren't real places and are left out. Public Nominatim blocks many cloud-host IPs (it fails from Render), so the directory makes the deployed app independent of it. For 560064 → 411045, the directory's start point gives exactly the same plazas as routing from Mappls' own pincode location.
- **Configuration.** The matching threshold, provider URLs and timeouts are all in `application.properties` under `toll.*`.

### Known limitations

- **Bengaluru 560064 → Pune 411045 with Mappls routing:** about 848 km on NH48 (Mappls' website shows 851.8 km; Mappls' distances vary by a few km between calls, likely with live traffic). We return 20 plazas. That's more than Mappls' 15 because the Hubli–Dharwad bypass has five plazas in the CSV: Gabbur, Tarihal, Kalghatgi, Goa and Narendra. Some of them are on entry/exit ramps right next to the main road. Coordinates alone can't tell a ramp plaza from a main-line plaza.
- **Duplicate plazas in the CSV.** Some plazas appear twice under different names, for example "Khed-Shivapur" / "Khedshivapur", "Dhamnod" / "Delanpur" and "Nellore" / "Nellore (NH- 16)". The two listings can be up to about 1 km apart. Matches within `toll.matching.duplicate-radius-meters` (1,000 m) of each other, both along the route and on the ground, are merged into one. Plazas on the same highway are normally tens of km apart, so this doesn't merge real, separate plazas.
- **OSRM fallback chooses different routes.** Without a Mappls key, OSRM routes 560064 → 411045 via Bijapur and then state roads, and returns 11 plazas.
- A pincode missing from the directory falls back to Nominatim. If that also fails (or is unreachable, as on Render), the pincode is reported as invalid.
