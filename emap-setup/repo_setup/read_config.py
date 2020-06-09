

def _is_dividing_line(line):
    """
    Check whether the line is a dividing line
    :param line: the line to check
    :return: True if dividing line else False
    """
    if line == '###############################################################################\n':
        return True
    return False


def _is_section_heading(line):
    """
    Check whether the line is a Section heading i.e. begins '##Section='
    :param line: line to be checked
    :return: True if section heading else False
    """
    if line.startswith('##Section='):
        return True
    return False


class ReadConfig:
    """
    Read the global-configurations.txt file
    and extract required data
    """

    def __init__(self, filename):
        """
        Read the file and create sets of the lines representing different data

        :param filename:
        """
        f = open(filename, 'r')
        self.config_lines = f.readlines()
        f.close()

        self.total = len(self.config_lines)

        self.repo_lines = []
        self.other_lines = []

        self._split_sections()

    def populate_repo_info(self):
        """
        Return a list of dictionary items with repository name and branch
        :return: list of repo dictionary items
        """
        repo_info = []
        for i in range(0, len(self.repo_lines)-1):
            line = self.repo_lines[i]
            nextline = self.repo_lines[i+1]
            if line == '\n' or nextline == '\n':
                continue
            else:
                # should only both have text when one is name=
                # and other is branch=
                name = line.split('=')
                branch = nextline.split('=')
                if len(name) < 2 or len(branch) < 2:
                    print('Problem with rep info')
                    return []
                else:
                    repo_info.append({'name': name[1][0:-1], 'branch': branch[1][0:-1]})
        return repo_info


    def print_content(self):
        """ here for development"""
        print('repo')
        for line in self.repo_lines:
            print(line)
        print('other')
        for line in self.other_lines:
            print(line)

    def _split_sections(self):
        """
        Split the configuration file into sections
        """
        for i in range(0, self.total):
            if (i < self.total - 1 and
                    _is_dividing_line(self.config_lines[i]) and
                    _is_section_heading(self.config_lines[i + 1])):
                i = self._read_section(i + 1)

    def _read_section(self, start_index):
        """
        Read a particular section of configuration doc
        :param start_index: index of section header in config_lines
        :return: index at end of reading section
        """
        header = self.config_lines[start_index].split('=')
        i = start_index + 1
        if len(header) != 2:
            return start_index
        if header[1].startswith('Repositories'):
            while i < self.total - 1 and not _is_dividing_line(self.config_lines[i]):
                self.repo_lines.append(self.config_lines[i])
                i = i + 1
        elif header[1].startswith('other'):
            while i < self.total - 1 and not _is_dividing_line(self.config_lines[i]):
                self.other_lines.append(self.config_lines[i])
                i = i + 1
        return i + 1
