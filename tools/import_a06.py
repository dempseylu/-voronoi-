#!/usr/bin/env python3
"""Convert the official A06 data set into the two CSV files this project reads.

The source is the 2020 (11th) China Students Service Outsourcing Innovation and
Entrepreneurship Competition problem A06 "交通时空大数据分析挖掘系统" sponsored by
Neusoft. The problem statement says the sponsor provides anonymised signalling
data plus the static data each module needs, and ``数据说明.docx`` lists the
cleaning rules applied here.

Usage:
    python3 tools/import_a06.py                # writes data/a06/stations.csv, users.csv
    python3 tools/import_a06.py --all-dates    # keep every day instead of 2018-10-03
    python3 tools/import_a06.py --help

Rules implemented (in the order given by the data description):
    1. keep only timestamp, imsi, lac_id, cell_id
    2. drop rows whose imsi contains '#', '*' or '^'
    3. drop rows with an empty imsi / lac_id / cell_id
    4. convert the epoch-millisecond timestamp to yyyyMMddHHmmss (UTC+8)
    5. keep only the records of 2018-10-03
    6. drop rows that cannot be joined to a station coordinate
    7. sort by imsi, then by time ascending

Station coordinates are additionally de-duplicated: the raw table contains many
sectors that share one physical position, and duplicate seeds would only slow
the triangulation down.
"""

import argparse
import csv
import datetime
import os
import sys
from collections import OrderedDict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_RAW = os.path.join(PROJECT_ROOT, "data", "a06", "raw")
DEFAULT_OUT = os.path.join(PROJECT_ROOT, "data", "a06")
CHINA_TZ = datetime.timezone(datetime.timedelta(hours=8))
TARGET_DATE = "2018-10-03"
SPECIAL_CHARS = ("#", "*", "^")


def open_text(path):
    """Open a CSV that may carry a byte-order mark."""
    return open(path, "r", encoding="utf-8-sig", newline="")


def find_file(folder, keyword):
    for name in sorted(os.listdir(folder)):
        if keyword in name and name.lower().endswith(".csv"):
            return os.path.join(folder, name)
    raise SystemExit("Cannot find a CSV containing '%s' in %s" % (keyword, folder))


def read_stations(path):
    """Returns (ordered {laci: (lon, lat)}, rows read)."""
    coordinates = OrderedDict()
    with open_text(path) as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            lacid = (row.get("laci") or "").strip()
            if not lacid:
                continue
            try:
                longitude = float(row["longitude"])
                latitude = float(row["latitude"])
            except (TypeError, ValueError):
                continue
            coordinates[lacid] = (longitude, latitude)
    return coordinates


def format_timestamp(epoch_millis):
    moment = datetime.datetime.fromtimestamp(epoch_millis / 1000.0, CHINA_TZ)
    return moment.strftime("%Y%m%d%H%M%S")


def import_data(raw_folder, out_folder, target_date):
    station_path = find_file(raw_folder, "基站")
    signalling_path = find_file(raw_folder, "原始数据")

    coordinates = read_stations(station_path)
    report = {
        "stations_read": len(coordinates),
        "signalling_read": 0,
        "after_special_chars": 0,
        "after_empty_fields": 0,
        "after_date_filter": 0,
        "after_join": 0,
    }

    records = []
    with open_text(signalling_path) as handle:
        for row in csv.DictReader(handle):
            report["signalling_read"] += 1

            # Rule 1 + 2: only four columns matter, and the IMSI must be clean.
            imsi = (row.get("imsi") or "").strip()
            lac_id = (row.get("lac_id") or "").strip()
            cell_id = (row.get("cell_id") or "").strip()

            if any(character in imsi for character in SPECIAL_CHARS):
                continue
            # Rule 3: every key field must be present.
            if not imsi or not lac_id or not cell_id or not imsi.isdigit():
                continue
            report["after_special_chars"] += 1

            # Rule 4: epoch milliseconds to yyyyMMddHHmmss in China Standard Time.
            try:
                epoch_millis = int(row["timestamp"])
                timestamp = format_timestamp(epoch_millis)
            except (TypeError, ValueError):
                continue
            report["after_empty_fields"] += 1

            # Rule 5: the working day of the data set.
            if target_date and not timestamp.startswith(target_date.replace("-", "")):
                continue
            report["after_date_filter"] += 1

            # Rule 6: the sector must resolve to a coordinate.
            position = coordinates.get("%s-%s" % (lac_id, cell_id))
            if position is None:
                continue
            report["after_join"] += 1

            records.append((imsi, timestamp, position[0], position[1], lac_id))

    # Rule 7: one person, chronological order.
    records.sort(key=lambda item: (item[0], item[1]))

    os.makedirs(out_folder, exist_ok=True)
    station_out = os.path.join(out_folder, "stations.csv")
    user_out = os.path.join(out_folder, "users.csv")

    # De-duplicate station positions; keep the first sector id as the name.
    seen = OrderedDict()
    for lacid, position in coordinates.items():
        seen.setdefault(position, lacid)

    with open(station_out, "w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["id", "name", "longitude", "latitude"])
        for index, (position, lacid) in enumerate(seen.items(), 1):
            writer.writerow([index, lacid, "%.7f" % position[0], "%.7f" % position[1]])

    with open(user_out, "w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["time", "imsi", "longitude", "latitude", "lac"])
        for imsi, timestamp, longitude, latitude, lac_id in records:
            writer.writerow([timestamp, imsi, "%.7f" % longitude, "%.7f" % latitude, lac_id])

    report["stations_written"] = len(seen)
    report["duplicate_stations_removed"] = len(coordinates) - len(seen)
    report["users_written"] = len(records)
    report["dates"] = target_date or "all"
    return report, station_out, user_out


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--raw", default=DEFAULT_RAW, help="folder with the official CSVs")
    parser.add_argument("--out", default=DEFAULT_OUT, help="folder for stations.csv and users.csv")
    parser.add_argument("--all-dates", action="store_true", help="keep every day, not just 2018-10-03")
    args = parser.parse_args(argv)

    if not os.path.isdir(args.raw):
        raise SystemExit("Raw data folder not found: %s" % args.raw)

    report, station_out, user_out = import_data(
        args.raw, args.out, None if args.all_dates else TARGET_DATE)

    print("A06 import report")
    print("  signalling rows read      : %d" % report["signalling_read"])
    print("  after IMSI special chars  : %d" % report["after_special_chars"])
    print("  after empty-field check   : %d" % report["after_empty_fields"])
    print("  after date filter (%s) : %d" % (report["dates"], report["after_date_filter"]))
    print("  after station join        : %d" % report["after_join"])
    print("  stations read / written   : %d / %d (duplicates removed: %d)"
          % (report["stations_read"], report["stations_written"],
             report["duplicate_stations_removed"]))
    print("  users written             : %d" % report["users_written"])
    print("  stations -> %s" % station_out)
    print("  users    -> %s" % user_out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
