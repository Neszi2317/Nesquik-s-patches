/* SPDX-License-Identifier: MPL-2.0
 * Local Website Blocker 0.1.3.
 * Rules are embedded at APK patch time. No UI, telemetry, dynamic downloads,
 * preferences or external services. Handles domains and exact URL path rules.
 */
const RULES = /*__LWB_RULES__*/ [];
const CONTRACT = "@local.nesquikos/firefox-website-blocker;1";
const CID = Components.ID("{998f6fb9-6867-4d27-bf8c-4747b6747c8e}");
const hosts = new Set();
const wildcardSuffixes = new Set();
const subreddits = new Map();     // hostname -> Set of subreddit slugs
const pageRules = new Map();      // hostname -> specific URL path/query prefixes

function canonicalHost(raw) {
  try {
    const value = raw.trim().replace(/\.$/, "");
    if (!value || /[/?#@*\s]/.test(value)) return null;
    const bracket = value.includes(":") && !value.startsWith("[") ? `[${value}]` : value;
    return Services.io.newURI(`http://${bracket}/`).asciiHost
      .toLowerCase().replace(/\.$/, "").replace(/^\[|\]$/g, "");
  } catch (_) {
    return null;
  }
}
function addRule(raw) {
  if (typeof raw !== "string") return;
  // Accept multi-entry lines from old TXT exports (including /r/foo continuations).
  for (let token of raw.trim().split(/\s+/)) {
    if (!token || token[0] === "#") continue;
    if (/^\/r\//i.test(token)) token = `reddit.com${token}`;
    token = token.replace(/^https?:\/\//i, "").replace(/^www\./i, "");
    if (token.startsWith("*.")) {
      const suffix = canonicalHost(token.slice(2));
      if (suffix) wildcardSuffixes.add(suffix);
      continue;
    }
    const slash = token.indexOf("/");
    if (slash < 0) {
      const host = canonicalHost(token);
      if (host) hosts.add(host);
      continue;
    }
    const host = canonicalHost(token.slice(0, slash));
    if (!host) continue;
    const path = token.slice(slash).toLowerCase().replace(/\/$/, "");
    if (!path) {
      hosts.add(host);
      continue;
    }
    const subreddit = /^\/r\/([a-z0-9_]{1,64})$/.exec(path);
    if (subreddit && (host === "reddit.com" || host.endsWith(".reddit.com"))) {
      if (!subreddits.has("reddit.com")) subreddits.set("reddit.com", new Set());
      subreddits.get("reddit.com").add(subreddit[1]);
    } else {
      if (!pageRules.has(host)) pageRules.set(host, new Set());
      pageRules.get(host).add(path);
    }
  }
}
for (const rule of RULES) addRule(rule);

// Subdomain checks take O(number of hostname labels), not O(number of rules).
function hostCandidates(host) {
  const candidates = [host];
  if (host.includes(":") || /^[0-9.]+$/.test(host)) return candidates;
  for (let dot = host.indexOf("."); dot !== -1; dot = host.indexOf(".")) {
    host = host.slice(dot + 1);
    candidates.push(host);
  }
  return candidates;
}
function pathMatched(path, prefix) {
  if (path === prefix) return true;
  if (path.startsWith(prefix + "/") || path.startsWith(prefix + "?") || path.startsWith(prefix + "#")) return true;
  // Exact query prefixes may be followed by another query parameter, but not
  // by additional characters in the last parameter value.
  if (prefix.includes("?") && path.startsWith(prefix) && path[prefix.length] === "&") return true;
  return false;
}
export function blockedURI(uri) {
  if (!uri || !(uri.schemeIs("http") || uri.schemeIs("https"))) return false;
  let host;
  try { host = uri.asciiHost.toLowerCase().replace(/\.$/, "").replace(/^\[|\]$/g, ""); }
  catch (_) { return false; }
  const candidates = hostCandidates(host);
  for (const candidate of candidates) {
    if (hosts.has(candidate) || wildcardSuffixes.has(candidate)) return true;
  }
  if (subreddits.size === 0 && pageRules.size === 0) return false;
  let path;
  try { path = (uri.pathQueryRef || "/").toLowerCase(); }
  catch (_) { return false; }
  for (const candidate of candidates) {
    const groups = subreddits.get(candidate);
    if (groups) {
      const match = /^\/r\/([^/?#]+)/.exec(path);
      if (match && groups.has(match[1])) return true;
    }
    const prefixes = pageRules.get(candidate);
    if (prefixes) for (const prefix of prefixes) {
      if (pathMatched(path, prefix)) return true;
    }
  }
  return false;
}

const policy = {
  QueryInterface: ChromeUtils.generateQI(["nsIContentPolicy"]),
  shouldLoad(uri) { return blockedURI(uri) ? Ci.nsIContentPolicy.REJECT_REQUEST : Ci.nsIContentPolicy.ACCEPT; },
  shouldProcess(uri) { return this.shouldLoad(uri); },
};
const factory = {
  QueryInterface: ChromeUtils.generateQI(["nsIFactory"]),
  createInstance(iid) { return policy.QueryInterface(iid); },
};
const networkObserver = {
  QueryInterface: ChromeUtils.generateQI(["nsIObserver"]),
  observe(subject) {
    const channel = subject.QueryInterface(Ci.nsIHttpChannel);
    if (blockedURI(channel.URI)) channel.cancel(Cr.NS_ERROR_BLOCKED_BY_POLICY);
  },
};
let installed = false;
export function install() {
  if (installed || (hosts.size === 0 && wildcardSuffixes.size === 0 && subreddits.size === 0 && pageRules.size === 0)) return;
  const registrar = Components.manager.QueryInterface(Ci.nsIComponentRegistrar);
  registrar.registerFactory(CID, "Local Website Blocker", CONTRACT, factory);
  Services.catMan.addCategoryEntry("content-policy", CONTRACT, CONTRACT, false, true);
  Services.obs.addObserver(networkObserver, "http-on-modify-request");
  installed = true;
}
