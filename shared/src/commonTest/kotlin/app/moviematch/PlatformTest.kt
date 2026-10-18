package app.moviematch

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun platformNameIsNotBlank() {
        assertTrue(
            actual = platformName().isNotBlank(),
            message = "platformName() must describe the running platform",
        )
    }
}
