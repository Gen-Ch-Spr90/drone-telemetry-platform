#!/usr/bin/env python3
"""
Synthetic DJI Mavic 2 Pro flight generator.
Produces realistic telemetry CSV for testing the ingestion pipeline.

Design notes:
- Mavic 2 Pro: ~30 min max flight, 400ft FAA ceiling, ~15 m/s max speed
- Models: takeoff, climb, waypoint transit, hover, return-to-home, land
- Adds GPS jitter, battery drain, occasional signal drop
"""
import csv
import math
import random
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import List, Tuple

FAA_MAX_ALT_M = 121.9          # 400 ft
MAX_SPEED_MPS = 15.0
HOME_LAT = 32.7767             # Dallas
HOME_LON = -96.7970


@dataclass
class Sample:
    recorded_at: datetime
    lat: float
    lon: float
    altitude_m: float
    speed_mps: float
    heading_deg: float
    battery_pct: float
    satellites: int
    signal_dbm: float
    gimbal_pitch: float
    gimbal_yaw: float


@dataclass
class Flight:
    flight_id: str
    started_at: datetime
    samples: List[Sample] = field(default_factory=list)


def haversine_m(lat1, lon1, lat2, lon2) -> float:
    R = 6371000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * R * math.asin(math.sqrt(a))


def bearing_deg(lat1, lon1, lat2, lon2) -> float:
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dl = math.radians(lon2 - lon1)
    x = math.sin(dl) * math.cos(p2)
    y = math.cos(p1) * math.sin(p2) - math.sin(p1) * math.cos(p2) * math.cos(dl)
    return (math.degrees(math.atan2(x, y)) + 360) % 360


def move_toward(lat, lon, target_lat, target_lon, distance_m):
    """Move `distance_m` toward target. Returns new (lat, lon)."""
    total = haversine_m(lat, lon, target_lat, target_lon)
    if total < 1e-6:
        return target_lat, target_lon
    frac = min(distance_m / total, 1.0)
    return lat + (target_lat - lat) * frac, lon + (target_lon - lon) * frac


def generate_flight(seed: int = 42, duration_s: int = 1200) -> Flight:
    random.seed(seed)
    flight = Flight(
        flight_id=str(uuid.uuid4()),
        started_at=datetime.now(timezone.utc) - timedelta(minutes=30),
    )

    # Build 3 random waypoints within ~500m of home
    waypoints: List[Tuple[float, float]] = []
    for _ in range(3):
        d_lat = random.uniform(-0.0045, 0.0045)
        d_lon = random.uniform(-0.0045, 0.0045)
        waypoints.append((HOME_LAT + d_lat, HOME_LON + d_lon))

    lat, lon = HOME_LAT, HOME_LON
    alt = 0.0
    battery = 100.0
    t = flight.started_at
    route = waypoints + [(HOME_LAT, HOME_LON)]  # return home at end

    for wp_idx, (tgt_lat, tgt_lon) in enumerate(route):
        # transit phase
        while haversine_m(lat, lon, tgt_lat, tgt_lon) > 3.0:
            speed = random.uniform(6.0, 12.0)
            lat, lon = move_toward(lat, lon, tgt_lat, tgt_lon, speed)
            target_alt = random.uniform(40.0, 110.0) if wp_idx < len(route) - 1 else 0.0
            alt += max(-3.0, min(3.0, target_alt - alt))
            alt = max(0.0, min(FAA_MAX_ALT_M, alt))
            battery = max(5.0, battery - random.uniform(0.04, 0.09))
            t += timedelta(seconds=1)

            gps_jitter = random.uniform(-1e-5, 1e-5)
            flight.samples.append(Sample(
                recorded_at=t,
                lat=lat + gps_jitter,
                lon=lon + gps_jitter,
                altitude_m=round(alt, 2),
                speed_mps=round(speed, 2),
                heading_deg=round(bearing_deg(lat, lon, tgt_lat, tgt_lon), 1),
                battery_pct=round(battery, 2),
                satellites=random.randint(11, 19),
                signal_dbm=round(random.uniform(-85, -55), 1),
                gimbal_pitch=round(random.uniform(-30, 0), 1),
                gimbal_yaw=round(random.uniform(-45, 45), 1),
            ))

        # hover at waypoint (except home)
        if wp_idx < len(route) - 1:
            for _ in range(random.randint(5, 15)):
                t += timedelta(seconds=1)
                battery = max(5.0, battery - random.uniform(0.03, 0.07))
                flight.samples.append(Sample(
                    recorded_at=t,
                    lat=lat + random.uniform(-1e-5, 1e-5),
                    lon=lon + random.uniform(-1e-5, 1e-5),
                    altitude_m=round(alt, 2),
                    speed_mps=round(random.uniform(0, 0.5), 2),
                    heading_deg=round(random.uniform(0, 360), 1),
                    battery_pct=round(battery, 2),
                    satellites=random.randint(11, 19),
                    signal_dbm=round(random.uniform(-85, -55), 1),
                    gimbal_pitch=round(random.uniform(-45, -10), 1),
                    gimbal_yaw=round(random.uniform(-90, 90), 1),
                ))

    return flight


def write_csv(flight: Flight, path: str) -> None:
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow([
            "flight_id", "recorded_at", "lat", "lon", "altitude_m",
            "speed_mps", "heading_deg", "battery_pct", "satellites",
            "signal_strength_dbm", "gimbal_pitch_deg", "gimbal_yaw_deg",
        ])
        for s in flight.samples:
            w.writerow([
                flight.flight_id, s.recorded_at.isoformat(), f"{s.lat:.7f}",
                f"{s.lon:.7f}", s.altitude_m, s.speed_mps, s.heading_deg,
                s.battery_pct, s.satellites, s.signal_dbm,
                s.gimbal_pitch, s.gimbal_yaw,
            ])


if __name__ == "__main__":
    flight = generate_flight()
    out = f"samples/flight_{flight.flight_id[:8]}.csv"
    write_csv(flight, out)
    print(f"Wrote {len(flight.samples)} samples to {out}")