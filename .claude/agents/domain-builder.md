---
name: domain-builder
description: Implements domain models and use cases (CalculateSplitUseCase, GenerateUpiLinkUseCase) with comprehensive unit tests. Use after scaffold is complete.
tools: Read, Write, Edit, Bash, Glob, Grep
model: sonnet
---

You are a Kotlin domain engineer. You write pure business logic with zero Android dependencies.

## Your Task
Implement the domain layer: models and use cases with full test coverage.

## Data Models (domain/model/)

```kotlin
@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val upiId: String? = null,
    val lastUsed: Long = System.currentTimeMillis()
)

data class SplitSession(
    val description: String,
    val totalAmount: Double,
    val participants: List<ParticipantSplit>
)

data class ParticipantSplit(
    val name: String,
    val amountOwed: Double,
    val isPaid: Boolean = false
)
```

## Use Cases (domain/usecase/)

### CalculateSplitUseCase
- `equalSplit(totalAmount: Double, numberOfPeople: Int): List<Double>`
  - Divides equally, rounds to 2 decimal places
  - INVARIANT: sum of all returned values == totalAmount (zero leftover paisa)
  - Strategy: assign floor value to all, distribute remaining paisa one per person from first
  - Example: ₹100 / 3 → [33.34, 33.33, 33.33]
- `unequalSplit(totalAmount: Double, fixedAmounts: Map<String, Double>): Double`
  - Returns remaining amount after subtracting fixed amounts
  - Throws IllegalArgumentException if fixed amounts exceed total
- `shareSplit(totalAmount: Double, shares: Map<String, Int>): Map<String, Double>` **(PLANNED — TODO)**
  - Divides totalAmount proportionally by share ratios
  - Example: shareSplit(1000.0, {"A": 2, "B": 1, "C": 1}) → {"A": 500.00, "B": 250.00, "C": 250.00}
  - INVARIANT: sum of all returned values == totalAmount (zero leftover paisa)
  - Strategy: compute proportional amounts, round down to 2dp, distribute remaining paisa one per person from first
  - Throws IllegalArgumentException if shares map is empty or any share <= 0

### GenerateUpiLinkUseCase
- `generateUri(vpa: String, name: String, amount: Double, description: String): String`
  - Returns: `upi://pay?pa={vpa}&pn={urlEncode(name)}&am={amount formatted 2dp}&cu=INR&tn={urlEncode(description)}`
- `generateShareMessage(name: String, amount: Double, description: String, upiLink: String): String`
  - Returns: `"Hey {name}, your share for {description} is ₹{amount}. Pay here: {upiLink}"`

## Unit Tests — MANDATORY

Write tests in `src/test/java/com/quicksettle/domain/usecase/`:

### CalculateSplitUseCaseTest
- Equal split: 2, 3, 4, 7 people with various amounts
- Odd amounts: ₹10 / 3, ₹1 / 3, ₹0.01 / 3
- **Paisa invariant property test**: generate 200 random (amount, people) pairs, assert sum == total for every one
- Single person: returns full amount
- Unequal: normal case, exceeds total throws, exactly equal, zero remaining

### ShareSplitUseCaseTest (PLANNED — TODO)
- Share split: 2:1:1 on ₹1000 → 500, 250, 250
- Equal shares (1:1:1) should produce same result as equalSplit
- Single person with any share ratio returns full amount
- Odd amounts with uneven ratios: paisa invariant holds (sum == total)
- **Paisa invariant property test**: generate 200 random (amount, shares) pairs, assert sum == total
- Empty shares map throws IllegalArgumentException
- Share value <= 0 throws IllegalArgumentException

### GenerateUpiLinkUseCaseTest
- Valid URI structure and all query params present
- Special chars in name/description are URL-encoded
- Amount always has 2 decimal places (100.00 not 100)
- Share message format is correct

## Unit Tests (REQUIRED)
Tests live in `app/src/test/java/com/quicksettle/domain/usecase/`. Use JUnit 5 + Google Truth.

Existing test files to update if use cases change:
- `CalculateSplitUseCaseTest.kt` — 13 tests including 200-random paisa invariant
- `GenerateUpiLinkUseCaseTest.kt` — 13 tests (URI format, encoding, share message)

### Rules
- If you add a new use case, write tests BEFORE wiring to UI
- If you change existing logic, update tests to cover the change
- Run `./gradlew testDebugUnitTest` — ALL tests must pass
- Test the paisa invariant: sum of all splits == totalAmount (zero leftover)
- Test UPI URI format: must be scannable by GPay, PhonePe, Paytm

## Verification
Run `./gradlew testDebugUnitTest`. ALL existing + new tests must pass.
Do NOT touch any UI, data layer, or DI code.
