package app.moviematch

/**
 * Returns a human readable description of the platform the app is running on.
 *
 * Kept intentionally tiny: it is the smoke test that proves the `expect`/`actual`
 * mechanism (and therefore the Kotlin Multiplatform setup) is wired correctly.
 */
expect fun platformName(): String
