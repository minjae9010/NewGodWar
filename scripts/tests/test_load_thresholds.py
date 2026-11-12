import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("load_runner", Path(__file__).parents[1] / "Test-Load.py")
load = importlib.util.module_from_spec(spec)
spec.loader.exec_module(load)


class LoadThresholdTests(unittest.TestCase):
    def phase(self, **updates):
        phase = dict(phase="32-normal", tps="20", tick_ms_p95="10", tick_ms_p99="15", tick_ms_max="40",
                     ticks="1200", ticks_over_50ms="0", rtt_p95_ms=12, rtt_max_ms=30)
        phase.update(updates)
        return phase

    def test_good_percentiles_do_not_hide_a_single_long_stall(self):
        failures = load.performance_failures([self.phase(tick_ms_max="319.4833")])
        self.assertEqual([failure["metric"] for failure in failures], ["tick_ms_max"])

    def test_sustained_slow_ticks_and_command_stalls_fail(self):
        failures = load.performance_failures([self.phase(ticks_over_50ms="13", rtt_max_ms=274.2)])
        self.assertEqual({failure["metric"] for failure in failures}, {"slow_tick_ratio", "rtt_max_ms"})

    def test_missing_nan_or_empty_measurements_cannot_pass(self):
        for phases in ([], [dict(phase="idle")], [self.phase(ticks="0")],
                       [self.phase(tick_ms_p99="nan")], [self.phase(rtt_max_ms=None)]):
            with self.subTest(phases=phases):
                self.assertTrue(load.performance_failures(phases))

    def test_healthy_measurements_and_documented_boundaries_pass(self):
        self.assertEqual(load.performance_failures([dict(phase="idle"), self.phase()]), [])
        self.assertEqual(load.performance_failures([self.phase(tps="19", tick_ms_max="250", ticks_over_50ms="12")]), [])


if __name__ == "__main__":
    unittest.main()
