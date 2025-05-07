# Architecture Overview

## Spring Boot Monoliths on Fargate for Faster Development
* Services Module API (Register, OpenAccount, Deposit, Payout) in one self‑contained service (Message Consumer)
* Worker Module - For the core processing (Message Processor)
  *(Later expand to individual services/containers.)*
  *This is setup across 2 AZs*

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

## Frontend & Data Visualization (see ui/reactnative)

* Adapted the default React Native template and added **React Native Web** support for a hosted web preview (via Netlify not working out of the box :-).
* The app polls the `/events` GET endpoint to render a list of all ingested events.
* Polling a simple REST API keeps the initial UI implementation minimal, demonstrating end‑to‑end data flow in a single codebase that works on mobile and web.
* In future, we could swap polling for WebSockets or SSE for real‑time updates.
* Tested on Android Emulator (I have this setup already from my previous projects)

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

# Solance Workflow Platform API

**Base URL:** `http://solance-cluster-alb-1606409103.eu-west-1.elb.amazonaws.com`

## API Endpoints

### 1. Register Customer

**Endpoint:** `POST /api/v1/register`

**Request Body:**
```json
{
  "userId": "alice1",
  "fullName": "Alice Smith",
  "email": "alice@example.com"
}
```

**Example Request:**
```bash
curl -i -X POST http://solance-cluster-alb-1606409103.eu-west-1.elb.amazonaws.com/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice1",
    "fullName": "Alice Smith",
    "email": "alice@example.com"
  }'
```

**Example Response:**
```json
{
  "eventId": "d3e1f9b2-12ab-4c5d-9e6f-abcdef123456"
}
```

### 2. Payout Example

**Endpoint:** `POST /api/v1/payout`

**Request Body:**
```json
{
  "userId": "alice1",
  "accountId": "acct-001",
  "currency": "EUR",
  "amount": 100.00,
  "transactedAt": "2025-05-06T11:00:00Z",
  "beneficiaryIban": "DE89370400440532013000",
  "paymentRef": "invoice-123",
  "purposeRef": "subscription fee"
}
```

**Example Request:**
```bash
curl -i -X POST http://solance-cluster-alb-1606409103.eu-west-1.elb.amazonaws.com/api/v1/payout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice1",
    "accountId": "acct-001",
    "currency": "EUR",
    "amount": 100.00,
    "transactedAt": "2025-05-06T11:00:00Z",
    "beneficiaryIban": "DE89370400440532013000",
    "paymentRef": "invoice-123",
    "purposeRef": "subscription fee"
  }'
```

### 3. Get All Events

**Endpoint:** `GET /api/v1/events`

**Example Request:**
```bash
curl -i http://solance-cluster-alb-1606409103.eu-west-1.elb.amazonaws.com/api/v1/events
```

**Example Response:**
```json
[
  {
    "eventId": "d3e1f9b2-12ab-4c5d-9e6f-abcdef123456",
    "detailType": "RegisterCustomer",
    "status": "COMPLETED"
  },
  {
    "eventId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
    "detailType": "Deposit",
    "status": "RECEIVED"
  }
]
```


> **Note:** Event status will typically show as "RECEIVED" when initially processed by the API layer, and "COMPLETED" after being processed by the workflow processor.

## Local Development

To run the full stack locally:
1. Use `localstack.sh` and `docker-compose.yml` to configure the environment
2. Run both modules with Maven using the local profile
