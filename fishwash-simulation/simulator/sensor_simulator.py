import argparse
import json
import math
import random
import signal
import sys
import time
from datetime import datetime

import requests

try:
    from colorama import Fore, Style, init as colorama_init

    colorama_init(autoreset=True)
    HAS_COLORAMA = True
except ImportError:
    HAS_COLORAMA = False


class Color:
    @staticmethod
    def green(text):
        return f"{Fore.GREEN}{text}{Style.RESET_ALL}" if HAS_COLORAMA else text

    @staticmethod
    def red(text):
        return f"{Fore.RED}{text}{Style.RESET_ALL}" if HAS_COLORAMA else text

    @staticmethod
    def yellow(text):
        return f"{Fore.YELLOW}{text}{Style.RESET_ALL}" if HAS_COLORAMA else text

    @staticmethod
    def cyan(text):
        return f"{Fore.CYAN}{text}{Style.RESET_ALL}" if HAS_COLORAMA else text

    @staticmethod
    def blue(text):
        return f"{Fore.BLUE}{text}{Style.RESET_ALL}" if HAS_COLORAMA else text

    @staticmethod
    def magenta(text):
        return f"{Fore.MAGENTA}{text}{Style.RESET_ALL}" if HAS_COLORAMA else text


class DeviceSimulator:
    def __init__(self, device_id, resonance_freq, base_freq, drift_probability):
        self.device_id = device_id
        self.resonance_freq = resonance_freq
        self.current_freq = base_freq
        self.base_freq = base_freq
        self.drift_probability = drift_probability
        self.water_temp = 20.0 + random.uniform(-1, 1)
        self.step_count = 0
        self.total_amplitude = 0.0
        self.total_spray_height = 0.0
        self.max_amplitude = 0.0
        self.max_spray_height = 0.0
        self.success_count = 0
        self.failure_count = 0

    def generate_data(self):
        self.step_count += 1

        if random.random() < self.drift_probability:
            self.current_freq += random.uniform(-10, 10)
        else:
            self.current_freq += random.uniform(-2, 2)

        self.current_freq = max(self.base_freq * 0.5, min(self.base_freq * 1.5, self.current_freq))

        amplitude = self._calc_amplitude(self.current_freq)
        spray_height = self._calc_spray_height(amplitude)
        self.water_temp += random.uniform(-0.1, 0.1)
        self.water_temp = max(15.0, min(35.0, self.water_temp))

        self.total_amplitude += amplitude
        self.total_spray_height += spray_height
        self.max_amplitude = max(self.max_amplitude, amplitude)
        self.max_spray_height = max(self.max_spray_height, spray_height)

        return {
            "frictionFreq": round(self.current_freq, 2),
            "amplitude": round(amplitude, 2),
            "sprayHeight": round(spray_height, 2),
            "waterTemp": round(self.water_temp, 1),
            "recordedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }

    def _calc_amplitude(self, freq):
        fr = self.resonance_freq
        zeta = 0.01
        a_max = 2.0
        ratio = freq / fr
        denom = math.sqrt((1 - ratio ** 2) ** 2 + (2 * zeta * ratio) ** 2)
        if denom < 1e-9:
            denom = 1e-9
        amplitude = a_max / denom
        amplitude = max(0.1, min(5.0, amplitude))
        return amplitude

    def _calc_spray_height(self, amplitude):
        k = 0.5
        random_factor = random.uniform(0.8, 1.2)
        height = k * amplitude ** 2 * random_factor
        height += random.uniform(-0.5, 0.5)
        height = max(0.0, min(50.0, height))
        return height

    @property
    def avg_amplitude(self):
        return self.total_amplitude / self.step_count if self.step_count > 0 else 0.0

    @property
    def avg_spray_height(self):
        return self.total_spray_height / self.step_count if self.step_count > 0 else 0.0


def post_with_retry(url, data, max_retries=3):
    for attempt in range(max_retries):
        try:
            resp = requests.post(url, json=data, timeout=10)
            resp.raise_for_status()
            return True, resp.status_code
        except requests.RequestException as e:
            if attempt < max_retries - 1:
                wait_time = 2 ** attempt
                time.sleep(wait_time)
            else:
                return False, str(e)
    return False, "Max retries exceeded"


def build_api_url(base_url, device_id):
    return f"{base_url.rstrip('/')}/api/sensor-data/{device_id}"


def format_table_row(device_id, data):
    return (
        f"  Device {device_id:>2} | "
        f"Freq: {data['frictionFreq']:>7.2f} Hz | "
        f"Amp: {data['amplitude']:>5.2f} mm | "
        f"Spray: {data['sprayHeight']:>6.2f} cm | "
        f"Temp: {data['waterTemp']:>5.1f} °C | "
        f"Time: {data['recordedAt']}"
    )


def print_stats(devices):
    print(Color.cyan("=" * 80))
    print(Color.cyan(" Cumulative Statistics"))
    print(Color.cyan("=" * 80))
    for dev in devices:
        print(f"  Device {dev.device_id}:")
        print(f"    Steps: {dev.step_count}  |  Success: {Color.green(str(dev.success_count))}  |  Failures: {Color.red(str(dev.failure_count))}")
        print(f"    Avg Amplitude: {dev.avg_amplitude:.2f} mm  |  Max Amplitude: {dev.max_amplitude:.2f} mm")
        print(f"    Avg Spray Height: {dev.avg_spray_height:.2f} cm  |  Max Spray Height: {dev.max_spray_height:.2f} cm")
        print(f"    Current Water Temp: {dev.water_temp:.1f} °C")
    print()


def main():
    parser = argparse.ArgumentParser(description="Fish Wash Sensor Simulator")
    parser.add_argument("--api-url", default="http://localhost:8080", help="API base URL")
    parser.add_argument("--device-ids", nargs="+", type=int, default=[1, 2], help="Device IDs to simulate")
    parser.add_argument("--interval", type=int, default=60, help="Reporting interval in seconds")
    parser.add_argument("--resonance-freq", type=float, default=None, help="Override resonance frequency for all devices")
    parser.add_argument("--drift-probability", type=float, default=0.1, help="Probability of frequency drift per step")
    parser.add_argument("--verbose", action="store_true", help="Enable verbose logging")
    args = parser.parse_args()

    device_configs = {
        1: {"resonance_freq": 285.6, "base_freq": 280.0},
        2: {"resonance_freq": 243.8, "base_freq": 220.0},
    }

    devices = []
    for did in args.device_ids:
        cfg = device_configs.get(did, {"resonance_freq": 250.0, "base_freq": 250.0})
        res_freq = args.resonance_freq if args.resonance_freq is not None else cfg["resonance_freq"]
        base_freq = cfg["base_freq"]
        if args.resonance_freq is not None:
            base_freq = args.resonance_freq
        dev = DeviceSimulator(did, res_freq, base_freq, args.drift_probability)
        devices.append(dev)

    running = True

    def signal_handler(sig, frame):
        nonlocal running
        print(Color.yellow("\n\nShutting down gracefully..."))
        running = False

    signal.signal(signal.SIGINT, signal_handler)

    print(Color.magenta("=" * 80))
    print(Color.magenta("  Fish Wash (鱼洗铜盆) Sensor Simulator"))
    print(Color.magenta("=" * 80))
    print(f"  API URL: {Color.blue(args.api_url)}")
    print(f"  Devices: {Color.blue(str(args.device_ids))}")
    print(f"  Interval: {Color.blue(str(args.interval) + 's')}")
    print(f"  Drift Probability: {Color.blue(str(args.drift_probability))}")
    for dev in devices:
        print(f"  Device {dev.device_id}: resonance={dev.resonance_freq} Hz, base_freq={dev.base_freq} Hz")
    print(Color.magenta("=" * 80))
    print(Color.yellow("  Press Ctrl+C to stop\n"))

    while running:
        for dev in devices:
            data = dev.generate_data()
            url = build_api_url(args.api_url, dev.device_id)

            if args.verbose:
                print(Color.yellow(f"[VERBOSE] POST {url}"))
                print(Color.yellow(f"[VERBOSE] Body: {json.dumps(data)}"))

            success, result = post_with_retry(url, data)

            if success:
                dev.success_count += 1
                row = format_table_row(dev.device_id, data)
                print(Color.green(row))
            else:
                dev.failure_count += 1
                row = format_table_row(dev.device_id, data)
                print(Color.red(f"{row}  | FAILED: {result}"))

        if devices and devices[0].step_count % 10 == 0:
            print()
            print_stats(devices)

        if not running:
            break

        for _ in range(args.interval):
            if not running:
                break
            time.sleep(1)

    print()
    print(Color.magenta("=" * 80))
    print(Color.magenta("  Shutdown Summary"))
    print(Color.magenta("=" * 80))
    print_stats(devices)
    print(Color.yellow("Simulator stopped."))


if __name__ == "__main__":
    main()
