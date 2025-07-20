# RDFdL: Integrating RDF with Differential Dynamic Logic

This repository implements the **RDFdL Verification Pipeline**, connecting RDF/SHACL modeling with differential dynamic logic (dL) proofs to model and validate Cyber-Physical Systems (CPS) in a mathematically guaranteed way.

## 1. Project Introduction 

The core of this project is the **"RDFdL Verification Pipeline"**, which operates as follows:

1.  **Modeling with RDF/SHACL**: We model a Cyber-Physical System—its components, states, and physical constraints—using the Resource Description Framework (RDF). The Shapes Constraint Language (SHACL) is used to define the precise rules and conditions for each state.
<<<<<<< HEAD
2.  **SHACL Validation**: Before inferring any transitions, each RDF state is checked against its SHACL shape (`Validation.java`). This ensures no invalid state (one that violates the predefined constraints) enters the reasoning or proof stages.
3.  **Reasoning with Jena**: The Apache Jena framework is used to reason the validated RDF graph and propose potential state transitions based on the system’s logic (e.g., continuous evolution or mode switches). These inferences are added as candidate triples (e.g., `ex:next`, `ex:ModeChange`).
4.  **Proof with KeYmaera X**: For each inferred transition, the system generates a formal proof obligation expressed in Differential Dynamic Logic (dL). This proof is sent to the **KeYmaera X** theorem prover for validation.
5.  **Verified State Graph**: A transition is only accepted and added to the final knowledge graph if KeYmaera X successfully proves its logical correctness. The final output is a fully verified state-transition graph of the hybrid system.
=======
2.  **Reasoning with Jena**: The Apache Jena framework is used to examine the validated RDF graph and propose potential state transitions based on the system’s logic (e.g., continuous evolution or mode switches). These inferences are added as candidate triples (e.g., `ex:next`, `ex:ModeChange`).
3.  **Proof with KeYmaera X**: For each inferred transition, the system generates a formal proof obligation expressed in Differential Dynamic Logic (dL). This proof is sent to the **KeYmaera X** theorem prover for validation.
4.  **Verified State Graph**:    The final output is an RDF graph in which every transition edge is backed by a successful dL proof, producing a fully verified hybrid-system state machine.
>>>>>>> eb8f28d (Add Example)

This approach ensures that the CPS model is both semantically rich (via RDF/SHACL) and formally safe (via KeYmaera X proofs).

## 2. Modules Overview 

This repository contains two examples to demonstrate the RDFdL pipeline:

* **`Yogurt_example/`**: A multi-stage yogurt manufacturing process. Involves multiple devices (heater, homogenizer, pasteurizer, cooler) and showcases cross-device handoffs and multi-mode transitions.
* **`Oven_cake/`**: A single-device example: an industrial oven controlling temperature. Models heating and cooling dynamics and infers transitions between four discrete states (two modes × two temperature regions).

## 3. Running Each Example 🚀

#### Yogurt Example 

* **Test Entry Point**: `Yogurt_example/src/test/java/IsNextBuiltinTest.java`
* **SHACL Validation**: `Yogurt_example/src/test/java/Validation.java` checks each state against SHACL shapes. Run separately with:  
  ```bash
  mvn test -pl Yogurt_example -Dtest=Validation
* **To Run**: The test automatically executes all reasoning rules, generating two TTL output files: one with the original inferred relations and one that is post-processed with more descriptive relationship names.
* **To Verify**: If you wish to verify a transition in KeYmaera X, you can use the `Prove_helper.java` class, which constructs and sends the dL proof requests.

#### Oven Example 

* **Test Entry Point**: `Oven_cake/src/test/java/IsNextBuiltinTest.java`
* **SHACL Validation**: `Oven_cake/src/test/java/Validation.java` checks each state against SHACL shapes. Run separately with:  
  ```bash
  mvn test -pl Oven_cake -Dtest=Validation
* **To Run**: Running this test generates the `knowledgeGraphWithSHACL_oven_safe_processed.ttl` file, which contains the final, inferred state-transition graph.
* **To Verify**: To validate the oven's temperature control safety properties, the dL script generated via `Prove_helper.java` can be imported into KeYmaera X for analysis.

## 4. Quick Start Guide ⚡

#### Prerequisites
* **Java**: Version 11+ (the project is configured for 23)
* **Maven**: To build the project.
* **KeYmaera X**: v5.0.1 for formal verification.
* **Wolfram Engine**: v13.1, as an ODE solver for KeYmaera X.

#### Build & Test Commands
From the root directory (`RDFDDL-main/`), run the following commands:

```bash
# Build all modules
 
mvn clean install

# Run the Yogurt example test specifically
mvn test -pl Yogurt_example -Dtest=IsNextBuiltinTest

# Run the Oven example test specifically
mvn test -pl Oven_cake -Dtest=IsNextBuiltinTest
