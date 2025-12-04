# DrumBoiler Verification Pipeline

Implementation of the FMU → RDF/SHACL → differential dynamic logic (dL) pipeline described in `DEV_NOTES.md`.

## Build

```powershell
cd drumboiler
mvn clean package
```

## Inputs

- `fmu/DrumBoiler.fmu` – pre-exported FMU archive containing `modelDescription.xml`.
- `config/drumboiler_config.json` – discretisation, state transitions, and simplified ODEs. Adjust this file to refine state ranges without touching code.

## Run

```powershell
java -jar target/drumboiler-0.1.0-SNAPSHOT.jar `
  --fmu fmu/DrumBoiler.fmu `
  --config config/drumboiler_config.json `
  --output build
```

Outputs inside `build/`:

- `drumboiler_shapes.ttl` – RDF + SHACL skeleton with `pre:` and `sh:` prefixes.
- `obligations/obligation_<src>_to_<tgt>.kyx` – KeYmaera X proof obligations for every configured transition.
- `verification_summary.json` – summary list with obligation paths (placeholder status until KeYmaera X integration).

The driver logs each step and can be extended to invoke KeYmaera X or additional tooling once the obligations exist.

### Optional: auto-verify with KeYmaera X

1. Start KeYmaera X as a server (default: http://localhost:8090), e.g.:
   ```powershell
   java -jar "path/to/keymaerax.jar" --server 8090
   ```
2. Run the pipeline with `--verify true`:
   ```powershell
   java -jar target/drumboiler-0.1.0-SNAPSHOT-jar-with-dependencies.jar `
     --fmu fmu/DrumBoiler.fmu `
     --config config/drumboiler_config.json `
     --output build `
     --verify true
   ```
The status field in `verification_summary.json` will be updated with the KeYmaera X result (`true/false/unknown` or `error:*`).
