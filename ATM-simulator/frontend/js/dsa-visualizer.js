/* ============================================================
   dsa-visualizer.js  -  Renders REAL backend DSA operations.
   No front-end simulation; every result comes from the Java API.
   ============================================================ */

const DSA = (() => {
  const DENOMS = [500, 200, 100, 50, 20, 10];

  function inr(n) {
    const v = Number(n) || 0;
    return "\u20B9" + v.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  }

  function showTab(tab) {
    document.querySelectorAll(".dsa-tab").forEach(t => t.classList.toggle("active", t.dataset.dsaTab === tab));
    ["stack", "queue", "search", "greedy"].forEach(p => {
      document.getElementById("dsa-" + p + "-panel").classList.toggle("active", p === tab);
    });
  }

  function activeTab() {
    const t = document.querySelector(".dsa-tab.active");
    return t ? t.dataset.dsaTab : "stack";
  }

  function refreshActive() {
    const t = activeTab();
    if (t === "stack") refreshStack();
    else if (t === "queue") refreshQueue();
  }

  async function refreshStack() {
    const out = document.getElementById("dsa-stack-content");
    out.innerHTML = '<p class="empty">Loading...</p>';
    try {
      const data = await API.dsaStack();
      if (!data.success) { out.innerHTML = '<p class="empty">' + (data.message || "Login required.") + "</p>"; return; }
      document.getElementById("dsa-stack-size").textContent = data.size;
      const items = data.items || [];
      if (!items.length) { out.innerHTML = '<p class="empty">Stack is empty (no transactions yet).</p>'; return; }
      out.innerHTML = '<div class="stack-visual">' + items.map((t, i) =>
        '<div class="stack-item ' + (i === 0 ? "top" : "") + '">' +
          '<span><span class="si-tag">' + (i === 0 ? "TOP" : "#" + (i + 1)) + "</span> " +
          '<strong>' + t.type + "</strong> " + inr(t.amount) + (t.reversed ? ' <span class="h-pill">REV</span>' : "") + "</span>" +
          '<span class="si-meta">#' + t.id + " &middot; " + t.timestamp + "</span>" +
        "</div>").join("") + "</div>";
    } catch (e) { out.innerHTML = '<p class="empty">' + e.message + "</p>"; }
  }

  async function refreshQueue() {
    const out = document.getElementById("dsa-queue-content");
    out.innerHTML = '<p class="empty">Loading...</p>';
    try {
      const data = await API.dsaQueue();
      document.getElementById("dsa-queue-size").textContent = data.size;
      const items = data.items || [];
      if (!items.length) { out.innerHTML = '<p class="empty">Queue is empty. Add requests from the Pending screen.</p>'; return; }
      out.innerHTML = '<div class="queue-visual">' + items.map((t, i) =>
        '<div class="q-item ' + (i === 0 ? "front" : "") + '">' +
          '<div class="qi-pos">' + (i === 0 ? "FRONT" : "#" + (i + 1)) + "</div>" +
          "<div><strong>" + t.type.replace("PENDING_", "") + "</strong></div>" +
          "<div>" + inr(t.amount) + "</div>" +
        "</div>" +
        (i < items.length - 1 ? '<div class="q-arrow">&rarr;</div>' : "")).join("") + "</div>";
    } catch (e) { out.innerHTML = '<p class="empty">' + e.message + "</p>"; }
  }

  async function runSearch() {
    const input = document.getElementById("dsa-search-input");
    const out = document.getElementById("dsa-search-content");
    const acc = (input.value || "").trim();
    if (!acc) { out.innerHTML = '<p class="empty">Enter an account number.</p>'; return; }
    out.innerHTML = '<p class="empty">Searching...</p>';
    try {
      const data = await API.dsaSearch(acc);
      const scanned = data.scanned || 0;
      // Linear-search scan visual: the indices actually checked (0 .. scanned-1).
      let cells = '<div class="search-cells">';
      for (let i = 0; i < scanned; i++) {
        cells += '<div class="search-cell checked ' + (data.found && data.index === i ? "found" : "") + '">' +
          '<div class="sc-i">index ' + i + "</div>" + (data.found && data.index === i ? "MATCH" : "checked") + "</div>";
      }
      cells += "</div>";

      let result = "";
      if (data.found) {
        result = '<div class="search-result">' +
          '<div class="kv"><span>Status</span><span style="color:var(--green)">Found at index ' + data.index + "</span></div>" +
          '<div class="kv"><span>Account No.</span><span>' + data.accountNumber + "</span></div>" +
          '<div class="kv"><span>Holder</span><span>' + data.name + "</span></div>" +
          '<div class="kv"><span>Balance</span><span>' + inr(data.balance) + "</span></div>" +
          '<div class="kv"><span>Elements scanned</span><span>' + scanned + "</span></div></div>";
      } else {
        result = '<div class="search-result"><div class="kv"><span>Status</span><span style="color:var(--red)">Not found</span></div>' +
          '<div class="kv"><span>Searched</span><span>' + acc + "</span></div>" +
          '<div class="kv"><span>Elements scanned</span><span>' + scanned + "</span></div></div>";
      }
      out.innerHTML = cells + result;
    } catch (e) { out.innerHTML = '<p class="empty">' + e.message + "</p>"; }
  }

  async function runGreedy() {
    const input = document.getElementById("dsa-greedy-input");
    const out = document.getElementById("dsa-greedy-content");
    const amt = (input.value || "").trim();
    if (!amt) { out.innerHTML = '<p class="empty">Enter an amount.</p>'; return; }
    out.innerHTML = '<p class="empty">Computing...</p>';
    try {
      const data = await API.dsaGreedy(amt);
      if (!data.success) { out.innerHTML = '<p class="empty">' + (data.message || "Cannot dispense.") + "</p>"; return; }
      const bk = data.breakdown || {};
      const max = Math.max(1, ...DENOMS.map(d => (bk[String(d)] || 0) * d));
      let rows = '<div class="greedy-rows">';
      DENOMS.forEach(d => {
        const c = bk[String(d)] || 0;
        if (c > 0) {
          const pct = Math.round(((c * d) / max) * 100);
          rows += '<div class="greedy-row"><span class="gr-den">' + inr(d) + "</span>" +
            '<div class="gr-line"><div class="gr-fill" style="width:' + pct + '%"></div></div>' +
            "<span>&times; " + c + " = " + inr(c * d) + "</span></div>";
        }
      });
      rows += "</div>";
      out.innerHTML = rows +
        '<div class="greedy-total"><div class="kv"><span>Total dispensed</span><span>' + inr(data.amount) + "</span></div>" +
        '<div class="kv"><span>Notes used</span><span>' + data.noteCount + "</span></div>" +
        '<div class="kv"><span>ATM cash available</span><span>' + inr(data.availableCash) + "</span></div></div>";
    } catch (e) { out.innerHTML = '<p class="empty">' + e.message + "</p>"; }
  }

  return { showTab, refreshActive, refreshStack, refreshQueue, runSearch, runGreedy };
})();
