import yaml


class ReadConfig:
    """
    Read the global-configurations.txt file
    and extract required data
    """

    def __init__(self, filename):
        """
        Read the file and create sets representing different data
        :param filename:
        """
        self.config_options = None
        with open(filename) as f:
            self.config_options = yaml.load(f, Loader=yaml.FullLoader)

    def get_repo_info(self):
        """
        Return a list of dictionary items with repository name and branch
        :return: list of repo dictionary items
        """
        repos = {}
        for repo in self.config_options['config']['repositories']:
            if 'repo_name' in \
                    self.config_options['config']['repositories'][repo]:
                repo_name = self.config_options['config']['repositories']
                [repo]['repo_name']
            else:
                repo_name = repo
            this_repo = {'name': repo_name,
                         'branch': self.config_options['config']['repositories'][repo]['branch']}
            repos[repo] =  this_repo
        return repos

    def get_git_dir(self):
        """
        Return the main git directory specified
        """
        return self.config_options['config']['main_git_dir']

    def get_caboodle_info(self):
        """
        Return the information relating to caboodle environment
        """
        return self.config_options['config']['caboodle']

    def get_uds_info(self):
        """
        Return the information relating to uds environment
        """
        return self.config_options['config']['uds']

    def get_rabbitmq_info(self):
        """
        Return the information relating to rabbitmq environment
        """
        return self.config_options['config']['rabbitmq']

    def get_ids_info(self):
        """
        Return the information relating to ids environment
        """
        return self.config_options['config']['ids']

    def get_informdb_info(self):
        """
        Return the information relating to informdb environment
        """
        return self.config_options['config']['informdb']

    def get_dates_info(self):
        """
        Return the information relating to dates specified
        """
        return self.config_options['config']['dates']

    def get_omop_info(self):
        """
        Return the information relating to omop environment
        """
        return self.config_options['config']['omop']

    def print_content(self):
        """ here for development"""
        print(self.config_options)
