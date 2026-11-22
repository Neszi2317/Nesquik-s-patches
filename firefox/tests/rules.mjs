import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

function wrap(uri) {
  const url = new URL(uri);
  return {
    schemeIs: name => url.protocol === name + ':',
    asciiHost: url.hostname,
    pathQueryRef: url.pathname + url.search + url.hash,
  };
}

const attrs = { calls: [] };
globalThis.Components = { ID: x => x, manager: { QueryInterface: () => ({
  registerFactory: (_cid, _name, _contract, factory) => { attrs.policy = factory.createInstance(); },
}) } };
globalThis.ChromeUtils = { generateQI: () => function () { return this; } };
globalThis.Ci = {
  nsIContentPolicy: { ACCEPT: 1, REJECT_REQUEST: -1 },
};
globalThis.Cr = { NS_ERROR_BLOCKED_BY_POLICY: 'blocked' };
globalThis.Services = {
  io: { newURI: wrap },
  catMan: { addCategoryEntry: () => attrs.calls.push('content-policy') },
  obs: { addObserver: (observer, topic) => { attrs.calls.push(topic); attrs.observer = observer; } },
};

const rules = ['example.org', 'https://example.net/', 'example.com/blocked', 'www.reddit.com/r/example', '*.blocked.test'];
const source = (await readFile(process.argv[2] ?? new URL('../src/LocalWebsiteBlocker.sys.mjs', import.meta.url), 'utf8')).replace('/*__LWB_RULES__*/ []', JSON.stringify(rules));
const mod = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
mod.install();
mod.install();
assert.deepEqual(attrs.calls, ['content-policy','http-on-modify-request']);
const cases = new Map([
  ['https://example.org/', true],
  ['https://a.example.org/a', true],
  ['http://notexample.org/', false],
  ['https://example.org.allowed.test/', false],
  ['https://example.com/blocked', true],
  ['https://example.com/blocked/a?q=1', true],
  ['https://example.com/blocked-other', false],
  ['https://example.com/allowed', false],
  ['https://old.reddit.com/r/example/comments/1', true],
  ['https://www.reddit.com/r/example_other', false],
  ['https://www.reddit.com/', false],
  ['https://foo.blocked.test/a', true],
  ['about:config', false],
  ['https://example.net/', true],
  ['https://example.net/other', true],
  ['https://example.net.allowed.test/', false],
]);
for (const [url, blocked] of cases) {
  const uri = wrap(url);
  assert.equal(mod.blockedURI(uri), blocked, url);
  assert.equal(attrs.policy.shouldLoad(uri), blocked ? -1 : 1, url);
  let wasCancelled = false;
  const channel = { URI: uri, cancel: reason => { assert.equal(reason, 'blocked'); wasCancelled = true; } };
  attrs.observer.observe({ QueryInterface: () => channel });
  assert.equal(wasCancelled, blocked, url);
}
console.log(`${cases.size} URL × 3 checks passed`);
