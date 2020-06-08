import os
from git import Repo


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, tt):
        """
        :param star_schema: schema to query for star tables
        :param output_dir: defaults to repository output directory
        """
        self.main_dir = 'C:/Development/Helpers/Test'
        print(tt)

    def clone_necessary_repos(self):
        print('cloning')
        this_path = os.path.join(self.main_dir, 'Emap-Core')
        repo = Repo.clone_from('https://github.com/inform-health-informatics/Emap-Core.git', this_path, branch='develop')
