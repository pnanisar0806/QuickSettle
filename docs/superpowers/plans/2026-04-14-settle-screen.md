# Settle Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Settlement List screen (Settle tab) with QR codes, WhatsApp sharing, and end-to-end data flow from Amount tab through Friends tab to Settle tab.

**Architecture:** SettleViewModel receives split data from FriendsViewModel via NavGraph wiring (same cross-tab pattern used for Entry→Crew). It builds SettlementItem list from the computed split, generates UPI URIs and share messages using GenerateUpiLinkUseCase, and exposes state via StateFlow. QrCodeImage composable renders ZXing bitmaps.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, ZXing (QR generation), JUnit 5 + Truth

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `app/src/main/java/com/quicksettle/presentation/screens/settle/SettleViewModel.kt` | Create | State management: settlements list, toggleQr, markPaid, share actions |
| `app/src/main/java/com/quicksettle/presentation/screens/settle/SettleScreen.kt` | Rewrite | Full settle UI: header, settlement cards, QR toggle, glassmorphism CTA |
| `app/src/main/java/com/quicksettle/presentation/components/QrCodeImage.kt` | Rewrite | ZXing QR bitmap composable |
| `app/src/main/java/com/quicksettle/presentation/navigation/NavGraph.kt` | Modify | Wire FriendsViewModel + EntryViewModel into SettleScreen route |
| `app/src/test/java/com/quicksettle/presentation/screens/settle/SettleViewModelTest.kt` | Create | ViewModel unit tests |
| `app/src/test/java/com/quicksettle/presentation/screens/settle/SettlementStatusTest.kt` | Create | Status display text + color mapping tests |

---

### Task 1: SettleViewModel — Data Model and State

**Files:**
- Create: `app/src/main/java/com/quicksettle/presentation/screens/settle/SettleViewModel.kt`

- [ ] **Step 1: Create SettlementStatus enum, SettlementItem data class, and SettleUiState**

```kotlin
package com.quicksettle.presentation.screens.settle

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.quicksettle.data.local.UserProfileStore
import com.quicksettle.domain.usecase.GenerateUpiLinkUseCase
import com.quicksettle.presentation.theme.Error
import com.quicksettle.presentation.theme.OnSurfaceVariant
import com.quicksettle.presentation.theme.TertiaryFixedDim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class SettlementStatus {
    PENDING,
    URGENT,
    DUE_TODAY,
    PAID;

    fun displayText(pendingDays: Int = 0): String = when (this) {
        PENDING -> "PENDING ${pendingDays}D"
        URGENT -> "URGENT"
        DUE_TODAY -> "DUE TODAY"
        PAID -> "PAID"
    }

    fun badgeColor(): Color = when (this) {
        PENDING -> OnSurfaceVariant
        URGENT -> Error
        DUE_TODAY -> OnSurfaceVariant
        PAID -> TertiaryFixedDim
    }
}

data class SettlementItem(
    val participantName: String,
    val description: String,
    val amount: Double,
    val status: SettlementStatus = SettlementStatus.PENDING,
    val pendingDays: Int = 0,
    val upiUri: String,
    val shareMessage: String,
    val showQr: Boolean = false,
    val avatarInitial: String,
    val friendUpiId: String?,
)

data class SettleUiState(
    val totalToCollect: Double = 0.0,
    val activeDebtsCount: Int = 0,
    val settlements: List<SettlementItem> = emptyList(),
    val userVpa: String = "",
    val userDisplayName: String = "",
)
```

- [ ] **Step 2: Create SettleViewModel with setSettlementData and actions**

Add to the same file, below the data classes:

```kotlin
@HiltViewModel
class SettleViewModel @Inject constructor(
    private val generateUpiLinkUseCase: GenerateUpiLinkUseCase,
    private val userProfileStore: UserProfileStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettleUiState())
    val uiState: StateFlow<SettleUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                userVpa = userProfileStore.getUpiId() ?: "",
                userDisplayName = userProfileStore.getDisplayName() ?: "",
            )
        }
    }

    /**
     * Called from NavGraph to populate settlements from the computed split.
     * [splits] maps friendName to (amount, upiId, description).
     */
    fun setSettlementData(
        description: String,
        splits: List<SplitEntry>,
    ) {
        val userVpa = _uiState.value.userVpa
        val items = splits.map { entry ->
            val upiUri = if (userVpa.isNotBlank()) {
                generateUpiLinkUseCase.generateUri(
                    vpa = userVpa,
                    name = _uiState.value.userDisplayName,
                    amount = entry.amount,
                    description = description,
                )
            } else ""
            val shareMessage = generateUpiLinkUseCase.generateShareMessage(
                name = entry.name,
                amount = entry.amount,
                description = description,
                upiLink = upiUri,
            )
            SettlementItem(
                participantName = entry.name,
                description = "Shared: $description",
                amount = entry.amount,
                status = SettlementStatus.PENDING,
                pendingDays = 0,
                upiUri = upiUri,
                shareMessage = shareMessage,
                showQr = false,
                avatarInitial = entry.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                friendUpiId = entry.upiId,
            )
        }
        val nonPaid = items.filter { it.status != SettlementStatus.PAID }
        _uiState.update {
            it.copy(
                settlements = items,
                totalToCollect = nonPaid.sumOf { s -> (s.amount * 100).toLong() } / 100.0,
                activeDebtsCount = nonPaid.size,
            )
        }
    }

    fun toggleQr(index: Int) {
        _uiState.update { state ->
            state.copy(
                settlements = state.settlements.mapIndexed { i, item ->
                    if (i == index) item.copy(showQr = !item.showQr) else item
                },
            )
        }
    }

    fun markPaid(index: Int) {
        _uiState.update { state ->
            val updated = state.settlements.mapIndexed { i, item ->
                if (i == index) item.copy(status = SettlementStatus.PAID) else item
            }
            val nonPaid = updated.filter { it.status != SettlementStatus.PAID }
            state.copy(
                settlements = updated,
                totalToCollect = nonPaid.sumOf { s -> (s.amount * 100).toLong() } / 100.0,
                activeDebtsCount = nonPaid.size,
            )
        }
    }

    /** Returns a combined share message for all non-PAID settlements. */
    fun settleAllMessage(): String {
        return _uiState.value.settlements
            .filter { it.status != SettlementStatus.PAID }
            .joinToString("\n\n") { it.shareMessage }
    }
}

/** Input data for building a settlement item. */
data class SplitEntry(
    val name: String,
    val amount: Double,
    val upiId: String?,
)
```

- [ ] **Step 3: Verify file compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quicksettle/presentation/screens/settle/SettleViewModel.kt
git commit -m "feat(settle): add SettleViewModel with settlement state and actions"
```

---

### Task 2: SettleViewModel Unit Tests

**Files:**
- Create: `app/src/test/java/com/quicksettle/presentation/screens/settle/SettleViewModelTest.kt`

- [ ] **Step 1: Write SettleViewModelTest**

```kotlin
package com.quicksettle.presentation.screens.settle

import com.google.common.truth.Truth.assertThat
import com.quicksettle.data.local.UserProfileStore
import com.quicksettle.domain.usecase.GenerateUpiLinkUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SettleViewModelTest {

    private lateinit var viewModel: SettleViewModel
    private val generateUpiLinkUseCase = GenerateUpiLinkUseCase()

    // Fake UserProfileStore that returns test values
    private lateinit var fakeProfileStore: FakeUserProfileStore

    private val testSplits = listOf(
        SplitEntry(name = "Alice", amount = 250.0, upiId = "alice@upi"),
        SplitEntry(name = "Bob", amount = 150.0, upiId = "bob@upi"),
        SplitEntry(name = "Charlie", amount = 100.0, upiId = null),
    )

    @BeforeEach
    fun setUp() {
        fakeProfileStore = FakeUserProfileStore(
            displayName = "TestUser",
            upiId = "testuser@upi",
        )
        viewModel = SettleViewModel(generateUpiLinkUseCase, fakeProfileStore)
    }

    @Nested
    inner class Initialization {
        @Test
        fun `init loads user profile from store`() {
            val state = viewModel.uiState.value
            assertThat(state.userVpa).isEqualTo("testuser@upi")
            assertThat(state.userDisplayName).isEqualTo("TestUser")
        }

        @Test
        fun `init handles missing profile gracefully`() {
            val emptyStore = FakeUserProfileStore(displayName = null, upiId = null)
            val vm = SettleViewModel(generateUpiLinkUseCase, emptyStore)
            assertThat(vm.uiState.value.userVpa).isEmpty()
            assertThat(vm.uiState.value.userDisplayName).isEmpty()
        }
    }

    @Nested
    inner class SetSettlementData {
        @Test
        fun `setSettlementData populates settlements from splits`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            val state = viewModel.uiState.value
            assertThat(state.settlements).hasSize(3)
            assertThat(state.settlements[0].participantName).isEqualTo("Alice")
            assertThat(state.settlements[0].amount).isEqualTo(250.0)
            assertThat(state.settlements[1].participantName).isEqualTo("Bob")
            assertThat(state.settlements[2].participantName).isEqualTo("Charlie")
        }

        @Test
        fun `totalToCollect sums all non-PAID settlements`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            assertThat(viewModel.uiState.value.totalToCollect).isEqualTo(500.0)
        }

        @Test
        fun `activeDebtsCount counts all non-PAID settlements`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            assertThat(viewModel.uiState.value.activeDebtsCount).isEqualTo(3)
        }

        @Test
        fun `settlements have correct description prefix`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            assertThat(viewModel.uiState.value.settlements[0].description).isEqualTo("Shared: Dinner")
        }

        @Test
        fun `settlements have correct avatar initials`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            assertThat(viewModel.uiState.value.settlements[0].avatarInitial).isEqualTo("A")
            assertThat(viewModel.uiState.value.settlements[1].avatarInitial).isEqualTo("B")
        }

        @Test
        fun `settlements have valid UPI URIs when user VPA is set`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            val uri = viewModel.uiState.value.settlements[0].upiUri
            assertThat(uri).startsWith("upi://pay?pa=testuser@upi")
            assertThat(uri).contains("am=250.00")
            assertThat(uri).contains("cu=INR")
        }

        @Test
        fun `settlements have empty UPI URI when user VPA is blank`() {
            val emptyStore = FakeUserProfileStore(displayName = "User", upiId = null)
            val vm = SettleViewModel(generateUpiLinkUseCase, emptyStore)
            vm.setSettlementData(description = "Dinner", splits = testSplits)
            assertThat(vm.uiState.value.settlements[0].upiUri).isEmpty()
        }

        @Test
        fun `settlements have share messages`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            val msg = viewModel.uiState.value.settlements[0].shareMessage
            assertThat(msg).contains("Alice")
            assertThat(msg).contains("250.00")
            assertThat(msg).contains("Dinner")
        }

        @Test
        fun `all settlements start as PENDING`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.uiState.value.settlements.forEach {
                assertThat(it.status).isEqualTo(SettlementStatus.PENDING)
            }
        }

        @Test
        fun `all settlements start with showQr false`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.uiState.value.settlements.forEach {
                assertThat(it.showQr).isFalse()
            }
        }
    }

    @Nested
    inner class ToggleQr {
        @Test
        fun `toggleQr flips showQr for correct index`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.toggleQr(1)
            val state = viewModel.uiState.value
            assertThat(state.settlements[0].showQr).isFalse()
            assertThat(state.settlements[1].showQr).isTrue()
            assertThat(state.settlements[2].showQr).isFalse()
        }

        @Test
        fun `toggleQr twice returns to false`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.toggleQr(0)
            viewModel.toggleQr(0)
            assertThat(viewModel.uiState.value.settlements[0].showQr).isFalse()
        }
    }

    @Nested
    inner class MarkPaid {
        @Test
        fun `markPaid updates status to PAID`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.markPaid(0)
            assertThat(viewModel.uiState.value.settlements[0].status).isEqualTo(SettlementStatus.PAID)
        }

        @Test
        fun `markPaid recalculates totalToCollect`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.markPaid(0) // Alice 250 marked paid
            assertThat(viewModel.uiState.value.totalToCollect).isEqualTo(250.0) // Bob 150 + Charlie 100
        }

        @Test
        fun `markPaid recalculates activeDebtsCount`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.markPaid(0)
            assertThat(viewModel.uiState.value.activeDebtsCount).isEqualTo(2)
        }

        @Test
        fun `markPaid does not affect other settlements`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.markPaid(1)
            assertThat(viewModel.uiState.value.settlements[0].status).isEqualTo(SettlementStatus.PENDING)
            assertThat(viewModel.uiState.value.settlements[2].status).isEqualTo(SettlementStatus.PENDING)
        }
    }

    @Nested
    inner class SettleAll {
        @Test
        fun `settleAllMessage combines all non-PAID share messages`() {
            viewModel.setSettlementData(description = "Dinner", splits = testSplits)
            viewModel.markPaid(2) // Mark Charlie as paid
            val message = viewModel.settleAllMessage()
            assertThat(message).contains("Alice")
            assertThat(message).contains("Bob")
            assertThat(message).doesNotContain("Charlie")
        }
    }
}

/**
 * Fake UserProfileStore for testing. Avoids EncryptedSharedPreferences
 * which requires an Android context.
 */
class FakeUserProfileStore(
    private val displayName: String?,
    private val upiId: String?,
) : UserProfileStore(fakeContext = true) {
    // This won't work — UserProfileStore uses EncryptedSharedPreferences in constructor.
    // We need to extract an interface. See Step 2.
}
```

**Problem:** `UserProfileStore` creates `EncryptedSharedPreferences` in its constructor, so we can't fake it in unit tests. We need to extract an interface.

- [ ] **Step 2: Extract UserProfileStore interface for testability**

Create `app/src/main/java/com/quicksettle/data/local/UserProfile.kt`:

```kotlin
package com.quicksettle.data.local

/**
 * Read-only interface for accessing the user's profile.
 * Allows faking in unit tests without EncryptedSharedPreferences.
 */
interface UserProfile {
    fun getDisplayName(): String?
    fun getUpiId(): String?
    fun isProfileSetup(): Boolean
}
```

Then modify `UserProfileStore` to implement it. In `app/src/main/java/com/quicksettle/data/local/UserProfileStore.kt`, change the class declaration from:

```kotlin
class UserProfileStore @Inject constructor(@ApplicationContext context: Context) {
```

to:

```kotlin
class UserProfileStore @Inject constructor(@ApplicationContext context: Context) : UserProfile {
```

Add `override` to `getDisplayName()`, `getUpiId()`, and `isProfileSetup()`.

Then update `SettleViewModel` to depend on `UserProfile` instead of `UserProfileStore`:

```kotlin
@HiltViewModel
class SettleViewModel @Inject constructor(
    private val generateUpiLinkUseCase: GenerateUpiLinkUseCase,
    private val userProfile: UserProfile,
) : ViewModel() {
```

And change `userProfileStore` references to `userProfile` in init block.

Add a Hilt binding in `app/src/main/java/com/quicksettle/di/AppModule.kt`:

```kotlin
package com.quicksettle.di

import com.quicksettle.data.local.UserProfile
import com.quicksettle.data.local.UserProfileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindUserProfile(impl: UserProfileStore): UserProfile
}
```

**Note:** The existing `AppModule.kt` may be an empty object — replace it with this abstract class.

- [ ] **Step 3: Update the test with a proper fake**

Replace the `FakeUserProfileStore` at the bottom of the test file with:

```kotlin
class FakeUserProfile(
    private val displayName: String?,
    private val upiId: String?,
) : UserProfile {
    override fun getDisplayName(): String? = displayName
    override fun getUpiId(): String? = upiId
    override fun isProfileSetup(): Boolean =
        !displayName.isNullOrBlank() && !upiId.isNullOrBlank()
}
```

Update `setUp()` and any test that creates a `SettleViewModel` to use `FakeUserProfile` instead of `FakeUserProfileStore`:

```kotlin
private lateinit var fakeProfile: FakeUserProfile

@BeforeEach
fun setUp() {
    fakeProfile = FakeUserProfile(
        displayName = "TestUser",
        upiId = "testuser@upi",
    )
    viewModel = SettleViewModel(generateUpiLinkUseCase, fakeProfile)
}
```

And in the "missing profile" test:
```kotlin
val emptyProfile = FakeUserProfile(displayName = null, upiId = null)
val vm = SettleViewModel(generateUpiLinkUseCase, emptyProfile)
```

And in the "empty VPA" test:
```kotlin
val emptyStore = FakeUserProfile(displayName = "User", upiId = null)
val vm = SettleViewModel(generateUpiLinkUseCase, emptyStore)
```

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest --tests "com.quicksettle.presentation.screens.settle.*" 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quicksettle/data/local/UserProfile.kt \
       app/src/main/java/com/quicksettle/data/local/UserProfileStore.kt \
       app/src/main/java/com/quicksettle/di/AppModule.kt \
       app/src/main/java/com/quicksettle/presentation/screens/settle/SettleViewModel.kt \
       app/src/test/java/com/quicksettle/presentation/screens/settle/SettleViewModelTest.kt
git commit -m "feat(settle): add SettleViewModel with tests, extract UserProfile interface"
```

---

### Task 3: SettlementStatus Display Tests

**Files:**
- Create: `app/src/test/java/com/quicksettle/presentation/screens/settle/SettlementStatusTest.kt`

- [ ] **Step 1: Write SettlementStatusTest**

```kotlin
package com.quicksettle.presentation.screens.settle

import com.google.common.truth.Truth.assertThat
import com.quicksettle.presentation.theme.Error
import com.quicksettle.presentation.theme.OnSurfaceVariant
import com.quicksettle.presentation.theme.TertiaryFixedDim
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SettlementStatusTest {

    @Nested
    inner class DisplayText {
        @Test
        fun `PENDING shows days`() {
            assertThat(SettlementStatus.PENDING.displayText(pendingDays = 3)).isEqualTo("PENDING 3D")
        }

        @Test
        fun `PENDING with zero days`() {
            assertThat(SettlementStatus.PENDING.displayText(pendingDays = 0)).isEqualTo("PENDING 0D")
        }

        @Test
        fun `URGENT text`() {
            assertThat(SettlementStatus.URGENT.displayText()).isEqualTo("URGENT")
        }

        @Test
        fun `DUE_TODAY text`() {
            assertThat(SettlementStatus.DUE_TODAY.displayText()).isEqualTo("DUE TODAY")
        }

        @Test
        fun `PAID text`() {
            assertThat(SettlementStatus.PAID.displayText()).isEqualTo("PAID")
        }
    }

    @Nested
    inner class BadgeColor {
        @Test
        fun `PENDING uses OnSurfaceVariant`() {
            assertThat(SettlementStatus.PENDING.badgeColor()).isEqualTo(OnSurfaceVariant)
        }

        @Test
        fun `URGENT uses Error`() {
            assertThat(SettlementStatus.URGENT.badgeColor()).isEqualTo(Error)
        }

        @Test
        fun `DUE_TODAY uses OnSurfaceVariant`() {
            assertThat(SettlementStatus.DUE_TODAY.badgeColor()).isEqualTo(OnSurfaceVariant)
        }

        @Test
        fun `PAID uses TertiaryFixedDim`() {
            assertThat(SettlementStatus.PAID.badgeColor()).isEqualTo(TertiaryFixedDim)
        }
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew testDebugUnitTest --tests "com.quicksettle.presentation.screens.settle.*" 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/quicksettle/presentation/screens/settle/SettlementStatusTest.kt
git commit -m "test(settle): add SettlementStatus display text and badge color tests"
```

---

### Task 4: QrCodeImage Component

**Files:**
- Rewrite: `app/src/main/java/com/quicksettle/presentation/components/QrCodeImage.kt`

- [ ] **Step 1: Implement QrCodeImage with ZXing**

```kotlin
package com.quicksettle.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders a QR code bitmap from [content] using ZXing.
 *
 * Always renders black-on-white regardless of dark theme.
 * Uses Error correction Level M for balance of size and reliability.
 * Result is memoized on (content, sizePx) to avoid re-encoding.
 */
@Composable
fun QrCodeImage(
    content: String,
    sizeDp: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String = "QR Code",
) {
    val density = LocalDensity.current
    val sizePx = with(density) { sizeDp.roundToPx() }

    val bitmap = remember(content, sizePx) {
        generateQrBitmap(content = content, sizePx = sizePx)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.size(sizeDp),
        )
    }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    if (content.isBlank() || sizePx <= 0) return null
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (bitMatrix[x, y]) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }
        Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    } catch (_: Exception) {
        null
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quicksettle/presentation/components/QrCodeImage.kt
git commit -m "feat(settle): implement QrCodeImage with ZXing QR rendering"
```

---

### Task 5: SettleScreen UI

**Files:**
- Rewrite: `app/src/main/java/com/quicksettle/presentation/screens/settle/SettleScreen.kt`

- [ ] **Step 1: Implement SettleScreen composable**

Replace the entire file with:

```kotlin
package com.quicksettle.presentation.screens.settle

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicksettle.presentation.components.GradientButton
import com.quicksettle.presentation.components.QrCodeImage
import com.quicksettle.presentation.screens.crew.FriendAvatar
import com.quicksettle.presentation.theme.ManropeBold
import com.quicksettle.presentation.theme.ManropeExtraBold
import com.quicksettle.presentation.theme.OnPrimary
import com.quicksettle.presentation.theme.OnSecondaryContainer
import com.quicksettle.presentation.theme.PrimaryFixed
import com.quicksettle.presentation.theme.SecondaryContainer
import com.quicksettle.presentation.util.AmountFormatter

@Composable
fun SettleScreen(
    modifier: Modifier = Modifier,
    viewModel: SettleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            // ── Total to Collect Header ─────────────────────────────────
            item {
                TotalToCollectHeader(
                    totalToCollect = uiState.totalToCollect,
                    activeDebtsCount = uiState.activeDebtsCount,
                )
            }

            // ── Settlement Cards ────────────────────────────────────────
            itemsIndexed(
                items = uiState.settlements,
                key = { index, _ -> index },
            ) { index, item ->
                SettlementCard(
                    item = item,
                    onToggleQr = { viewModel.toggleQr(index) },
                    onShareWhatsApp = { shareWhatsApp(context, item.shareMessage) },
                    onMarkPaid = { viewModel.markPaid(index) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Glassmorphism "Settle All Balances" CTA ─────────────────
        SettleAllCtaBar(
            enabled = uiState.activeDebtsCount > 0,
            onSettleAll = {
                val message = viewModel.settleAllMessage()
                shareGeneric(context, message)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────

@Composable
private fun TotalToCollectHeader(
    totalToCollect: Double,
    activeDebtsCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            text = "TOTAL TO COLLECT",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\u20B9${AmountFormatter.format(totalToCollect)}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = ManropeExtraBold,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Active debts badge
        if (activeDebtsCount > 0) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SecondaryContainer,
            ) {
                Text(
                    text = "\u26A1 $activeDebtsCount ACTIVE DEBT${if (activeDebtsCount != 1) "S" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = OnSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ── Settlement Card ───────────────────────────────────────────────────────

@Composable
private fun SettlementCard(
    item: SettlementItem,
    onToggleQr: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onMarkPaid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPaid = item.status == SettlementStatus.PAID

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
    ) {
        // Top row: avatar + name/desc + amount/status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            FriendAvatar(
                name = item.participantName,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.participantName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\u20B9${AmountFormatter.format(item.amount)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Status badge
                Text(
                    text = item.status.displayText(pendingDays = item.pendingDays),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (item.status == SettlementStatus.DUE_TODAY) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.5.sp,
                    ),
                    color = item.status.badgeColor(),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons row
        if (!isPaid) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Show QR button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleQr),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.showQr) "Hide QR" else "Show QR",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // WhatsApp button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SecondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onShareWhatsApp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = OnSecondaryContainer,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSecondaryContainer,
                        )
                    }
                }
            }
        } else {
            // Paid — show "Mark as Paid" indicator (already paid)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryFixed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Settled",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // QR Code expandable section
        AnimatedVisibility(
            visible = item.showQr && !isPaid,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (item.upiUri.isNotBlank()) {
                    QrCodeImage(
                        content = item.upiUri,
                        sizeDp = 250.dp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = item.upiUri,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = "Set up your UPI ID in profile to generate QR codes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Long-press or tap to mark paid (simple tap for MVP)
        if (!isPaid) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to mark settled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMarkPaid)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

// ── Glassmorphism CTA Bar ────────────────────────────────────────────────

@Composable
private fun SettleAllCtaBar(
    enabled: Boolean,
    onSettleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color(0xCCFFFFFF))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        GradientButton(
            text = "SETTLE ALL BALANCES",
            icon = Icons.Filled.East,
            enabled = enabled,
            onClick = onSettleAll,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Share Helpers ─────────────────────────────────────────────────────────

private fun shareWhatsApp(context: Context, message: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // WhatsApp not installed — fall back to generic share
        shareGeneric(context, message)
    }
}

private fun shareGeneric(context: Context, message: String) {
    val intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        },
        "Share settlement",
    )
    context.startActivity(intent)
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quicksettle/presentation/screens/settle/SettleScreen.kt
git commit -m "feat(settle): implement SettleScreen UI with cards, QR toggle, share actions"
```

---

### Task 6: Wire NavGraph — End-to-End Data Flow

**Files:**
- Modify: `app/src/main/java/com/quicksettle/presentation/navigation/NavGraph.kt`

- [ ] **Step 1: Update NavGraph to pass split data to SettleScreen**

Replace the Settle composable block (lines 71-73) with:

```kotlin
composable(route = Screen.Settle.route) {
    // Access EntryViewModel for description
    val entryBackStackEntry = navController.getBackStackEntry(Screen.Entry.route)
    val entryViewModel: EntryViewModel = hiltViewModel(entryBackStackEntry)
    val entryState by entryViewModel.uiState.collectAsStateWithLifecycle()

    // Access FriendsViewModel for split data
    val crewBackStackEntry = navController.getBackStackEntry(Screen.Crew.route)
    val friendsViewModel: FriendsViewModel = hiltViewModel(crewBackStackEntry)
    val friendsState by friendsViewModel.uiState.collectAsStateWithLifecycle()

    val settleViewModel: SettleViewModel = hiltViewModel()

    // Forward split data whenever friends state changes
    LaunchedEffect(friendsState.selectedFriends, friendsState.splitMode, friendsState.includeSelfInSplit) {
        val (amounts, _) = friendsState.computeSplit(
            com.quicksettle.domain.usecase.CalculateSplitUseCase()
        )
        val description = entryState.description.ifBlank { "Bill Split" }
        val splits = friendsState.selectedFriends
            .filter { sf -> (amounts[sf.friend.id] ?: 0.0) > 0.0 }
            .map { sf ->
                SplitEntry(
                    name = sf.friend.name,
                    amount = amounts[sf.friend.id] ?: 0.0,
                    upiId = sf.friend.upiId,
                )
            }
        settleViewModel.setSettlementData(description = description, splits = splits)
    }

    SettleScreen(viewModel = settleViewModel)
}
```

Add the missing import at the top of NavGraph.kt:

```kotlin
import com.quicksettle.presentation.screens.settle.SettleViewModel
import com.quicksettle.presentation.screens.settle.SplitEntry
```

**Important:** The `CalculateSplitUseCase()` instantiation inside `LaunchedEffect` is needed because `computeSplit` is an extension function on `FriendsUiState` that takes a `CalculateSplitUseCase` parameter. This is a stateless use case with no dependencies, so constructing it inline is fine.

- [ ] **Step 2: Verify compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all tests to confirm no regressions**

Run: `./gradlew testDebugUnitTest 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quicksettle/presentation/navigation/NavGraph.kt
git commit -m "feat(settle): wire NavGraph to pass split data from Crew to Settle tab"
```

---

### Task 7: Full Build Verification

- [ ] **Step 1: Run debug build**

Run: `./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all unit tests**

Run: `./gradlew testDebugUnitTest 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 3: Commit any remaining changes and update phase checklist**

Update `.claude/memory/project_phase_checklist.md` — replace the Phase 5 section with verified items:

```markdown
### Phase 5 — Settle Screen: COMPLETE
- [x] SettleViewModel — state management with settlements, toggleQr, markPaid, settleAll
- [x] UserProfile interface extracted for testability, Hilt binding in AppModule
- [x] SettleViewModelTest — 16 tests (init, setSettlementData, toggleQr, markPaid, settleAll)
- [x] SettlementStatusTest — 8 tests (displayText, badgeColor for all statuses)
- [x] QrCodeImage — ZXing QR rendering, black-on-white, memoized on (content, sizePx)
- [x] SettleScreen — TotalToCollect header, active debts badge, settlement cards
- [x] Settlement cards — avatar, name, description, amount, status badge, Show QR + WhatsApp buttons
- [x] QR code toggle with AnimatedVisibility (fadeIn + expandVertically)
- [x] WhatsApp share intent with fallback to generic share
- [x] Glassmorphism "Settle All Balances" CTA bar
- [x] NavGraph wired: Entry→Crew→Settle end-to-end data flow
- [x] assembleDebug + testDebugUnitTest pass
```
