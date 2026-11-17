# Admission research and its limited role in this experiment

Primary sources consulted on 2026-11-17:

- Einziger, Friedman and Manes, [TinyLFU: A Highly Efficient Cache Admission Policy](https://arxiv.org/html/1512.00727v2), especially sections 3.3 and 4.
- Caffeine's maintainer documentation, [Efficiency](https://github.com/ben-manes/caffeine/wiki/Efficiency), especially its Window TinyLFU description.

TinyLFU ages its approximate frequency history by halving counters at a sample boundary. Caffeine combines frequency-based admission with a small recency window, allowing bursts to demonstrate value before competing for the main cache. These designs make recent access history relevant; they do not make an irreversible decision from the first prefix of a request stream.

Our inference is narrower than adopting either implementation: both cold-to-hot and hot-to-unique transitions are necessary experimental inputs for the iteration memo. The first quantifies useful reuse lost by the one-time cutoff; the second quantifies continued lookup and insertion cost after a useful phase ends. A separate reuse-distance sweep must begin from known admission so the first-window gate is not confused with table capacity.

No Caffeine code or dependency is added. Its frequency sketch and replacement lists solve a broader cache-management problem and would add metadata and work to this direct-mapped, iteration-local memo. The present `always` arm bypasses one cutoff only. If it exposes a useful opportunity, a bounded reconsideration policy with recent-window evidence is a subsequent experiment, not a conclusion assumed here.
