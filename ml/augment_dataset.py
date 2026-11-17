"""Rebuild the two reproducible simulated sensor CSVs in data/raw/.

The seven water-cuvette calibration cycles below were supplied from the
ESP32/TCS34725. Milk attenuation is assumed, not experimentally measured.
All milk-water dilution rows (including "raw") are synthetic demonstrations.

Run: python ml/augment_dataset.py
Requires: numpy
"""
from pathlib import Path
from collections import defaultdict
import csv
import numpy as np

OUT = Path(__file__).resolve().parents[1] / "data" / "raw"
OUT.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(20261118)
COLS = ["sample_no", "repeat", "LED", "R", "G", "B", "Clear", "water_pct"]
LEDS = ("RED", "GREEN", "BLUE")
CHANNELS = ("R", "G", "B", "Clear")

# Stable real water-only readings, not measurements of milk-water mixtures.
water = np.array([
    [[254,23,32,280], [482,5530,3396,8954], [33,306,1299,1603]],
    [[261,23,32,289], [482,5539,3402,8968], [34,310,1316,1624]],
    [[260,23,32,287], [478,5497,3377,8898], [33,308,1306,1611]],
    [[261,23,32,288], [489,5623,3458,9138], [33,309,1310,1619]],
    [[262,23,32,288], [488,5607,3446,9106], [33,302,1282,1583]],
    [[263,24,32,290], [496,5698,3505,9259], [33,301,1276,1575]],
    [[266,24,33,294], [493,5661,3480,9197], [33,302,1285,1586]],
], dtype=float)
reference = np.median(water, axis=0)
residual = np.log(water / reference)
residual -= residual.mean(axis=0, keepdims=True)

# Hypothetical, NOT measured, apparent absorbance of pure milk.
attenuation_milk = np.array([0.47, 0.71, 0.76], dtype=float)
attenuation_curve = np.array([1.08, 1.17, 1.10], dtype=float)

# 100 simulated profiles: 9 at each 0..90%, 10 at 100%.
profiles = []
for water_pct in range(0, 101, 10):
    n_samples = 10 if water_pct == 100 else 9
    for physical_idx in range(1, n_samples + 1):
        profiles.append((water_pct, physical_idx))

# Hierarchical variation and 12-channel correlated residual resampling.
batch_att_shift = rng.normal(0, 0.017, size=(5, 3))
synthetic = []
for water_pct, physical_idx in profiles:
    milk_fraction = 1.0 - water_pct / 100.0
    sample_id = f"W{water_pct:03d}_S{physical_idx:02d}"
    global_perturb = rng.normal(0, 0.015) * milk_fraction
    led_perturb = rng.normal(0, 0.010, size=3) * milk_fraction
    a = attenuation_milk * milk_fraction**attenuation_curve
    a += batch_att_shift[(physical_idx - 1) % 5] * milk_fraction
    a += global_perturb + led_perturb
    a = np.maximum(0, a)

    for rep in range(1, 6):
        synthetic.append(dict(zip(COLS, [sample_id, rep, "OFF", 0, 0, 0, 0, water_pct])))
        r = residual[int(rng.integers(len(residual)))]
        cycle_shift = rng.normal(0, 0.004)
        led_shifts = rng.normal(0, 0.0045, size=3)
        for led_idx, led in enumerate(LEDS):
            transmitted = reference[led_idx] * 10.0 ** (-a[led_idx])
            joint_log_noise = 0.7 * r[led_idx] + cycle_shift + led_shifts[led_idx]
            joint_log_noise += rng.normal(0, 0.003, size=4)
            small_additive = rng.normal(0, 0.55 + 0.016*np.sqrt(transmitted), size=4)
            values = np.rint(np.clip(transmitted * np.exp(joint_log_noise) + small_additive, 0, 65535)).astype(int)
            synthetic.append({
                "sample_no": sample_id, "repeat": rep, "LED": led,
                **dict(zip(CHANNELS, map(int, values))), "water_pct": water_pct,
            })

# The base dataset is one profile per concentration, five repeats each.
base = [row for row in synthetic if row["sample_no"].endswith("_S01")]
assert len(base) == 220
assert len(synthetic) == 2000
for filename, rows in [
    ("01_RAW_0_to_100_5_repeats.csv", base),
    ("02_AUGMENTED_0_to_100_500_cycles.csv", synthetic),
]:
    path = OUT / filename
    with path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=COLS)
        writer.writeheader()
        writer.writerows(rows)
    assert len(rows) == (220 if filename.startswith("01") else 2000)
    print(f"Wrote {len(rows)} rows to {path}")

# Complete-cycle and uniqueness validations.
by_cycle = defaultdict(list)
for row in synthetic:
    by_cycle[(row["sample_no"], row["repeat"])].append(row)
assert len(by_cycle) == 500
assert all([r["LED"] for r in rows] == ["OFF","RED","GREEN","BLUE"] for rows in by_cycle.values())
assert all(0 <= int(r[c]) <= 65535 for r in synthetic for c in CHANNELS)
fingerprints = {
    tuple(int(r[c]) for r in rows[1:] for c in CHANNELS)
    for rows in by_cycle.values()
}
assert len(fingerprints) == 500
print("Verified: 55 base cycles, 500 unique augmented cycles, concentrations 0–100%.")
