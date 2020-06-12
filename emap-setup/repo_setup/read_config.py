import yaml


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
        self.config_options = None
        with open(filename) as f:
            self.config_options = yaml.load(f, Loader=yaml.FullLoader)
        self.print_content()

    def get_repo_info(self):
        """
        Return a list of dictionary items with repository name and branch
        :return: list of repo dictionary items
        """
        repos = []
        for repo in self.config_options['config']['repositories']:
            if 'repo_name' in self.config_options['config']['repositories'][repo]:
                repo_name = self.config_options['config']['repositories'][repo]['repo_name']
            else:
                repo_name = repo
            repos.append({'name': repo_name,
                          'dirname': repo,
                          'branch': self.config_options['config']['repositories'][repo]['branch']})
        return repos

    def get_git_dir(self):
        """
        Return the main git directory specified
        :return:
        """
        return self.config_options['config']['main_git_dir']

    def print_content(self):
        """ here for development"""
        print(self.config_options)
