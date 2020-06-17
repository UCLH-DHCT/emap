import os

from git import Repo
from git import GitCommandError
from git import InvalidGitRepositoryError


def _report_error(message):
    print(message)


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, main_dir, git_dir, repos=None):
        """
        Initialise the repository setup
        :param main_dir: the working directory in which to clone repositories
        :param repos: a dictionary items with name and branch of each repositories
        :param git_dir: the main git repository path
        """
        self.main_dir = main_dir
        self.main_github = git_dir
        if repos:
            self.repos = repos
        else:
            self.repos =  self.detect_repos()

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

    def detect_repos(self):
        """
        Repositories already exist; determine the names, directories and branches
        Populate the self.repos list with the existing information
        """
        self.repos = []
        list_of_dirs = os.listdir(self.main_dir)
        for this_dir in list_of_dirs:
            try:
                this_repo = Repo(os.path.join(self.main_dir, this_dir))
                remote_url = this_repo.remotes[0].config_reader.get("url")
                repo_name = os.path.splitext(os.path.basename(remote_url))[0]
                repo_info = {
                    'dirname': this_dir,
                    'name': repo_name,
                    'branch': this_repo.active_branch.name
                }
                self.repos.append(repo_info)
            except InvalidGitRepositoryError:
                continue

        print(self.repos)