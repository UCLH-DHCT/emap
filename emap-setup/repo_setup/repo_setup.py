import os
from git import Repo, GitCommandError


def _report_error(message):
    print(message)


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, main_dir, repos, git_dir):
        """
        Initialise the repository setup
        :param main_dir: the working directory in which to clone repositories
        :param repos: a dictionary items with name and branch of each repositories
        :param git_dir: the main git repository path
        """
        self.main_dir = main_dir
        self.repos = repos
        self.main_github = git_dir

    def clone_necessary_repos(self):
        """
        Clone the requested repositories and branches
        """
        for repo in self.repos:
            this_path = os.path.join(self.main_dir, repo['dirname'])
            this_git = repo['name'] + '.git'
            this_git_path = os.path.join(self.main_github, this_git)
            print('cloning ' + this_git_path + ' branch:' + repo['branch'] + ' to ' + this_path)
            try:
                Repo.clone_from(this_git_path, this_path, branch=repo['branch'])
            except GitCommandError as e:
                _report_error('necessary repos could not be cloned due to' + e.stderr)
                break
