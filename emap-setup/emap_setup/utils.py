
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
