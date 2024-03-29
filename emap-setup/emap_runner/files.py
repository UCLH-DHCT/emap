from pathlib import Path
from typing import Optional, List

from emap_runner.log import logger
from emap_runner.utils import EMAPRunnerException


class NewLine(str):
    """A new line in a file"""


class CommentLine(str):
    """Line in a file that is a comment"""


class File:
    def __init__(self, filename: Path, allow_empty: bool = False):
        """Construct a file that may, or may not be empty"""

        self.path = filename

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

        with open(self.path, "w") as file:
            file.write("".join(self.lines))

        return None

    def set_comment_line_at(self, line: str, idx: int) -> None:
        self.lines[idx] = CommentLine(line)

    def set_new_line_at(self, line: str, idx: int) -> None:
        self.lines[idx] = NewLine(line)


class EnvironmentFile(File):
    def __init__(self, filename: Path):
        super().__init__(filename, allow_empty=True)

    @classmethod
    def from_example_file(cls, example_filename: Path) -> "EnvironmentFile":

        if ".EXAMPLE" not in str(example_filename):
            raise EMAPRunnerException(
                f"Cannot create an environment file. {example_filename} "
                f"did not have a .EXAMPLE containing extension"
            )

        file = EnvironmentFile(example_filename.with_suffix(""))

        with open(example_filename, "r") as example_file:
            file.lines = example_file.readlines()

        return file

    @property
    def unchanged_lines(self) -> List[str]:
        """List of unchanged lines in a file"""
        return [
            str(line)
            for line in self.lines
            if not (isinstance(line, NewLine) or isinstance(line, CommentLine))
        ]

    def replace_value_of(self, key: str, value: str) -> None:
        """Replace a value given a string key"""

        if not value.endswith("\n"):
            value += "\n"

        for i, line in enumerate(self.lines):
            if line.startswith(key):
                self.lines[i] = f"{key}={value}"

        return None

    def write(self, directory: Optional[Path] = None) -> None:

        if directory is not None:
            self.path = Path(directory, self.path.name)

        return super().write()

    @property
    def basename(self) -> str:
        """
        Base name of this environment file .e.g
            /some/dir/rabbitmq-config-envs -> rabbitmq
        """
        return str(self.path.name).replace("-config-envs", "")

    @property
    def environment_variables(self) -> dict:
        """Construct a dictionary of environment variables stored in a file in
        the format::

            key0=value0
            key1=value1
            ...
        """

        env_vars = {}

        for line in self.lines:

            if line.startswith("#"):
                continue  # skip comment lines

            try:
                key = line.split("=")[0]
                value = "=".join(line.split("=")[1:])
                env_vars[key] = value.strip()

            except (ValueError, IndexError):
                logger.error(
                    f"{line} was not a valid env variable pair in: {self.path}"
                )

        return env_vars
