const katex = require('katex');
try {
    katex.renderToString("↑n", { throwOnError: false });
    console.log("No error thrown for ↑n");
} catch (e) {
    console.log("ERROR THROWN for ↑n:", e.message);
}
