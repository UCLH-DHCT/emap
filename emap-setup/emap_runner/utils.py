from datetime import date


class EMAPRunnerException(Exception):
    """Base exception for any error in EMAP setup"""


class TimeWindow:
    def __init__(self, start_date: str, end_date: str):
        """Time window constructed from strings of the start and end dates"""

        self.start = self._parse_date_string(start_date)
        self.end = self._parse_date_string(end_date)

    @staticmethod
    def _parse_date_string(string: str) -> date:
        """
        Convert a string referring to a date that may be relative to today
        into a Python datetime.date object
        """
        if string.endswith("_default"):
            _date_type = DefaultDate
            string = string.replace("_default", "")

        else:
            _date_type = Date

        today = _date_type.today()

        if string == "today":
            return today

        if string.endswith(" days ago"):  # e.g. 7 days ago
            string_n = string.split(" days ago")[0]

            if string_n == "":
                raise EMAPRunnerException(f"Invalid date string: {string}")

            return _date_type.fromordinal(today.toordinal() - int(string_n))

        try:
            return _date_type.fromisoformat(string)
        except ValueError as e:
            raise EMAPRunnerException(f"Failed to parse {string} as a date") from e

    @property
    def start_stamp(self) -> str:
        return f"{self.start}T00:00:00.00Z"

    @property
    def end_stamp(self) -> str:
        return f"{self.end}T00:00:00.00Z"


class Date(date):

    is_default = False


class DefaultDate(Date):

    is_default = True
