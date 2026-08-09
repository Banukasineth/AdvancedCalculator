const katex = require('katex');

try {
    let html = katex.renderToString("\\overline{}", { throwOnError: false });
    console.log("Empty overline:", html);
} catch (e) {
    console.log("ERROR:", e.message);
}
