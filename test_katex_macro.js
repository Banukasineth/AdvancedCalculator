const katex = require('katex');

try {
    let html = katex.renderToString("\\SQRT(5+2)", { 
        throwOnError: false,
        macros: {
            "\\SQRT(#1)": "\\sqrt{#1}"
        }
    });
    console.log("Macro 1:", html);
} catch (e) {
    console.log("Macro 1 ERROR:", e.message);
}
