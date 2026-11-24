/* ============================================================
   api.js  -  Fetch API client for the Java ATM backend.
   Every call hits the REAL backend endpoint (no mock data).
   ============================================================ */

const API = (() => {
  // Same-origin: the backend serves the frontend, so relative URLs work.
  async function request(path, params = {}, method = "GET") {
    const url = new URL(path, location.origin);
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));

    let res;
    try {
      res = await fetch(url.toString(), { method });
    } catch (e) {
      throw new Error("Cannot reach the ATM server. Is it running?");
    }

    let data = {};
    try { data = await res.json(); } catch (e) { data = {}; }

    if (!res.ok) {
      throw new Error(data.message || ("HTTP " + res.status));
    }
    return data;
  }

  return {
    session:        () => request("/api/session"),
    login:   (a, p) => request("/api/login",   { accountNumber: a, pin: p }, "POST"),
    logout:         () => request("/api/logout", {}, "POST"),
    balance:        () => request("/api/balance"),
    withdraw:(amt)  => request("/api/withdraw",  { amount: amt }, "POST"),
    deposit: (amt)  => request("/api/deposit",   { amount: amt }, "POST"),
    history:        () => request("/api/history"),
    reverse:        () => request("/api/reverse", {}, "POST"),
    pendingAdd:(t,a)=> request("/api/pending/add", { type: t, amount: a }, "POST"),
    pendingProcess:  () => request("/api/pending/process", {}, "POST"),
    pendingList:      () => request("/api/pending/list"),
    dsaStack:         () => request("/api/dsa/stack"),
    dsaQueue:         () => request("/api/dsa/queue"),
    dsaSearch:(a)   => request("/api/dsa/search", { accountNumber: a }, "POST"),
    dsaGreedy:(a)   => request("/api/dsa/greedy", { amount: a }, "POST"),
  };
})();
