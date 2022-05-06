import pytest

from datetime import date
from emap_runner.utils import TimeWindow, EMAPRunnerException


def test_both_today():
    """Ensure that today is a supported string in defining a time window"""

    window = TimeWindow("today", "today")

    assert window.start == date.today()
    assert window.end == date.today()


def test_today_and_n_days_ago():
    """Ensure that N days ago is a valid date"""

    window = TimeWindow("4 days ago", "today")

    assert (window.end.toordinal() - window.start.toordinal()) == 4


def test_iso_format():
    """Ensure that a date can be specified directly in YYYY-MM-DD format"""

    window = TimeWindow("2019-12-04", "2019-12-08")

    assert (window.end.toordinal() - window.start.toordinal()) == 4


def test_invalid_time_format():
    """Ensure that an invalid time format raises an exception"""

    with pytest.raises(EMAPRunnerException):
        _ = TimeWindow("something_invalid", "today")
