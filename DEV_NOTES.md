## DrumBoiler Automation Notes

Target: automate FMU → RDF/SHACL → differential dynamic logic (dL) obligations for verifying
`Modelica.Fluid.Examples.DrumBoiler.DrumBoiler`.

Pipeline outline:

1. **FMU ingestion**  
   - FMU file lives under `fmu/` (e.g. `fmu/DrumBoiler.fmu`).  
   - Extract `modelDescription.xml`.  
   - Parse all `ScalarVariable` entries → keep `name`, `valueReference`, `unit/declaredType`, `causality`, `variability`.

2. **RDF + SHACL skeleton**  
   - Namespace: `pre = http://anonymous.example.org#`, `sh = http://www.w3.org/ns/shacl#`.  
   - Represent tracked physical variables as `pre:<var>` instances of `pre:Variable` with `pre:unit`.  
   - Define SHACL NodeShapes for discretised boiler states (low/normal/high pressure, etc.).  
   - NodeShapes constrain temperature/pressure/volume flow ranges using SHACL property constraints.  
   - Emit TTL file (default `drumboiler_shapes.ttl`).

3. **dL obligations**  
   - Config file defines transitions between states + simplified ODEs.  
   - Translate SHACL constraint ranges into predicates φ_src / φ_tgt.  
   - Emit KeYmaera X problems of the form `φ_src -> [α & (φ_src ∨ φ_tgt)] φ_src`.  
   - α = hybrid program generated from config ODEs + evolution domain.  
   - Output `obligation_<src>_to_<tgt>.kyx`.

4. **Driver**  
   - CLI takes `--fmu`, `--config`, optional `--output`.  
   - Steps: parse FMU → generate RDF/Turtle → emit obligations → (optionally) call KeymaeraX.  
   - Produce JSON summary with obligation file paths + verification status placeholders.

Implementation constraints: Java 17, Maven project, Apache Jena for RDF/SHACL, DOM/JAXP for XML parsing.
