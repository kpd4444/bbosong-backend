# Test Fixtures

The baseline/load/stress/verify scripts use this default image:

```text
k6/tests/fixtures/black_jacket_001.png
```

The default k6 scripts reference it as `tests/fixtures/black_jacket_001.png` because k6 resolves `open()` relative to the `k6` script directory.

Accuracy fixtures must have both an image and a matching label image, and must be listed in `expected.json` or `expected-reliable.json`.
