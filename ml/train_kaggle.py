"""OptiMilk-AI Kaggle/Local training script for the simulated demo dataset.

Kaggle: Add Input -> upload 02_AUGMENTED_0_to_100_500_cycles.csv;
copy this file into a Kaggle notebook cell, or run it as a Python script.
Local: python ml/train_kaggle.py
Dependency: numpy, pandas, scikit-learn.
CAUTION: Simulated-data accuracy is not real milk-adulteration accuracy.
"""
from pathlib import Path
import argparse
import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split

COLUMNS = ["sample_no", "repeat", "LED", "R", "G", "B", "Clear", "water_pct"]
FEATURES = ["A_RED", "A_GREEN", "A_BLUE"]
# Measured pure-water reference Clear counts for red/green/blue LED states.
# Always use the same sensor gain, LED current, timing and optical geometry.
REFERENCE = {"RED": 288.0, "GREEN": 9106.0, "BLUE": 1603.0}

def find_csv(path=None):
    if path:
        p = Path(path)
        if not p.is_file():
            raise FileNotFoundError(p)
        return p
    filename = "02_AUGMENTED_0_to_100_500_cycles.csv"
    roots = [Path("/kaggle/input"), Path(__file__).resolve().parents[1] / "data" / "raw", Path.cwd()]
    for root in roots:
        if root.exists():
            paths = sorted(root.rglob(filename))
            if paths:
                return paths[0]
    raise FileNotFoundError("Upload " + filename + " on Kaggle or pass --data PATH")

def prepare(csv_path):
    raw = pd.read_csv(csv_path)
    if raw.columns.tolist() != COLUMNS or raw[COLUMNS].isna().any().any():
        raise ValueError("CSV must contain eight required columns with no missing values")
    if raw.duplicated(["sample_no","repeat","LED"]).any():
        raise ValueError("Duplicate LED row within one measurement cycle")
    # Pivot four LED states into one cycle, then subtract that cycle's OFF value.
    cycles = raw.pivot(
        index=["sample_no","repeat","water_pct"], columns="LED", values="Clear"
    ).reset_index()
    if cycles[["OFF","RED","GREEN","BLUE"]].isna().any().any():
        raise ValueError("Each cycle requires OFF, RED, GREEN and BLUE readings")
    for led in ("RED", "GREEN", "BLUE"):
        cycles["NET_" + led] = cycles[led] - cycles["OFF"]
    # Five cycles form ONE ML observation (not five independent samples).
    net_cols = ["NET_RED","NET_GREEN","NET_BLUE"]
    sample = cycles.groupby(["sample_no","water_pct"], as_index=False)[net_cols].median()
    for led in ("RED", "GREEN", "BLUE"):
        t = sample["NET_" + led] / REFERENCE[led]
        if (t <= 0).any():
            raise ValueError("Nonpositive transmission: " + led)
        sample["A_" + led] = -np.log10(t)
    return raw, sample

def write_esp32_header(model, output):
    b0 = float(model.intercept_)
    b1, b2, b3 = [float(v) for v in model.coef_]
    # REG_* names avoid Arduino's built-in B0/B1 constants.
    header = f"""// SIMULATED data only: demo model, not validated milk measurement.
#pragma once
static const float REG_INTERCEPT = {b0:.10f}f;
static const float REG_RED = {b1:.10f}f;
static const float REG_GREEN = {b2:.10f}f;
static const float REG_BLUE = {b3:.10f}f;
static const float REF_RED_CLEAR = 288.0f;
static const float REF_GREEN_CLEAR = 9106.0f;
static const float REF_BLUE_CLEAR = 1603.0f;
inline float predictWaterPct(float aR, float aG, float aB) {{
  return REG_INTERCEPT + REG_RED*aR + REG_GREEN*aG + REG_BLUE*aB;
}}
"""
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(header, encoding="utf-8")
    return header

def main():
    parser = argparse.ArgumentParser(description="Train embedded Ridge Regression demo")
    parser.add_argument("--data", default=None, help="Augmented CSV path (auto-detected on Kaggle)")
    parser.add_argument("--output", default="model_parameters.h", help="Generated Arduino header")
    args, _unknown = parser.parse_known_args()
    path = find_csv(args.data)
    raw, features = prepare(path)
    X, y = features[FEATURES], features["water_pct"]
    print("Loaded:", path, "| raw rows:", len(raw), "| synthetic sample profiles:", len(features))
    print("Concentrations:", sorted(features["water_pct"].unique().tolist()))
    stratify = y if y.value_counts().min() >= 2 else None
    trainX, testX, trainY, testY = train_test_split(
        X, y, test_size=0.22, random_state=42, stratify=stratify
    )
    for name, model in [("Linear", LinearRegression()), ("Ridge", Ridge(alpha=0.01))]:
        model.fit(trainX, trainY)
        pred = model.predict(testX)
        print(f"{name}: MAE={mean_absolute_error(testY, pred):.3f} percentage points, "
              f"RMSE={np.sqrt(mean_squared_error(testY, pred)):.3f}, "
              f"R²={r2_score(testY, pred):.4f} (SIMULATED TEST ONLY)")
    # Refit with all simulated examples before demonstrating ESP32 inference.
    model = Ridge(alpha=0.01).fit(X, y)
    print("REG_INTERCEPT =", repr(float(model.intercept_)))
    for name, coeff in zip(FEATURES, model.coef_):
        print(name, "coefficient =", repr(float(coeff)))
    target = Path(args.output)
    print(write_esp32_header(model, target))
    print("ESP32 parameters written to:", target.resolve())
    # Reproduce ESP32 float32 arithmetic and compare with Python.
    row = X.iloc[0].to_numpy(dtype=np.float32)
    fp32 = float(np.float32(model.intercept_) + np.sum(
        np.asarray(model.coef_, dtype=np.float32) * row, dtype=np.float32
    ))
    python_val = float(model.predict(X.iloc[[0]])[0])
    assert abs(fp32 - python_val) < 0.01
    print("PASS: ESP32 float32 prediction parity; difference =", abs(fp32-python_val))

if __name__ == "__main__":
    main()
