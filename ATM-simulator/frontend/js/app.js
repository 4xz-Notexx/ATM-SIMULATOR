/* ============================================================
   app.js  -  Controller: navigation, state, and wiring every
   button to the real Java backend via api.js.
   ============================================================ */

let state = { name: null, accountNumber: null, balance: null };

// keypad targets (created in init, reset on screen entry)
let loginAccountTarget, loginPinTarget, withdrawTarget, depositTarget, pendingTarget;

function inr(n) {
  const v = Number(n) || 0;
  return "\u20B9" + v.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function init() {
  Keypad.init();

  loginAccountTarget = makeTarget("login-account-display", false, 4, () => selectLoginField("pin"));
  loginPinTarget     = makeTarget("login-pin-display", true, 4);
  withdrawTarget     = makeTarget("withdraw-display", false, 7);
  depositTarget      = makeTarget("deposit-display", false, 7);
  pendingTarget      = makeTarget("pending-display", false, 7);

  document.addEventListener("click", onClick);
  showScreen("screen-welcome");
  checkSession();
}

function makeTarget(displayId, masked, maxLen, onFull) {
  return { value: "", masked, maxLen, displayEl: document.getElementById(displayId), onFull };
}

// ---------------- navigation ----------------

function showScreen(id) {
  document.querySelectorAll(".screen").forEach(s => s.classList.remove("active"));
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.add("active");
  el.style.animation = "none";
  void el.offsetWidth;            // reflow to restart the screen-in animation
  el.style.animation = "";
  const screenArea = document.getElementById("screen-area");
  if (screenArea) screenArea.scrollTop = 0;
  setupKeypadForScreen(el);
  if (id === "screen-menu") updateMenu();
}

function setupKeypadForScreen(el) {
  const needs = el.dataset.needsKeypad === "true";
  Keypad.show(needs);
  if (!needs) { Keypad.clearTarget(); return; }
  switch (el.id) {
    case "screen-login":    setupLogin(); break;
    case "screen-withdraw":  withdrawTarget.value = ""; Keypad.setTarget(withdrawTarget); break;
    case "screen-deposit":   depositTarget.value = "";  Keypad.setTarget(depositTarget); break;
    case "screen-pending":   pendingTarget.value = "";  Keypad.setTarget(pendingTarget); break;
  }
}

// ---------------- click routing ----------------

function onClick(e) {
  const tab = e.target.closest("[data-dsa-tab]");
  if (tab) { DSA.showTab(tab.dataset.dsaTab); return; }

  const sel = e.target.closest("[data-select]");
  if (sel) { handleSelect(sel.dataset.select); return; }

  const amt = e.target.closest("[data-amount]");
  if (amt) { doWithdraw(Number(amt.dataset.amount)); return; }

  const act = e.target.closest("[data-action]");
  if (act) { handleAction(act.dataset.action); }
}

function handleAction(a) {
  switch (a) {
    case "start":                 showScreen("screen-login"); break;
    case "login-enter":            doLogin(); break;
    case "login-cancel":           showScreen("screen-welcome"); break;
    case "go-balance":             openBalance(); break;
    case "go-withdraw":            showScreen("screen-withdraw"); break;
    case "go-deposit":             showScreen("screen-deposit"); break;
    case "go-history":             openHistory(); break;
    case "go-services":           showScreen("screen-services"); break;
    case "go-pending":             openPending(); break;
    case "go-dsa":                 openDsa(); break;
    case "logout":                 doLogout(); break;
    case "menu":                   showScreen("screen-menu"); break;
    case "withdraw":               doWithdraw(Number(withdrawTarget.value)); break;
    case "deposit":                doDeposit(Number(depositTarget.value)); break;
    case "reverse":                doReverse(); break;
    case "print":                  printReceipt(); break;
    case "print-receipt-balance":  showReceipt({ type: "BALANCE", balance: state.balance }); break;
    case "pending-add-withdraw":   doPendingAdd("PENDING_WITHDRAW"); break;
    case "pending-add-deposit":    doPendingAdd("PENDING_DEPOSIT"); break;
    case "pending-process":        doPendingProcess(); break;
    case "dsa-refresh-stack":      DSA.refreshStack(); break;
    case "dsa-refresh-queue":      DSA.refreshQueue(); break;
    case "dsa-search":             DSA.runSearch(); break;
    case "dsa-greedy":             DSA.runGreedy(); break;
  }
}

function handleSelect(sel) {
  if (sel === "account")      selectLoginField("account");
  else if (sel === "pin")     selectLoginField("pin");
  else if (sel === "withdraw") Keypad.setTarget(withdrawTarget);
  else if (sel === "deposit")  Keypad.setTarget(depositTarget);
  else if (sel === "pending")  Keypad.setTarget(pendingTarget);
}

// ---------------- login ----------------

function selectLoginField(which) {
  document.getElementById("login-account-field").classList.toggle("active", which === "account");
  document.getElementById("login-pin-field").classList.toggle("active", which === "pin");
  Keypad.setTarget(which === "account" ? loginAccountTarget : loginPinTarget);
}

function setupLogin() {
  loginAccountTarget.value = "";
  loginPinTarget.value = "";
  selectLoginField("account");
}

async function doLogin() {
  const acc = loginAccountTarget.value;
  const pin = loginPinTarget.value;
  if (!acc || !pin) { notify("Please enter account number and PIN", "error"); return; }
  setLoading(true, "Verifying...");
  try {
    const data = await API.login(acc, pin);
    if (data.success) {
      state.name = data.name;
      state.accountNumber = data.accountNumber;
      state.balance = data.balance;
      notify("Welcome, " + data.name, "success");
      showScreen("screen-menu");
    } else {
      notify(data.message || "Login failed", "error");
      if ((data.message || "").includes("Account not found")) {
        loginAccountTarget.value = "";
        selectLoginField("account");
      } else {
        loginPinTarget.value = "";
        selectLoginField("pin");
      }
    }
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

async function checkSession() {
  try {
    const data = await API.session();
    if (data.authenticated) {
      state.name = data.name;
      state.accountNumber = data.accountNumber;
      state.balance = data.balance;
      showScreen("screen-menu");
    }
  } catch (e) { /* ignore - stay on welcome */ }
}

async function doLogout() {
  setLoading(true);
  try { await API.logout(); } catch (e) { /* ignore */ }
  state = { name: null, accountNumber: null, balance: null };
  notify("Logged out", "info");
  showScreen("screen-welcome");
  setLoading(false);
}

function updateMenu() {
  document.getElementById("menu-name").textContent = state.name || "User";
  document.getElementById("menu-balance").textContent = state.balance != null ? inr(state.balance) : "--";
}

// ---------------- balance ----------------

async function openBalance() {
  showScreen("screen-balance");
  setLoading(true);
  try {
    const data = await API.balance();
    state.balance = data.balance;
    const el = document.getElementById("balance-amount");
    el.textContent = inr(data.balance);
    el.classList.remove("pop"); void el.offsetWidth; el.classList.add("pop");
    document.getElementById("balance-acct-no").textContent = state.accountNumber;
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

// ---------------- withdraw ----------------

async function doWithdraw(amount) {
  amount = Number(amount);
  if (!amount || amount <= 0) { notify("Enter a valid amount", "error"); return; }
  setLoading(true, "Dispensing...");
  try {
    const data = await API.withdraw(amount);
    if (data.success) {
      state.balance = data.balance;
      showReceipt({ type: "WITHDRAW", amount, breakdown: data.breakdown, balance: data.balance });
      notify("Withdrawal successful", "success");
      withdrawTarget.value = ""; Keypad.render();
    } else { notify(data.message, "error"); }
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

// ---------------- deposit ----------------

async function doDeposit(amount) {
  amount = Number(amount);
  if (!amount || amount <= 0) { notify("Enter a valid amount", "error"); return; }
  setLoading(true, "Depositing...");
  try {
    const data = await API.deposit(amount);
    if (data.success) {
      state.balance = data.balance;
      showReceipt({ type: "DEPOSIT", amount, balance: data.balance });
      notify("Deposit successful", "success");
      depositTarget.value = ""; Keypad.render();
    } else { notify(data.message, "error"); }
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

// ---------------- history ----------------

async function openHistory() {
  showScreen("screen-history");
  const list = document.getElementById("history-list");
  list.innerHTML = '<p class="empty">Loading...</p>';
  try {
    const data = await API.history();
    const txns = data.transactions || [];
    if (!txns.length) { list.innerHTML = '<p class="empty">No transactions yet.</p>'; return; }
    list.innerHTML = txns.map(historyRow).join("");
  } catch (e) { list.innerHTML = '<p class="empty">' + e.message + "</p>"; }
}

function typeClass(type) {
  if (type.startsWith("PENDING_WITHDRAW")) return "PENDING_WITHDRAW";
  if (type.startsWith("PENDING_DEPOSIT"))  return "PENDING_DEPOSIT";
  if (type.startsWith("DEPOSIT"))  return "deposit";
  if (type.startsWith("REVERSAL")) return "REVERSAL";
  return "withdraw";
}

function historyRow(t) {
  const cls = typeClass(t.type);
  const amtColor = (cls === "deposit" || cls === "PENDING_DEPOSIT") ? "deposit" : "withdraw";
  return '<div class="history-item">' +
    '<div><div class="h-type ' + cls + '">' + t.type + "</div>" +
    '<div class="h-id">#' + t.id + (t.reversed ? " &middot; reversed" : "") + "</div></div>" +
    '<div style="text-align:right"><div class="h-amount ' + amtColor + '">' + inr(t.amount) + "</div>" +
    '<div class="h-time">' + t.timestamp + "</div></div></div>";
}

async function doReverse() {
  setLoading(true, "Reversing...");
  try {
    const data = await API.reverse();
    if (data.success) {
      state.balance = data.balance;
      showReceipt({ type: "REVERSAL", reversedType: data.reversedType, amount: data.reversedAmount, balance: data.balance });
      notify("Transaction reversed", "success");
    } else { notify(data.message, "error"); }
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

// ---------------- pending (queue) ----------------

async function openPending() {
  showScreen("screen-pending");
  refreshPending();
}

async function refreshPending() {
  const list = document.getElementById("pending-list");
  try {
    const data = await API.pendingList();
    const items = data.requests || [];
    if (!items.length) { list.innerHTML = '<p class="empty">No pending requests.</p>'; return; }
    list.innerHTML = items.map((t, i) =>
      '<div class="pend-item ' + (i === 0 ? "pend-front" : "") + '">' +
      '<div class="pend-meta"><span class="pend-pos">' + (i === 0 ? "F" : i + 1) + "</span>" +
      "<span>" + t.type.replace("PENDING_", "") + "</span></div>" +
      '<span class="pend-amt">' + inr(t.amount) + "</span></div>").join("");
  } catch (e) { list.innerHTML = '<p class="empty">' + e.message + "</p>"; }
}

async function doPendingAdd(type) {
  const amount = Number(pendingTarget.value);
  if (!amount || amount <= 0) { notify("Enter an amount", "error"); return; }
  setLoading(true);
  try {
    const data = await API.pendingAdd(type, amount);
    if (data.success) {
      notify("Request added to queue (" + data.size + " pending)", "success");
      pendingTarget.value = ""; Keypad.render();
      refreshPending();
    } else { notify(data.message, "error"); }
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

async function doPendingProcess() {
  setLoading(true, "Processing...");
  try {
    const data = await API.pendingProcess();
    if (data.success) {
      state.balance = data.balance;
      notify(data.kind + " of " + inr(data.amount) + " processed (FIFO)", "success");
      refreshPending();
    } else { notify(data.message, "error"); }
  } catch (e) { notify(e.message, "error"); }
  setLoading(false);
}

// ---------------- DSA ----------------

function openDsa() {
  showScreen("screen-dsa");
  DSA.refreshActive();
}

// ---------------- receipt ----------------

function showReceipt(data) {
  const now = new Date().toLocaleString("en-IN");
  let body = "";
  if (data.type === "WITHDRAW") {
    body = '<div class="r-row"><span>Withdrawal</span><span>' + inr(data.amount) + "</span></div>";
    const bk = data.breakdown || {};
    const notes = Object.keys(bk).map(Number).sort((a, b) => b - a).filter(d => bk[String(d)] > 0);
    if (notes.length) {
      body += '<div class="r-notes"><strong>Notes dispensed:</strong><br>';
      notes.forEach(d => { body += inr(d) + " x " + bk[String(d)] + " = " + inr(d * bk[String(d)]) + "<br>"; });
      body += "</div>";
    }
  } else if (data.type === "DEPOSIT") {
    body = '<div class="r-row"><span>Deposit</span><span>' + inr(data.amount) + "</span></div>";
  } else if (data.type === "REVERSAL") {
    body = '<div class="r-row"><span>Reversed ' + data.reversedType + "</span><span>" + inr(data.amount) + "</span></div>";
  } else {
    body = '<div class="r-row"><span>Balance Inquiry</span><span></span></div>';
  }

  document.getElementById("receipt-content").innerHTML =
    '<div class="r-head">NEXUS BANK</div><div class="r-sub">ATM RECEIPT</div>' +
    '<hr class="r-divider">' +
    '<div class="r-row"><span>Account</span><span>' + (state.accountNumber || "--") + "</span></div>" +
    '<div class="r-row"><span>Date</span><span>' + now + "</span></div>" +
    '<hr class="r-divider">' + body +
    '<hr class="r-divider">' +
    '<div class="r-row r-total"><span>Balance</span><span>' + inr(data.balance) + "</span></div>" +
    '<div class="r-foot">Thank you for banking with NEXUS</div>';
  showScreen("screen-receipt");
}

function printReceipt() { window.print(); }

// ---------------- UI helpers ----------------

function setLoading(on, text) {
  const ov = document.getElementById("loading");
  ov.hidden = !on;
  if (on && text) document.getElementById("loading-text").textContent = text;
}

let notifyTimer;
function notify(msg, type) {
  const n = document.getElementById("notification");
  n.textContent = msg;
  n.className = "notification " + (type || "info") + " show";
  n.hidden = false;
  clearTimeout(notifyTimer);
  notifyTimer = setTimeout(() => { n.hidden = true; n.classList.remove("show"); }, 3000);
}

document.addEventListener("DOMContentLoaded", init);
