# Walkthrough - Haifa Phase 10 Model Retries & Safety Enhancements

We successfully borrowed and adapted key engineering and safety mechanisms from `deer-flow/` to enhance Haifa's Memory Candidates, Persona boundaries, and User Isolation. We also implemented a robust retry mechanism for model calls.

## Key Enhancements

### 1. Model Call Retry Mechanism
- Enhanced `SpringAiAgentModelClient.java` with a reactive retry chain:
  - Appended `.retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3)))` to the reactive execution flow.
  - Used `.onRetryExhaustedThrow((spec, signal) -> signal.failure())` to propagate the original exception (e.g. Netty `ReadTimeoutException` or HTTP `TimeoutException`) on exhaustion instead of wrapping it, ensuring cleaner failure logging.
  - Added warning logging during each retry attempt detailing attempt count and failure cause.

### 2. Structured `sourceError` Parameter
- Added `sourceError` field to `MemoryFactEntity`, `MemoryFactRecord`, `MemoryCandidateEntity`, and `MemoryCandidateRecord`.
- Updated converters in `MemoryFactStore` and `MemoryCandidateStore` to map `sourceError`.
- Propagated it through the LLM reflection extraction prompt and parser inside `MemoryReflectionService` to store the prior wrong approach description when a correction signal is processed.

### 3. Session and Upload Temporary Message Filtering
- Implemented `UPLOAD_SENTENCE_RE` inside `MemoryReflectionService` to strip out transient session-specific events, file upload mentions, and temporary search details from memory content before saving candidates, matching the logic in `deer-flow`.

### 4. Memory Fact Deduplication
- Added active fact content deduplication (trimmed and case-insensitive check) to candidate `ADD` approvals in `MemoryController` to prevent duplicate facts for a single user scope.

### 5. Persona Rules Validation
- Added strict validations in `savePersona` (rejecting empty names or souls, enforcing a 10,000-character length limit, and validating that only one enabled persona can exist per `(userId, agentId)` context).

## Verification & Tests
- Updated `MemoryReflectionServiceTest` and `StructuredMemoryMiddlewareTest` constructor mappings.
- Wrote new tests in `SpringAiAgentModelClientTest` verifying that a failing client successfully retries 3 times with 3-second delay on timeout and general failures, and propagates the underlying error when exhausted.
- Wrote new tests in [MemoryControllerTest.java](file:///d:/workspace/haifa/haifa-ai/haifa-ai-deerflow/src/test/java/org/wrj/haifa/ai/deerflow/web/MemoryControllerTest.java) verifying:
  - Required field persona checks (empty name/soul, character limits);
  - Single active persona constraint enforcement;
  - Duplicate memory fact suppression on `ADD` candidate approval.
- Verified backend builds cleanly:
  ```bash
  mvn -pl haifa-ai/haifa-ai-deerflow -am test
  ```
  Result: **BUILD SUCCESS** (223 tests passed, 0 failures).
- Verified frontend compiles cleanly:
  ```bash
  npm run build
  ```
  Result: **Vite build compiled successfully**.
