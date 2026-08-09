const katex = require('katex');
try {
    katex.renderToString("\\bmod", { throwOnError: false });
    console.log("No error thrown for \\bmod");
} catch (e) {
    console.log("ERROR THROWN for \\bmod:", e.message);
}
try {
    katex.renderToString("400 \\bmod 5", { throwOnError: false });
    console.log("No error thrown for 400 \\bmod 5");
} catch (e) {
    console.log("ERROR THROWN for 400 \\bmod 5:", e.message);
}
