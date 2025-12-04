# Industrial Empty Tank Example

## 1. Project Overview

This project demonstrates a continuous empty tank filling process, with 2 devices, 2‑dimensional, non‑linear ODE.

The system simulates an oven with two primary modes:
- **OnMode (Flowing On):** `h1' = -0.0417*(h1 - h2 + 1)^(1/2)`, `h2' = 0.0417*(h1 - h2 + 1)^(1/2)`
    - Active when the Tank 1 level `h1` is `1 m` and Tank 2 level `h2` is `0 m`.
    - Automatically stops when the `h1` reaches `0 m` and `h2` reaches `1 m`.
- **OffMode (Flowing Off):** `h1'=0`, `h2'=0`
    - Active when the Tank 1 level `h1` is `0 m` and Tank 2 level `h2` is `1 m`.
    - Automatically stops when the `h1` reaches `1 m` and `h2` reaches `0 m`.

The system behavior is modeled across four discrete states, each defined by its mode and the liquid level of tank 1 and tank 2:
- **s0:** `OffMode`, `h1=1m & h2=0m`
- **s1:** `OnMode`, `h1=1m & h2=0m`
- **s2:** `OnMode`, `0m<h1<1m & 0m<h2<1m`
- **s3:** `OnMode`, `h1=0m & h2=1m`
- **s4:** `OffMode`, `h1=0m & h2=1m`