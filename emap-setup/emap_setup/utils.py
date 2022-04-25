import os

from typing import Optional, List
from datetime import date


class NewLine(str):
    """A new line in a file"""


class CommentLine(str):
    """Line in a file that is a comment"""


class File:

    def __init__(self, filename):

        self.filename = filename

        with open(filename, 'r') as file:
            self.lines = file.readlines()

    def replace(self, old: str, new: str) -> None:
        """Replace a string with another that may or may not exist"""

        for i, line in enumerate(self.lines):
            if str(old) in line:
                self.lines[i] = line.replace(str(old), str(new))

        return None

    def write(self) -> None:
        """Write the file lines to a new version of the file"""

        with open(self.filename, 'w') as file:
            file.write(''.join(self.lines))

        return None


class EnvironmentFile(File):

    def __init__(self, example_filename: str):
        super().__init__(example_filename)

        if '.EX' not in example_filename:
            exit(f'Cannot create an environment file. {example_filename} '
                 f'did not have a .EX containing extension')

        with open(example_filename, 'r') as file:
            self.lines = file.readlines()

        self.filename = example_filename.split('.EX')[0]

    @property
    def unchanged_lines(self) -> List[str]:
        """List of unchanged lines in a file"""
        return [str(l) for l in self.lines
                if not (isinstance(l, NewLine) or isinstance(l, CommentLine))]

    def replace_value_of(self, key: str, value: str) -> None:
        """Replace a value given a string key"""

        for i, line in enumerate(self.lines):
            if line.startswith(key):
                self.lines[i] = f'{key}={value}'

        return None

    def write(self, directory: Optional[str] = None) -> None:

        if directory is not None:
            self.filename = os.path.join(directory, os.path.basename(self.filename))

        return super().write()


class TimeWindow:

    def __init__(self,
                 start_date:    str,
                 end_date:      str):
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

        if string == 'today':
            return today

        if string.endswith(' days ago'):
            n = today.toordinal() - int(string.split(' days ago')[0])
            return date.fromordinal(n)

        try:
            return date.fromisoformat(string)
        except ValueError:
            raise NotImplementedError(f'Failed to parse {string}')

    @property
    def start_stamp(self) -> str:
        return f'{self.start}T00:00:00.00Z'

    @property
    def end_stamp(self) -> str:
        return f'{self.end}T00:00:00.00Z'
