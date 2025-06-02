# Industrial Oven Temperature Control Example

## Project Overview

This project demonstrates a temperature-controlled industrial oven system that uses a combination of RDF (Resource Description Framework), SHACL (Shapes Constraint Language), and Differential Dynamic Logic (dL) to model and reason about its operational states.

The system simulates an oven with two primary modes:
- **OnMode (Heating):** `dx/dt = (2620 – 10·x) / 4000`
    - Active when the temperature `x` is at or below 180°C.
    - Automatically stops when the temperature `x` reaches 180°C.
- **OffMode (Cooling):** `dx/dt = (–10·x + 200) / 4000`
    - Active when the temperature `x` is at or above 180°C.
    - Automatically stops when the temperature `x` cools down to 180°C.

The oven's behavior is modeled across four discrete states, each defined by its mode and temperature range:
- **s11:** `OffMode`, `x ≤ 180°C`
- **s12:** `OnMode`, `x ≤ 180°C`
- **s21:** `OnMode`, `x ≥ 180°C`
- **s22:** `OffMode`, `x ≥ 180°C`

## Inferring State Transitions

The system uses custom Apache Jena `Builtin` functions to infer valid transitions between states. These transitions are only added to the knowledge graph if they are formally proven correct by the **KeYmaera X** theorem prover.

There are two types of transitions:

## Modelica Simulation ⚙️
The Ordinary Differential Equations (ODEs) used in this example were derived from a Modelica simulation model. The simulation file is located at: `D:\RDFDDL-main\Oven_cake\Simulation_Modelica\Oven.mo`.

### a. Continuous Transition (`isNext`)
This transition occurs when the oven remains in the same mode but crosses the 180°C temperature threshold.
- **Examples:**
    - Heating: `s12 → s21` (temperature rises past 180°C)
    - Cooling: `s22 → s11` (temperature drops below 180°C)
- **Proof:** KeYmaera X is used to formally verify that the Ordinary Differential Equation (ODE) governing the current mode will inevitably lead the system from its current temperature range to the next.
- **Output:** If proven, an `ex:next` triple is added to the graph.

### b. Mode Switch (`mode_change`)
This transition occurs when the oven switches modes while the temperature remains within the same region (either high or low).
- **Examples:**
    - `s11 → s12` (turning the oven on at a low temperature)
    - `s21 → s22` (turning the oven off at a high temperature)
- **Proof:** KeYmaera X verifies two dL conditions:
    1. The source state's constraints logically imply the starting condition of its mode.
    2. The ending condition of the source mode is logically equivalent to the starting condition of the target mode.
- **Output:** If proven, a descriptive triple like `example:Oven_OffMode_to_OnMode` is added.

## Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- **(Optional)** KeYmaera X 5.0.1 JAR and Wolfram Engine 13.1 for formal verification.

### Steps
1.  **(Optional) Start KeYmaera X Server:**
    If you intend to run the formal verification, start the KeYmaera X server first.
    ```bash
    java -jar /path/to/KeYmaeraX-5.0.1.jar
    ```


2.  **Build the Project:**
    Navigate to the project's root directory and use Maven to compile the code and run the tests.
    ```bash
    mvn clean install
    ```
    This command will automatically execute the `IsNextBuiltinTest`, which generates the output files.

## Output

The reasoning process generates two Turtle (`.ttl`) files. Based on the test configuration in `IsNextBuiltinTest.java`, these files will be saved to the `D:\TTL2\` directory.

-   **`knowledgeGraphWithSHACL_oven_safe.ttl`**
    This is the initial knowledge graph *before* post-processing. It contains:
    -   The core definitions for the `pre:Oven` and its `pre:ModeRecord` entries (including ODEs and conditions).
    -   The four `pre:State` individuals (s11–s22), each linked to its corresponding SHACL shape.
    -   Inferred raw transitions like `ex:next` and `ex:ModeChange`.

-   **`knowledgeGraphWithSHACL_oven_safe_processed.ttl`**
    This is the final, enriched knowledge graph. It includes:
    -   All triples from the original graph.
    -   The generic `ex:ModeChange` triples are replaced with more descriptive predicates (e.g., `states:Oven_OffMode_to_OnMode`).
    -   Labels are added to the new predicates to make them human-readable (e.g., "Change Oven from OffMode to OnMode").

You can open these files in any text editor or RDF visualization tool to inspect the initial state definitions and the final inferred state-transition graph.