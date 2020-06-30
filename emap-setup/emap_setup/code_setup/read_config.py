import yaml


class ReadConfig:
    """
    Read the global-configurations.txt file
    and extract required data
    """

    def __init__(self, filename: str) -> None:
        """
        Read the file and create sets representing different data
        :param filename:
        """
        self.config_options = None
        with open(filename) as f:
            self.config_options = yaml.load(f, Loader=yaml.FullLoader)

    def get_repo_info(self) -> dict:
        """
        Return a list of dictionary items with repository name and branch
        :return: list of repo dictionary items
        """
        repos = {}
        for repo in self.config_options['config']['repositories']:
            if 'repo_name' in \
                    self.config_options['config']['repositories'][repo]:
                repo_name = self.config_options['config']['repositories'][repo]['repo_name']
            else:
                repo_name = repo
            this_repo = {'name': repo_name,
                         'branch': self.config_options['config']['repositories'][repo]['branch']}
            repos[repo] = this_repo
        return repos

    def get_git_dir(self) -> str:
        """
        Return the main git directory specified
        """
        return self.config_options['config']['main_git_dir']

    def get_data_for(self, dataname: str) -> dict:
        """
        Retrun information for dataname specified.
        :param dataname: str name of data requested
        :return: dictionary of data for dataname
        """
        if dataname in self.config_options['config']:
            return self.config_options['config'][dataname]
        return None
