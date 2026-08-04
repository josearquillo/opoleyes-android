import json
import re
import difflib
import glob
import os


def normalize(t):
    return re.sub(r"\s+", " ", t.lower().strip())


def max_option_similarity(opts):
    keys = ["A", "B", "C", "D"]
    texts = [normalize(opts[k]) for k in keys]
    max_sim = 0.0
    for i in range(4):
        for j in range(i + 1, 4):
            s = difflib.SequenceMatcher(None, texts[i], texts[j]).ratio()
            if s > max_sim:
                max_sim = s
    return max_sim


def numeric_difficulty(opts, correct, en):
    keys = ["A", "B", "C", "D"]
    nums = {}
    for k in keys:
        found = re.findall(r"\b\d+(?:[.,]\d+)?\b", opts[k])
        if found:
            nums[k] = [float(n.replace(",", ".")) for n in found]
        else:
            nums[k] = []
    if not nums[correct]:
        return 0
    all_numeric = all(len(nums[k]) > 0 for k in keys)
    all_percent = all(re.search(r"%", opts[k]) for k in keys) and ("%" in en or "por ciento" in en or "porcentaje" in en)
    all_days = all("día" in normalize(opts[k]) for k in keys) and "día" in en
    all_years = all_numeric and all(1800 <= n <= 2100 for k in keys for n in nums[k])
    if all_numeric:
        flat = [n for k in keys for n in nums[k]]
        unique = set(flat)
        if (all_percent or all_days) and len(unique) >= 4:
            return 2.5
        if (all_percent or all_days) and len(unique) >= 3:
            return 1.5
        if all_years and len(unique) >= 4:
            return 1.2
        if len(unique) >= 4:
            return 1.8
        if len(unique) >= 3:
            return 1.0
        return 0.6
    numeric_opts = [k for k in keys if nums[k]]
    if len(numeric_opts) >= 2:
        return 0.5
    return 0


def compute_score(item):
    score = 2.0
    en = normalize(item["enunciado"])
    opts = item["opciones"]
    correct = item["correct"].strip().upper()

    # 1. Negative / exception wording
    if re.search(r"\bno (será|es|corresponde|seran|sera|se considera|constituye|debe|podrá)\b", en):
        score += 0.5
    if re.search(r"\b(excepto|salvo|menos|falso|incorrecto|denegar|nunca)\b", en):
        score += 0.4

    # 2. Concrete legal references
    if re.search(r"\bartículo\s*\d+(?:\.\d+)?\b", en):
        score += 0.6
    if re.search(r"\bley\s+orgánica\s+\d+/\d+\b", en) or re.search(r"\bley\s+\d+/\d+\b", en):
        score += 0.4
    if re.search(r"\breal\s+decreto\s+\d+/\d+\b", en):
        score += 0.3

    # 3. Numerical / deadline content in the statement
    if re.search(r"\b\d+\s*(días?|meses?|años?|semanas?|horas?)\b", en):
        score += 0.9
    if re.search(r"\b\d+%\b", en):
        score += 0.9
    if re.search(r"\b\d+\s*euros?\b", en):
        score += 0.6

    # 4. Options with lots of similar words / phrasing
    sim = max_option_similarity(opts)
    if sim >= 0.85:
        score += 1.8
    elif sim >= 0.70:
        score += 1.0
    elif sim >= 0.55:
        score += 0.5
    elif sim <= 0.25:
        score -= 0.6

    # 5. Numerical options that can be confused
    score += numeric_difficulty(opts, correct, en)

    # 6. Legal / procedural jargon density
    legal_terms = [
        "jurisprudencia", "procedimiento", "auto", "sentencia", "decreto",
        "providencia", "recurso", "plazo", "alegaciones", "incoado",
        "incomparecencia", "excepción", "requisito", "tutela", "legitimación",
        "jurisdiccional", "audiencia", "juzgado",
        "denuncia", "querella", "acusación", "investigación", "diligencia",
        "resolución", "competencia"
    ]
    term_count = sum(1 for term in legal_terms if term in en)
    if term_count >= 3:
        score += 0.7
    elif term_count >= 1:
        score += 0.2

    # 7. Very short options with a clearly different one
    avg_len = sum(len(normalize(opts[k])) for k in ["A", "B", "C", "D"]) / 4
    if avg_len < 40 and sim < 0.35:
        score -= 1.0
    elif avg_len < 30:
        score -= 0.5

    # 8. Statement length / syntactic complexity
    if len(en) > 180:
        score += 0.3
    if len(en) < 80:
        score -= 0.4

    # 9. High-level doctrinal/jurisprudential nuance
    if "jurisprudencia" in en or "doctrina" in en or "criterio reiterado" in en:
        score += 1.0

    # 10. Multiple numerical / temporal cues in both statement and options
    stmt_has_num = bool(re.search(r"\b\d+", en))
    opts_have_num = sum(1 for k in ["A","B","C","D"] if re.search(r"\b\d+", opts[k]))
    if stmt_has_num and opts_have_num >= 2:
        score += 0.6

    return score


def percentile_thresholds(scores, cutoffs):
    """cutoffs: cumulative percentages (e.g. [0.12, 0.38, 0.70, 0.90])"""
    sorted_scores = sorted(scores)
    n = len(sorted_scores)
    thresholds = []
    for p in cutoffs:
        idx = int(p * n)
        if idx >= n:
            idx = n - 1
        thresholds.append(sorted_scores[idx])
    return thresholds


def main():
    batch_dir = "batches"
    files = sorted(glob.glob(os.path.join(batch_dir, "batch_*.json")))
    files = [f for f in files if not f.endswith("_result.json")]

    # Pass 1: compute scores for all questions
    all_items = []  # list of (file_index, item_index, item, score)
    all_scores = []
    for fi, in_path in enumerate(files):
        with open(in_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        for ii, item in enumerate(data):
            score = compute_score(item)
            all_items.append((fi, ii, in_path, item, score))
            all_scores.append(score)

    # Determine difficulty thresholds by percentiles.
    # Targets: 1=12%, 2=26% (cum 38%), 3=32% (cum 70%), 4=20% (cum 90%), 5=10%.
    cutoffs = [0.12, 0.38, 0.70, 0.90]
    thresholds = percentile_thresholds(all_scores, cutoffs)

    def score_to_difficulty(score):
        if score <= thresholds[0]:
            return 1
        if score <= thresholds[1]:
            return 2
        if score <= thresholds[2]:
            return 3
        if score <= thresholds[3]:
            return 4
        return 5

    # Assign difficulty
    for _, _, _, item, score in all_items:
        item["difficulty"] = score_to_difficulty(score)

    # Pass 2: write result files
    total_counts = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
    total = 0
    for fi, in_path in enumerate(files):
        items_for_file = [(item, score) for fii, _, inp, item, score in all_items if fii == fi]
        results = []
        for item, score in items_for_file:
            results.append({
                "test_index": item["test_index"],
                "question_index": item["question_index"],
                "id": item["id"],
                "test_id": item["test_id"],
                "orig_id": item["orig_id"],
                "difficulty": item["difficulty"]
            })
        out_path = in_path.replace(".json", "_result.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(results, f, ensure_ascii=False, indent=1)
        counts = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
        for r in results:
            counts[r["difficulty"]] += 1
            total_counts[r["difficulty"]] += 1
        total += len(results)
        print(f"{os.path.basename(in_path)}: {len(results)} -> {counts}")

    print(f"TOTAL: {total} -> {total_counts}")
    print(f"Percentile thresholds: {thresholds}")


if __name__ == "__main__":
    main()
