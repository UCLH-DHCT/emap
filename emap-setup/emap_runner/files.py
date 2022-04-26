import os
from typing import Optional, List

from emap_runner.utils import EMAPRunnerException


class NewLine(str):
    """A new line in a file"""


class CommentLine(str):
    """Line in a file that is a comment"""


class File:
    def __init__(self, filename: str, allow_empty: bool = False):

        self.filename = filename

        try:
            with open(filename, "r") as file:
                self.lines = file.readlines()

        except IOError as e:
            if not allow_empty:
                raise e

    def replace(self, old: str, new: str) -> None:
        """Replace a string with another that may or may not exist"""

        for i, line in enumerate(self.lines):
            if str(old) in line:
                self.lines[i] = line.replace(str(old), str(new))

        return None

    def write(self) -> None:
        """Write the file lines to a new version of the file"""

        with open(self.filename, "w") as file:
            file.write("".join(self.lines))

        return None


class EnvironmentFile(File):
    def __init__(self, filename: str):
        super().__init__(filename, allow_empty=True)

    @classmethod
    def from_example_file(cls, example_filename: str) -> "EnvironmentFile":

        if ".EXAMPLE" not in example_filename:
            raise EMAPRunnerException(
                f"Cannot create an environment file. {example_filename} "
                f"did not have a .EXAMPLE containing extension"
            )

        file = EnvironmentFile(example_filename.split(".EXAMPLE")[0])

        with open(example_filename, "r") as example_file:
            file.lines = example_file.readlines()

        return file

    @property
    def unchanged_lines(self) -> List[str]:
        """List of unchanged lines in a file"""
        return [
            str(l)
            for l in self.lines
            if not (isinstance(l, NewLine) or isinstance(l, CommentLine))
        ]

    def replace_value_of(self, key: str, value: str) -> None:
        """Replace a value given a string key"""

        for i, line in enumerate(self.lines):
            if line.startswith(key):
                self.lines[i] = f"{key}={value}"

        return None

    def write(self, directory: Optional[str] = None) -> None:

        if directory is not None:
            self.filename = os.path.join(directory, os.path.basename(self.filename))

        return super().write()
