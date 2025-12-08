## FMU → RDF/SHACL → dL Automation Notes

Applies to all cases (DrumBoiler, Oven, EmptyTank, Yogurt, ...), same pipeline:

1) FMU parsing  
   - Each case stores its FMU in its own `fmu/` folder (e.g., `fmu/DrumBoiler.fmu`, `fmu/Oven.fmu`, `fmu/EmptyTanks.fmu`).  
   - Extract `modelDescription.xml`, parse `ScalarVariable`: `name`, `valueReference`, `unit/declaredType`, `causality`, `variability`.

2) RDF + SHACL  
   - Prefixes: `pre = http://anonymous.example.org#`, `sh = http://www.w3.org/ns/shacl#`.  
   - Represent tracked physical quantities as `pre:<var>` (type `pre:Variable`, with `pre:unit`).  
   - Build SHACL NodeShapes for discretized states using hasValue/min/max constraints.  
   - Output TTL (per case, e.g., `build/<case>_shapes.ttl`).

3) dL obligations  
   - `config/<case>_config.json` defines states, modes, ODEs, and candidate transitions; if transitions are absent, auto-generate Cartesian-product isNext/ModeChange candidates.  
   - Convert SHACL constraints to predicates φ_src / φ_tgt.  
   - Emit KeYmaera X formulas: isNext as `φ_src -> [HP & (φ_src ∨ φ_tgt)] φ_src`; ModeChange uses start/end conditions.  
   - Output `build/obligations/<...>.kyx`.

4) Driver / CLI  
   - Args: `--fmu`, `--config`, `--output`; optional `--verify` (KeYmaeraX), `--stats-from-csv`, `--fit-ode-from-csv`, etc.  
   - Flow: parse FMU → generate RDF/TTL → generate dL obligations → (optional) verify → write JSON summary and verified RDF edges.

Implementation constraints: Java 17, Maven, Apache Jena for RDF/SHACL, DOM/JAXP for XML.  
Optional extensions: simulation-CSV stats / linear ODE fitting, or LLM-assisted state/mode suggestions; final correctness is via SHACL + dL/KeYmaeraX.
