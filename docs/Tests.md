# Test groups and nettests

Overview of test groups (descriptors) bundled with each app flavor, their manual and background run defaults, how tests are disabled, and past default changes.

## 1. Descriptors

The app groups nettests into descriptors, also called test groups or OONI Run v2 descriptors. A descriptor has metadata (title, description, icon, color, expiration date) and a list of nettests to run.

Each flavor bundles a `descriptors.json` asset so the default groups are available offline and on first launch. Descriptors installed later come from the OONI Run API or from deep links, and the app updates them through the same API.

## 2. Test groups by flavor

### 2.1. OONI Probe (`ooniMain`)

Source: `composeApp/src/ooniMain/composeResources/files/assets/descriptors.json`

| Group | ID | Nettests | Manual run | Background run | Notes |
| :--- | :--- | :--- | :---: | :---: | :--- |
| Websites | `00104` | `web_connectivity` | On | On | URLs come from check-in, filtered by the user's website categories |
| Instant Messaging | `00105` | `whatsapp`, `telegram`, `facebook_messenger`, `signal` | On | On | |
| Circumvention | `00106` | `psiphon`, `tor` | On | On | |
| Performance | `00107` | `ndt`, `dash`, `http_header_field_manipulation`, `http_invalid_request_line` | On | Mixed | `ndt` and `dash` are off for background runs because they use a lot of data |
| Experimental | `00108` | `stunreachability`, `openvpn`, `vanilla_tor` | On | Off | |
| VPNs | `local-vpns` | `web_connectivity`, `tlsping`, `quicping` | On | Off | Local group, see section 2.3 |
| Website Blocking Methods | `local-blocking-methods` | `dnscheck`, `echcheck`, `sniblocking`, `httphostheader` | On | Off | Local group, see section 2.3 |
| Social Media | `local-social-media` | `web_connectivity` | On | Off | Local group, see section 2.3 |
| News Media | `local-news-media` | `web_connectivity` | On | Off | Local group, see section 2.3 |

### 2.2. News Media Scan (`dwMain`)

Source: `composeApp/src/dwMain/composeResources/files/assets/descriptors.json`

All three DW groups run `web_connectivity` against a fixed URL list bundled in the descriptor.

| Group | ID | URLs | Manual run | Background run | Contents |
| :--- | :--- | :---: | :---: | :---: | :--- |
| Trusted International Media | `10004` | 16 | On | On | International outlets such as DW, BBC, RFI and VOA |
| Selected (inter)national media | `10005` | 90 | On | On | A curated list of national and international news sites |
| Global media | `10006` | 222 | On | On | A broad list of media sites worldwide |

### 2.3. Local test groups

Local test groups ship inside `descriptors.json` like the others, but they are not published as OONI Run links. Their ID starts with `local-` (`Descriptor.LOCAL_ID_PREFIX`), which can never match an OONI Run link ID, and the app treats them differently from other bundled groups:

* `FetchDescriptorsUpdates` never asks the OONI Run API for updates, so they only change with app updates.
* They are listed with the OONI tests, but keep their own settings prefix, so their nettests don't share settings with the OONI default groups.
* They have no share link, update switch or uninstall button. They are bootstrapped again on every app start, so uninstalling would not stick.
* Measurements are not annotated with an `ooni_run_link_id`.
* Their manual and background run defaults come from the `is_manual_run_enabled_default` and `is_background_run_enabled_default` fields in `descriptors.json`, see section 3.

| Group | ID | Nettests and inputs |
| :--- | :--- | :--- |
| VPNs | `local-vpns` | `web_connectivity` on 26 VPN and circumvention tool websites, `tlsping` on 5 VPN sign-in and tunnel servers, `quicping` on iCloud Private Relay with Google and Cloudflare as a baseline |
| Website Blocking Methods | `local-blocking-methods` | `dnscheck` and `echcheck` with the engine default inputs, `sniblocking` and `httphostheader` on 10 domains |
| Social Media | `local-social-media` | `web_connectivity` on 26 social media, video and messaging websites |
| News Media | `local-news-media` | `web_connectivity` on 26 international news and press freedom websites |

Every nettest other than `web_connectivity` maps to `TestType.Experimental`, so the app does not flag anomalies for it. Its details are on OONI Explorer.

`BundledDescriptorsTest` checks that local groups only use nettests that the bundled engine can run without a check-in feature flag, with inputs in the format each nettest expects.

#### VPN protocol tests that are not included

As of oonimkall `3.30.0`, the engine has no WireGuard, IPsec or other VPN protocol experiment. The VPN related experiments it has can't be used by local groups:

* `openvpn` is disabled by the check-in API (`openvpn_enabled: false`). It is enabled by default in the engine, so `OONI_FORCE_ENABLE_EXPERIMENT` can't turn it back on.
* `riseupvpn` is disabled by default in the engine, and the check-in API never enables it.
* `portfiltering` connects to `127.0.0.1` until OONI deploys its test helper.

Adding more VPN protocol tests needs a new experiment in [probe-cli](https://github.com/ooni/probe-cli) and a new engine release.

## 3. Nettest defaults

The manual and background defaults come from `TestType` (`composeApp/src/commonMain/kotlin/org/ooni/engine/models/TestType.kt`). `PreferenceRepository.defaultPreferenceValue` reads `TestType.isManualRunEnabled` or `TestType.isBackgroundRunEnabled` when the user has not set a value yet.

The `is_manual_run_enabled_default` and `is_background_run_enabled_default` fields in `descriptors.json` are parsed into `NetTest`, but they only drive the defaults of local test groups (section 2.3). In the other bundled descriptors every nettest has both fields set to `false`. Local groups are on for manual runs and off for background runs.

| Group | Nettest | Manual run | Background run | Preference key |
| :--- | :--- | :---: | :---: | :--- |
| Websites | `web_connectivity` | On | On | `web_connectivity` |
| Instant Messaging | `whatsapp` | On | On | `test_whatsapp` |
| Instant Messaging | `telegram` | On | On | `test_telegram` |
| Instant Messaging | `facebook_messenger` | On | On | `test_facebook_messenger` |
| Instant Messaging | `signal` | On | On | `test_signal` |
| Circumvention | `psiphon` | On | On | `test_psiphon` |
| Circumvention | `tor` | On | On | `test_tor` |
| Performance | `ndt` | On | Off | `run_ndt` |
| Performance | `dash` | On | Off | `run_dash` |
| Performance | `http_header_field_manipulation` | On | On | `run_http_header_field_manipulation` |
| Performance | `http_invalid_request_line` | On | On | `run_http_invalid_request_line` |
| Experimental | `stunreachability` | On | Off | `stunreachability` |
| Experimental | `openvpn` | On | Off | `openvpn` |
| Experimental | `vanilla_tor` | On | Off | `vanilla_tor` |

Every `TestType` is on for manual runs unless it overrides `isManualRunEnabled`. Background runs are off for `Dash`, `Ndt` and anything mapped to `TestType.Experimental`. `Experimental` takes both flags as constructor arguments, so a specific experimental test can opt into background runs.

## 4. When a nettest runs

A manual run starts when the user taps run in the app. A background run starts from the scheduled automated testing task.

A nettest runs only if all of these hold:

1. The descriptor still lists it after the check-in filter in section 5.1.
2. The user has it switched on for that run type in settings. Without a saved choice, the default from section 3 applies.
3. The run's other conditions are met, for example the Wi-Fi only setting for automated runs.

## 5. Ways a test gets turned off

### 5.1. Check-in feature flags

`CheckIn` (`composeApp/src/commonMain/kotlin/org/ooni/probe/domain/CheckIn.kt`) calls `/api/v1/check-in`. The response can return feature flags in `conf.features`, for example `"web_connectivity_enabled": false`. `CheckInResponse.kt` maps every key ending in `_enabled` with a `false` value to a `TestType`, and `CheckIn` stores the preference keys under `SettingsKey.DISABLED_TESTS` (`disabled_tests`).

`GetTestDescriptors` watches that preference and removes matching tests from `netTests` and `longRunningTests` in each installed descriptor. The UI and scheduler both read descriptors through it, so tests disabled by the backend disappear from both.

### 5.2. Website categories

If the user unchecks every `WebConnectivityCategory` in settings, `GetTestDescriptors.isWebsitesDescriptorEnabled` returns `false` and the Websites descriptor (`00104`) is marked disabled. This applies only to the Websites group.

### 5.3. User settings

Users can switch individual nettests, or a whole group, on or off separately for manual and background runs. These choices are stored per descriptor and nettest in `PreferenceRepository`.

