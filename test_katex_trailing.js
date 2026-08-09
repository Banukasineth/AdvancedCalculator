const katex = require('katex');
try {
    katex.renderToString("400\\", { throwOnError: false });
    console.log("No error thrown for 400\\");
} catch (e) {
    console.log("ERROR THROWN for 400\\:", e.message);
}
