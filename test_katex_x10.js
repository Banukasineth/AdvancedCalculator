const katex = require('katex');
try {
    katex.renderToString("x10ⁿ", { throwOnError: false });
    console.log("No error thrown for x10ⁿ");
} catch (e) {
    console.log("ERROR THROWN for x10ⁿ:", e.message);
}
