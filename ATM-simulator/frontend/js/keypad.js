/* ============================================================
   keypad.js  -  On-screen numeric keypad + PIN masking.
   A "target" describes where digits go:
     { value, masked, maxLen, displayEl, onUpdate?, onFull? }
   ============================================================ */

const Keypad = (() => {
  let target = null;

  function el() { return document.getElementById("keypad"); }

  function setTarget(t) {
    target = t;
    if (target && target.displayEl) target.displayEl.classList.toggle("masked", !!target.masked);
    render();
  }

  function clearTarget() { target = null; }

  function render() {
    if (!target || !target.displayEl) return;
    const v = target.value || "";
    target.displayEl.textContent = target.masked ? "\u2022".repeat(v.length) : v;
    if (target.onUpdate) target.onUpdate(v);
  }

  function press(key) {
    if (!target) return;
    if (key === "clear") { target.value = ""; render(); return; }
    if (key === "del")   { target.value = target.value.slice(0, -1); render(); return; }
    if (/^\d$/.test(key)) {
      if (target.maxLen && target.value.length >= target.maxLen) return;
      target.value += key;
      render();
      if (target.maxLen && target.value.length === target.maxLen && target.onFull) target.onFull();
    }
  }

  function init() {
    el().addEventListener("click", (e) => {
      const b = e.target.closest("[data-key]");
      if (!b) return;
      press(b.dataset.key);
      b.classList.add("pressed");
      setTimeout(() => b.classList.remove("pressed"), 110);
    });
  }

  function show(show) { el().hidden = !show; }

  return { init, setTarget, clearTarget, press, render, show };
})();
