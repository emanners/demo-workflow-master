# Architecture Overview

## Spring Boot Monoliths on Fargate for Faster Development
* Services Module API (Register, OpenAccount, Deposit, Payout) in one self‑contained service aka MessageConsumer. A
* Another monolith for message processing Worker Module 
  *(Later expand to individual services/containers.)*

## AWS Fargate + ALB
* Deploy Dockerized Spring apps without managing servers.

## Event‑Driven Workflow
* **API** pushes events to **EventBridge** → **SQS** → **Worker** consumes and updates **DynamoDB**. 
* However, within the timeframe EventBridge wasn't configured correctly, so reverted to Direct SQS (EB future proofing)

## DynamoDB
* Workflow‑events registration.

---

## API Layer (Serices module)

* Exposed a single HTTP endpoint per workflow action (register, open‑account, deposit, payout) via a Spring Boot application behind an AWS Application Load Balancer (ALB).
* Using a small set of REST endpoints kept the design straightforward and fit naturally with React Native’s networking model.
* An ALB allows integration with Cognito, SSL, and high‑throughput routing without extra infrastructure. *(API Gateway not needed in this setup).*

---

## Compute Platform

* Packaged the Spring Boot apps: servies and worker as Docker images and deployed them to Amazon ECS on **Fargate**.
* Fargate removes the need to manage EC2 instances and simplifies capacity planning. It also integrates smoothly with the CI/CD pipeline and IAM roles.
* This is also a good step in the direction of EKS, if required.

---

## Asynchronous Processing

* All incoming events are published to **Amazon EventBridge** with `detailType` tags and forwarded into an **SQS** queue.
* A separate “worker” service (another Fargate task) processes events, and updates **DynamoDB**.
* Decoupling the API from long‑running business logic ensures responsiveness and resilience. EventBridge → SQS is a fully managed, highly available pattern that we can extend later to multiple consumers or retries.

---

## Data Store

* **DynamoDB** as the sole persistence layer, with a table keyed by `eventId`.
* DynamoDB’s low‑latency millisecond reads and writes fit the requirement to ingest and retrieve events at scale.
* Its flexible schema simplifies storing diverse event payloads without upfront migrations. *(Could have used an RDBMS, but that’s a discussion in its own right, requirements etc.)*

---

## Infrastructure as Code & CI/CD

* All AWS resources are defined in **Terraform**; application builds, Docker pushes, and infra plan/apply steps run in a **GitHub Actions** workflow.
* Terraform gives versioned, reviewable infra changes. GitHub Actions ties code commits to automated builds, tests, and deploys—enabling rapid iteration and a clear audit trail.

---

## Frontend & Data Visualization

* Adapted the default React Native template and added **React Native Web** support for a hosted web preview (via Netlify).
* The app polls the `/events` GET endpoint to render a list of all ingested events.
* Polling a simple REST API keeps the initial UI implementation minimal, demonstrating end‑to‑end data flow in a single codebase that works on mobile and web.
* In future, we could swap polling for WebSockets or SSE for real‑time updates.

---

## Observability & Monitoring

* Added basic **Log4j** logging, leveraging **CloudWatch Logs** for both API and worker tasks; set up basic health‑check metrics on the ALB and ECS service alarms.
* Rationale: CloudWatch provides out‑of‑the‑box dashboards and alerts. In a production rollout, we’d layer in synthetics, X‑Ray tracing, and SNS‑backed alarms for rapid incident response.

---

## Testing Strategy

* Basic unit tests implemented for JSON/serialization.
* Should add more testing for core service logic (using JUnit 5 + Mockito) and a small end‑to‑end smoke test that calls the live API and inspects DynamoDB (if time allowed).
* Going forward, add contract tests for each event type, plus integration tests in CI against a local DynamoDB and a mocked EventBridge/SQS stack.

---

## Additional Security & UX Considerations

* Integrate **AWS Cognito** (or OAuth/OIDC) to authenticate users before opening WebSockets or consuming APIs.
* Cache tokens securely (SecureStore) and retry connections gracefully.
* Implement graceful error states (offline banners, reconnect prompts) to keep users informed.

## TODOs
* common code in separate JAR (model, config code) 
* clean up of properties
* unit tests are very light
* restructuring of security/roles in AWS.

## End Notes

* Various AI tools assisted me in building out the boilerplate code, including ci/cd/terraform aspects. 
* I really enjoyed working on this, apart from some of the tedious security/role issues I encountered :-)

