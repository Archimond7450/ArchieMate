/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "../js/target/scala-3.3.4/archiemate-frontend-fastopt/*.js", // Scala.js output
        "../js/target/scala-3.3.4/archiemate-frontend-opt/*.js", // Scala.js output
    ],
    theme: {
        extend: {},
    },
    plugins: [],
}
