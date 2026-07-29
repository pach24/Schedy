# Shared links (App Links)

Sharing a session produces a message with a link:

```
https://getschedy.web.app/join?code=JT5QEJ
```

Tapping it opens **the app**, on "join", with the code filled in. If the phone doesn't have
Schedy, it opens a page showing the code in large type, ready to be copied by hand.

## Why it isn't a `schedy://`

It was, and it didn't work. WhatsApp, Telegram and SMS only turn `http(s)` into a tappable
link: a custom scheme arrives as plain text. And since Android 12 pasting it by hand doesn't
open the app either.

The standard answer is an **App Link**: an https URL on a domain you control, which Android
verifies against the server and, if it checks out, hands to the app without going through
the browser.

`schedy://` is still alive, but for internal use only: pushes, the local reminder and the
button on the fallback page.

## The three moving parts

**1. The manifest** declares the domain with `android:autoVerify="true"`, for the `/join`
and `/event` paths, on `getschedy.web.app` and `getschedy.firebaseapp.com`.

**2. The server** publishes `https://getschedy.web.app/.well-known/assetlinks.json`, listing
the SHA-256 fingerprints of the keys the app is signed with:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.schednd",
    "sha256_cert_fingerprints": [
      "03:13:C2:…",   // debug
      "57:E5:AA:…"    // release (the GitHub APK)
    ]
  }
}]
```

**3. The routes** declare the deep link with `navDeepLink`, and Navigation pulls `code` out
of the query.

## The one rule behind all of it

> Android compares the **signature of the installed APK** against the fingerprints in
> `assetlinks.json`. If its own isn't there, the link is not verified and the browser opens.

Everything else follows:

- Every distribution channel needs its fingerprint on the list. Debug (your machine),
  release (the GitHub APK) and **the Play app signing key** — which is not yours: with Play
  App Signing, Google re-signs the app with its own. That SHA-256 comes from Play Console →
  *Test and release → App integrity*, and using the upload key instead is the classic
  mistake.
- The file lives on the server, **not inside the app**: you can add the Play fingerprint
  after publishing and phones re-verify on their own, no new release needed.
- Regenerate the keystore and the fingerprint changes, so links stop opening the app until
  you update the file.

## Checking that it works

```bash
# is the file served, with the right content type?
curl -i https://getschedy.web.app/.well-known/assetlinks.json

# same thing, as Google's validator sees it
curl "https://digitalassetlinks.googleapis.com/v1/statements:list?\
source.web.site=https://getschedy.web.app&\
relation=delegate_permission/common.handle_all_urls"

# the real test: what the phone thinks
adb shell pm verify-app-links --re-verify com.schednd
adb shell pm get-app-links com.schednd
#   getschedy.web.app: verified   ← this is what you want to see
```

Exercising the whole path without going through WhatsApp:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "https://getschedy.web.app/join?code=JT5QEJ" com.schednd
```

> Careful: passing the package makes the intent explicit, which **skips verification**. It
> proves the route and the argument work, not that verification passed — only
> `pm get-app-links` tells you that.

## The fallback page

`public/index.html`. Only people without the app ever see it (desktop, iOS, or an Android
that hasn't verified yet). It reads `?code=`, shows it large, offers a button that tries
`schedy://join?code=…`, and translates itself based on the browser language. Plain HTML with
no dependencies; it ships with `firebase deploy --only hosting`.

## History, in case an old link turns up

The first site was `schednd.web.app` (the project id). When we wanted a domain matching the
app name, `schedy` and `schedyapp` turned out to be reserved by other projects, so the site
`getschedy` was created and **hosting on the old one was disabled**. Links shared before
that change open nothing.
