// Checks a newman JSON report from run-api-tests.sh.
//
// The RealWorld collection covers the whole API, but this app only implements part of it (the rest
// answers 501 Not Implemented). So instead of requiring every request to pass, this fails only if a
// request for an endpoint that IS implemented fails. When you implement another endpoint, add its
// request name(s) to IMPLEMENTED so CI starts protecting it.
//
// Usage: node spec-api/check-results.js <newman-report.json>
// Writes a Markdown summary to $GITHUB_STEP_SUMMARY when set (GitHub Actions), otherwise to stdout.

const fs = require("fs");

const IMPLEMENTED = [
    "Register",
    "Login",
    "Login and Remember Token",
    "Current User",
    "Update User",
    "Create Article",
    "Single Article by slug",
    "Register Celeb",
    "All Tags",
];

const reportPath = process.argv[2];
if (!reportPath || !fs.existsSync(reportPath)) {
    console.error(`No newman report at "${reportPath}". Did the app start and the spec run?`);
    process.exit(1);
}
const executions = JSON.parse(fs.readFileSync(reportPath, "utf8")).run.executions;

const rows = executions.map((e) => {
    const assertions = e.assertions || [];
    const failed = assertions.filter((a) => a.error);
    return {
        name: e.item.name,
        implemented: IMPLEMENTED.includes(e.item.name),
        code: e.response ? e.response.code : "no response",
        passed: assertions.length - failed.length,
        total: assertions.length,
        // A request that never got a response (e.g. the app crashed) counts as failed.
        ok: !e.requestError && failed.length === 0,
        firstError: e.requestError ? e.requestError.message : failed.length ? failed[0].assertion : "",
    };
});

const problems = [];
// A listed name missing from the results (e.g. renamed in the collection) must not silently pass.
for (const name of IMPLEMENTED) {
    if (!rows.some((r) => r.name === name)) problems.push(`"${name}" was not found in the spec run`);
}
for (const r of rows.filter((r) => r.implemented && !r.ok)) {
    problems.push(`"${r.name}" failed (HTTP ${r.code}): ${r.firstError}`);
}

const implemented = rows.filter((r) => r.implemented);
const notImplemented = rows.filter((r) => !r.implemented);
const lines = [
    "## RealWorld API spec (Postman collection)",
    "",
    problems.length
        ? `❌ **${problems.length} problem(s) with implemented endpoints:**\n\n${problems.map((p) => `- ${p}`).join("\n")}`
        : `✅ **All ${implemented.length} requests for implemented endpoints pass.**`,
    "",
    `${notImplemented.length} requests target endpoints that are not implemented yet (they answer 501); ` +
        "they are reported below but do not fail the build.",
    "",
    "| Request | Implemented | HTTP | Assertions passed |",
    "|---|---|---|---|",
    ...rows.map((r) => `| ${r.ok ? "✅" : "❌"} ${r.name} | ${r.implemented ? "yes" : "no"} | ${r.code} | ${r.passed}/${r.total} |`),
    "",
];
const summary = lines.join("\n");

if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary);
console.log(summary);
process.exit(problems.length ? 1 : 0);
