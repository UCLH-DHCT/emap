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
            config_options = yaml.load(f, Loader=yaml.FullLoader)
        self.config = config_options['config']
        self.propagate_common_values()

    def propagate_common_values(self) -> None:
        """
        Take common data and make entries for each configuration that
        may use them.
        """
        inform_common = self.config['informdb']['common']
        # put the common informdb entry into every set of informdb entries
        for entry in self.config['informdb']:
            if entry == 'common':
                continue
            for key in inform_common:
                self.config['informdb'][entry][key] = \
                    inform_common.get(key, ' ')
        # sort out entries for dates
        start = self.config['dates']['start']
        if not start:
            start = ' '
        finish = self.config['dates']['end']
        if not finish:
            finish = ' '
        self.config['ids']['IDS_CFG_DEFAULT_START_DATETIME'] = start
        self.config['ids']['IDS_CFG_END_DATETIME'] = finish
        self.config['caboodle']['CABOODLE_DATE_FROM'] = start
        self.config['caboodle']['CABOODLE_DATE_UNTIL'] = finish

    def get_repo_info(self) -> dict:
        """
        Return a list of dictionary items with repository name and branch
        :return: list of repo dictionary items
        """
        repos = {}
        for repo in self.config['repositories']:
            if 'repo_name' in \
                    self.config['repositories'][repo]:
                repo_name = self.config['repositories'][repo]['repo_name']
            else:
                repo_name = repo
            this_repo = {'name': repo_name,
                         'branch': self.config['repositories'][repo]['branch']}
            repos[repo] = this_repo
        return repos

    def get_git_dir(self) -> str:
        """
        Return the main git directory specified
        """
        return self.config['main_git_dir']

    def get_data_for(self, dataname: str, element: str,
                     outer_element: str = '') -> dict:
        """
        Return information for dataname specified.
        :param dataname: str name of data requested
        :param element
        :param outer_element
        :return: data entry for config[dataname]
                             or config[element][dataname]
                             or config[outer_element][element][dataname]
        """
        if dataname in self.config:
            return self.config[dataname]
        elif element in self.config and \
                dataname in self.config[element]:
            return self.config[element][dataname]
        elif outer_element in self.config and \
                element in self.config[outer_element] and \
                dataname in self.config[outer_element][element]:
            return self.config[outer_element][element][dataname]
        return None
