# Class Profile: ExtractionController

**File Path**: `com/example/aikef/controller/ExtractionController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages structured data extraction tasks. It provides APIs to define extraction schemas (what fields to extract), create extraction sessions, submit text for processing, and retrieve extracted results. This is key for converting unstructured customer inputs into structured data (e.g., order forms).

# Method Deep Dive

## Schema Management
- **`getSchemas()`, `getSchema(id)`**: Retrieves extraction schemas (blueprints for extraction).
- **`createSchema(...)`**: Defines a new extraction schema with fields, types, and prompts.

## Session Management
- **`createSession(...)`**: Starts a new extraction session based on a schema. Links to a reference (e.g., a ChatSession).
- **`getSession(id)`**: Checks the current status, extracted data, and missing fields.
- **`submitText(...)`**: Sends user input to the session. The service uses LLM to fill slots.
- **`updateField(...)`**: Manually overrides or fills a specific field value.
- **`completeSession(id)`**: Finalizes the extraction.
- **`cancelSession(id)`**: Aborts the session.

## Shortcuts
- **`extractOnce(...)`**: A utility endpoint that combines session creation and text submission for single-turn extraction needs.

# Dependency Graph

**Core Dependencies**:
- `StructuredExtractionService`: Core logic for LLM-based extraction.
- `ExtractionSchemaRepository`: Schema storage.
- `AgentPrincipal`: Security context to identify the creator.

**Key Imports**:
```java
import com.example.aikef.extraction.service.StructuredExtractionService;
import com.example.aikef.extraction.model.ExtractionSchema;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Typical Flow
1. **Create Schema**: Define "Shipping Address" schema (Street, City, Zip).
2. **Start Session**: `POST /api/v1/extraction/sessions` with schema ID.
3. **Loop**:
   - `POST .../submit` with user text ("I live in New York").
   - Check response: `missingFields` = ["Street", "Zip"].
   - Ask user: "What is your street and zip?"
   - `POST .../submit` with answer ("123 Main St, 10001").
   - Check response: `missingFields` is empty.
4. **Complete**: `POST .../complete`.

# Source Link
[ExtractionController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/ExtractionController.java)
