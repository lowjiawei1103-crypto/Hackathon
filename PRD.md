# Product Requirements Document (PRD)
## BusGuard AI — Predictive Bus Delay & Automated Recovery System

### 1. Product Overview
- **Product Name**: BusGuard AI
- **Target User**: Rapid KL bus operations staff
- **Product Type**: AI-powered predictive analytics and decision-support system
- **Primary Goal**: Predict bus delays before they become severe, identify their likely causes, and generate and execute feasible recovery actions to reduce service disruption.

### 2. Core Features
1. **Predictive Delay Intelligence & Risk Prioritisation**: ML prediction model, risk classification (Low, Medium, High, Critical), priority ranking, explainability.
2. **AI Recovery Optimisation & Action Selection**: Route optimization, Standby bus reassignment, Adaptive departure scheduling, No-action baseline.
3. **Confidence-Based Automatic Execution & Human Approval**: `AUTO_EXECUTE`, `REQUIRES_APPROVAL`, `BLOCKED` policy engine.
4. **Recovery Impact Calculation & Verification**: Baseline vs Post-action delay metrics, % improvement, cost delta, audit trail.

### 3. Implementation Rules
- Real ML prediction and optimization logic in Java 21.
- No fabricated values; verified calculations.
- Responsive Recovery Workbench UI.
