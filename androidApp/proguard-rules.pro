# MovieMatch — R8 / ProGuard rules.
#
# Rules are kept in this file even while `isMinifyEnabled` is false, so turning minification on for
# a release build does not silently break authentication.

# ---------------------------------------------------------------------------------------------
# Google Sign-In (Credential Manager)
# ---------------------------------------------------------------------------------------------
# `CredentialManager` loads the Play Services backed provider reflectively (it is what makes the
# flow work on Android 13 and below, via `credentials-play-services-auth`). Without this rule R8
# strips the implementation and `getCredential()` fails at runtime with a provider configuration
# error. Taken verbatim from the official Android sign-in implementation guide.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
