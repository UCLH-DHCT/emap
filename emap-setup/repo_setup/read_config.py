import yaml
from openpyxl.compat import file


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
        self.docs = None
        with open(filename) as f:
            doc = yaml.load_all(f, Loader=yaml.FullLoader)
            self.docs = list(doc)

    def populate_repo_info(self):
        """
        Return a list of dictionary items with repository name and branch
        :return: list of repo dictionary items
        """
        repo_info = []
        # find repositories in docs
        index = self._get_index('repositories')
        if index == -1:
            return repo_info
        for entry in self.docs[index]['repositories']:
            repo_info.append(entry)
        return repo_info

    def print_content(self):
        """ here for development"""
        for doc in self.docs:
            for k, v in doc.items():
                print(k, '->', v)

    def _get_index(self, name_of_list):
        """
        Return the index in docs for the relevant section
        :param name_of_list: section name
        :return: index of section or -1 if not found
        """
        for i in range(0, len(self.docs)):
            if name_of_list in self.docs[i]:
                return i
        return -1;

