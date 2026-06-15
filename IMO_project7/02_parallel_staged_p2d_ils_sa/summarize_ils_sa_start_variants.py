#!/usr/bin/env python3
import csv
import math
import sys
from collections import defaultdict
from pathlib import Path


def mean(xs):
    return sum(xs) / len(xs) if xs else float('nan')


def stdev(xs):
    if len(xs) < 2:
        return 0.0
    m = mean(xs)
    return math.sqrt(sum((x - m) ** 2 for x in xs) / (len(xs) - 1))


def main() -> int:
    if len(sys.argv) < 2:
        print('usage: summarize_ils_sa_start_variants.py raw.csv [out_dir]', file=sys.stderr)
        return 2
    raw_path = Path(sys.argv[1])
    out_dir = Path(sys.argv[2]) if len(sys.argv) >= 3 else raw_path.parent
    out_dir.mkdir(parents=True, exist_ok=True)

    rows = []
    with raw_path.open(newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)

    groups = defaultdict(list)
    for row in rows:
        groups[(row['instance'], row['algorithm'])].append(row)

    summary_rows = []
    for (instance, algorithm), rs in sorted(groups.items()):
        objectives = [int(r['objective']) for r in rs]
        runtimes = [int(r['runtimeNanos']) / 1_000_000.0 for r in rs]
        iterations = [int(r['iterationCount']) for r in rs if int(r['iterationCount']) >= 0]
        accepted_better = [int(r['acceptedBetterCount']) for r in rs if int(r['acceptedBetterCount']) >= 0]
        accepted_worse = [int(r['acceptedWorseCount']) for r in rs if int(r['acceptedWorseCount']) >= 0]
        rejected_worse = [int(r['rejectedWorseCount']) for r in rs if int(r['rejectedWorseCount']) >= 0]
        best_row = max(rs, key=lambda r: int(r['objective']))
        summary_rows.append({
            'instance': instance,
            'algorithm': algorithm,
            'runs': len(rs),
            'min': min(objectives),
            'avg': f'{mean(objectives):.3f}',
            'max': max(objectives),
            'std': f'{stdev(objectives):.3f}',
            'bestStartVertex': best_row['startVertexId'],
            'avgRuntimeMs': f'{mean(runtimes):.3f}',
            'avgIterations': f'{mean(iterations):.3f}' if iterations else '',
            'avgAcceptedBetter': f'{mean(accepted_better):.3f}' if accepted_better else '',
            'avgAcceptedWorse': f'{mean(accepted_worse):.3f}' if accepted_worse else '',
            'avgRejectedWorse': f'{mean(rejected_worse):.3f}' if rejected_worse else '',
        })

    summary_path = out_dir / 'summary_by_instance.csv'
    with summary_path.open('w', newline='', encoding='utf-8') as f:
        fieldnames = ['instance','algorithm','runs','min','avg','max','std','bestStartVertex','avgRuntimeMs','avgIterations','avgAcceptedBetter','avgAcceptedWorse','avgRejectedWorse']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(summary_rows)

    by_alg = defaultdict(list)
    for row in summary_rows:
        by_alg[row['algorithm']].append(row)
    ranking_rows = []
    for algorithm, rs in by_alg.items():
        lookup = {r['instance']: r for r in rs}
        if 'TSPA' not in lookup or 'TSPB' not in lookup:
            continue
        ranking_rows.append({
            'algorithm': algorithm,
            'sumAvg': f"{float(lookup['TSPA']['avg']) + float(lookup['TSPB']['avg']):.3f}",
            'TSPA_min': lookup['TSPA']['min'],
            'TSPA_avg': lookup['TSPA']['avg'],
            'TSPA_max': lookup['TSPA']['max'],
            'TSPA_std': lookup['TSPA']['std'],
            'TSPA_avgIterations': lookup['TSPA']['avgIterations'],
            'TSPB_min': lookup['TSPB']['min'],
            'TSPB_avg': lookup['TSPB']['avg'],
            'TSPB_max': lookup['TSPB']['max'],
            'TSPB_std': lookup['TSPB']['std'],
            'TSPB_avgIterations': lookup['TSPB']['avgIterations'],
        })
    ranking_rows.sort(key=lambda r: float(r['sumAvg']), reverse=True)

    ranking_path = out_dir / 'ranking_sum_avg.csv'
    with ranking_path.open('w', newline='', encoding='utf-8') as f:
        fieldnames = ['algorithm','sumAvg','TSPA_min','TSPA_avg','TSPA_max','TSPA_std','TSPA_avgIterations','TSPB_min','TSPB_avg','TSPB_max','TSPB_std','TSPB_avgIterations']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(ranking_rows)

    print(f'wrote {summary_path}')
    print(f'wrote {ranking_path}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
