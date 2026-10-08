# Selenium WebDriver + TestNG Framework (Java)

[![Selenium Tests](https://github.com/deepakdeakin/selenium-java-testng-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/deepakdeakin/selenium-java-testng-framework/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.33-43B02A)](https://www.selenium.dev/)
[![TestNG](https://img.shields.io/badge/TestNG-7.11-DE4B2E)](https://testng.org/)
[![Allure](https://img.shields.io/badge/Allure-reporting-FF6C37)](https://allurereport.org/)

**33 tests running in parallel in ~27 seconds, with Allure reporting, a Dockerised Selenium Grid and both GitHub Actions and Jenkins pipelines.**

---

## Why this exists alongside the Playwright framework

This suite covers the same application as [playwright-ts-ui-framework](https://github.com/deepakdeakin/playwright-ts-ui-framework), deliberately. The application is the constant; the stack is the variable. Building both against one target is what makes the comparison meaningful — and the Selenium-specific problems it surfaced (documented below) are ones the Playwright suite never hits, because Playwright solves them in the tool.

What is unique to this repository: thread-confined driver management, TestNG data providers and groups, an ITestListener capturing diagnostics, Selenium Grid via Docker, a Jenkinsfile, and the Maven build.

## What this demonstrates

| Capability | Where to look |
|---|---|
| Thread-safe parallel execution | [`DriverManager`](src/test/java/com/deepakkhatri/qa/driver/DriverManager.java), [`BaseTest`](src/test/java/com/deepakkhatri/qa/base/BaseTest.java) |
| Cross-browser driver construction | [`DriverFactory`](src/test/java/com/deepakkhatri/qa/driver/DriverFactory.java) |
| Page Object Model with a component object | [`pages/`](src/test/java/com/deepakkhatri/qa/pages/) |
| Explicit waits — no implicit wait, no `Thread.sleep` | [`BasePage`](src/test/java/com/deepakkhatri/qa/pages/BasePage.java) |
| JSON-backed TestNG data providers | [`DataProviders`](src/test/java/com/deepakkhatri/qa/data/DataProviders.java), [`invalid-logins.json`](src/test/resources/testdata/invalid-logins.json) |
| Builder pattern for test data | [`CheckoutDetails`](src/test/java/com/deepakkhatri/qa/data/CheckoutDetails.java) |
| Screenshot, page source and context on failure | [`TestListener`](src/test/java/com/deepakkhatri/qa/listeners/TestListener.java) |
| Layered configuration (`-D` → env → properties) | [`Configuration`](src/test/java/com/deepakkhatri/qa/config/Configuration.java) |
| Selenium Grid, three browser nodes | [`docker-compose.grid.yml`](docker-compose.grid.yml) |
| Jenkins pipeline with parameters | [`Jenkinsfile`](Jenkinsfile) |
| GitHub Actions pipeline + Allure publishing | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |

## Test coverage

| Class | Tests | Focus |
|---|---|---|
| `LoginTests` | 11 | Valid sign-in, locked-out account, six data-driven rejections, sign-out, direct-URL access control |
| `InventoryTests` | 10 | Item count, four sort orders, set-preservation across all four sorts |
| `CartTests` | 7 | Badge counting, add/remove, persistence across navigation, price consistency |
| `CheckoutTests` | 5 | End-to-end order, total arithmetic, three required-field validations, cancel behaviour, post-order state |

**33 tests, 3 parallel threads, ~27 s.** Verified repeatedly on Chrome and Firefox locally.

## Quick start

Requires JDK 17+ and Chrome or Firefox. Maven comes from the wrapper; drivers are resolved by Selenium Manager, so there is nothing to install and no `chromedriver` in the repository.

```bash
./mvnw test
```

```bash
./mvnw test -Dsuite=testng-smoke.xml      # smoke subset
./mvnw test -Dbrowser=firefox             # another browser
./mvnw test -Dheadless=false              # watch it run
./mvnw test -Dthreads=5                   # more parallelism
./mvnw allure:serve                       # open the Allure report
```

Against a Grid:

```bash
docker compose -f docker-compose.grid.yml up -d
./mvnw test -Pgrid -Dbrowser=firefox
docker compose -f docker-compose.grid.yml down
```

The Grid console is at `http://localhost:4444`; noVNC on ports 7900–7902 (password `secret`) lets you watch the browsers work.

## Three defects this framework hit, and what they teach

These were all found by building and running the suite. Each is fixed in the code with the reasoning recorded next to the fix.

### 1. `ThreadLocal` on the driver is not enough

The first parallel run produced three failures whose symptoms pointed squarely at the application: a locked-out user receiving a *bad credentials* message, and timeouts waiting for pages that had already loaded.

The cause was in the framework. TestNG instantiates a test class **once** and, under `parallel="methods"`, runs its methods concurrently against that single instance. Page objects held in instance fields and assigned in `@BeforeMethod` are therefore shared: whichever thread starts next overwrites them, and tests end up driving each other's browsers. A `ThreadLocal` driver does not help, because the shared field holds a page object that already captured the wrong driver.

**Fix:** page objects are exposed as [methods, not fields](src/test/java/com/deepakkhatri/qa/base/BaseTest.java), built on demand from the calling thread's driver. They are thin wrappers, so construction is effectively free.

This is the single most common way a Selenium suite becomes "randomly flaky" when parallelism is switched on.

### 2. `maximize()` silently overrides `--window-size` in headless Chrome

Measured, not assumed: with `--window-size=1920,1080` **and** `driver.manage().window().maximize()`, the viewport came out at **800×457**. Layout-sensitive elements then sit at different coordinates locally and in CI, and screenshots come out at an unexpected size.

**Fix:** an explicit `window().setSize(...)` and no `maximize()`.

### 3. A native click that reports success and does nothing

The sign-out test failed for a long time, and the investigation is the most interesting thing in this repository.

The slide-out menu is never removed from the DOM — closed, it is only translated 300 px off-screen. Its links still report `display: block` and `visibility: visible`, so `ExpectedConditions.elementToBeClickable` calls them clickable and Selenium clicks a point outside the viewport. `aria-hidden` is not a usable signal either: it flips to `false` while the CSS transform is still animating.

Adding a real hit test — `document.elementFromPoint` at the element's centre, which is [the actionability check Selenium does not provide](src/test/java/com/deepakkhatri/qa/pages/components/HeaderComponent.java) — fixed the timing but not the click. With the menu fully open and the link settled at `x=24, y=136, 252×41`:

- `elementFromPoint` at its centre returns the link itself; `pointer-events` is `auto`
- `WebElement.click()` returns without throwing, yet **no mousedown, mouseup or click event reaches the element**, and the app does not navigate
- `Actions.moveToElement().click()` behaves identically
- Keyboard `ENTER` on the focused link does nothing
- `window.scrollY` is `0`, so a stale scroll offset is not the cause
- A scripted click navigates correctly, every time

The distinguishing feature is the container: the menu wrapper is `position: fixed` **and** CSS-transformed, putting it on its own compositing layer. WebDriver resolves a click coordinate that never reaches that layer, while reporting success.

**Fix:** a scripted click, confined to the menu — the only one in the framework; every cart, catalogue and checkout control is driven with real clicks. It is deliberately paired with the hit test, so the workaround cannot mask a genuinely missing or obscured control.

This one has a sting in the tail, covered in the open finding below: the same hit-test probe, applied to *every* click, made things worse rather than better.

The Playwright suite passes this same scenario without special handling, because its actionability model performs this check in the tool. That contrast is the honest summary of the two stacks.

## An open finding: Chrome on GitHub's runners

This is unresolved, and it is recorded rather than hidden.

Chrome on GitHub's Ubuntu runners intermittently drops clicks against this application. WebDriver accepts the click and returns without error; the page never receives it. The failures are always *"the cart is empty"* or *"the badge did not change"* — never a wrong value, which is what separates a lost click from an application defect.

What is established:

- The same commit passes on Chrome **locally**, repeatedly, headless and headed — 33/33 in ~27 s
- The same commit passes on **Firefox on the same runner**, consistently
- The demo site is healthy throughout, and the companion Playwright suite passes against it in 13 s
- Login clicks succeed on the failing runs; cart and catalogue clicks are the ones lost

Four fixes were tried and measured, none sufficient:

| Attempt | Result |
|---|---|
| Reduce parallel threads from 3 to 2 (a 2-core runner driving 3 browsers) | Passed once, failed on the next run |
| Hit-test and layout-stability probe before every click | Made it **worse**, and broke the suite locally — see below |
| Verified-click retry, three attempts with a mandatory post-condition | Clear failure messages, same outcome |
| Headed under Xvfb, removing the headless compositor | No change |
| A version-matched Chrome and chromedriver (the runner paired 151.0.7922.108 with driver .77) | Sessions failed to start; reverted |

The probe attempt produced the most useful by-product. Running JavaScript immediately before a native `WebElement.click()` leaves Chrome's hit-test data stale, so the click is dispatched to nothing — the *same* silent-failure signature. That is why the scripted probe is now confined to the slide-out menu, which clicks via script anyway, and why `BasePage.click` is a plain native click with no scripted pre-check.

**The decision:** Firefox gates the pipeline. Chrome runs in [`chrome.yml`](.github/workflows/chrome.yml) — scheduled, on demand, reported, not gating. The verified-click retry stays in the framework because it converts a silent wrong result into a failure message that names the control and the expectation.

Marking a suite green by weakening its assertions would be the wrong fix. Leaving a red badge on work that passes everywhere else would misrepresent it just as badly. Splitting the pipeline states the situation accurately: the framework is sound, and one browser on one CI platform has a problem that is documented and still open.

## Design decisions

**Explicit waits only.** No implicit wait is configured anywhere, and there is no `Thread.sleep` in the framework. Mixing implicit and explicit waits produces timeouts that are neither value and are notoriously hard to diagnose.

**Page objects never assert.** They expose intent and return state; assertions live in the tests, so a failure names the business rule that broke rather than a helper several frames down the stack.

**Money is compared in cents.** Floating-point currency arithmetic makes a correct checkout total fail an equality assertion.

**Prices are read from the page, never hardcoded.** The catalogue data selects *which* product to use; the expected value comes from the application.

**A fresh browser per test method.** `@BeforeMethod` rather than `@BeforeClass` — it costs launch time and buys isolation, which is the right trade at this size. `alwaysRun = true` on teardown so a failing setup cannot leak a browser process per test.

**An unknown browser name fails loudly.** Silently defaulting to Chrome on a typo would report a green cross-browser run that never touched the other browser.

**Blank configuration values are treated as unset.** An unset CI secret or an empty `-D` flag arrives as an empty string, not null — carried over from a defect that broke the companion project's first pipeline run.

## CI

**GitHub Actions** — compile, then the suite on Firefox, then Allure published to Pages. Runs on push, PR, nightly, and on demand with a suite selector. Chrome runs in a separate non-gating workflow, for the reason below.

**Jenkins** — the same pipeline as a parameterised `Jenkinsfile`: suite, browser, thread count and an optional Grid, with JUnit and Allure publishing in `post { always }` and Grid teardown guaranteed.

## Notes

- The application under test is [saucedemo.com](https://www.saucedemo.com), a public demo site published for automation practice. Its credentials are printed on its own sign-in page.
- This repository contains no code, data, or credentials from any employer. Every line was written for this portfolio.

## Companion projects

- [playwright-ts-ui-framework](https://github.com/deepakdeakin/playwright-ts-ui-framework) — the same application in Playwright + TypeScript
- [rest-api-automation-suite](https://github.com/deepakdeakin/rest-api-automation-suite) — API testing with contract schemas

## Licence

MIT
