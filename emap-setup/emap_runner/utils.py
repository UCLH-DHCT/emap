from datetime import date


class EMAPRunnerException(Exception):
    """Base exception for any error in EMAP setup"""


class TimeWindow:
    def __init__(self, start_date: str, end_date: str):
        """Time window defined by a string"""

        self.start = self._parse_date_string(start_date)
        self.end = self._parse_date_string(end_date)

    @staticmethod
    def _parse_date_string(string: str) -> date:
        """
        Convert a string referring to a date that may be relative to today
        into a Python datetime.date object
        """
        today = date.today()

        if string == "today":
            return today

        if string.endswith(" days ago"):
            n = today.toordinal() - int(string.split(" days ago")[0])
            return date.fromordinal(n)

        try:
            return date.fromisoformat(string)
        except ValueError as e:
            raise EMAPRunnerException(
                f"Failed to parse {string} as a date " f"because:\n{e}"
            ) from e

    @property
    def start_stamp(self) -> str:
        return f"{self.start}T00:00:00.00Z"

    @property
    def end_stamp(self) -> str:
        return f"{self.end}T00:00:00.00Z"
