import os
from git import Repo
from git import GitCommandError
from git import InvalidGitRepositoryError

from git import RemoteProgress


class MyProgressPrinter(RemoteProgress):
    def update(self, op_code, cur_count, max_count=None, message=''):
        print(op_code, cur_count, max_count, cur_count / (max_count or 100.0), message)


def _report_error(message):
    print(message)


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, main_dir, git_dir, repos):
        """
        Initialise the repository setup
        :param main_dir: the working directory in which to clone repositories
        :param repos: a dictionary items with name
                      and branch of each repositories
        :param git_dir: the main git repository path
        """
        self.main_dir = main_dir
        self.main_github = git_dir
        self.repos = repos
        self.current_repos = self.detect_repos()

    def clone_necessary_repos(self):
        """
        Clone the requested repositories and branches
        and make the self.current_repos list in line
        with these repositories
        """
        self.current_repos = {}
        for repo in self.repos:
            this_path = os.path.join(self.main_dir, repo)
            this_repo = self.repos[repo]
            this_git = this_repo['name'] + '.git'
            this_git_path = os.path.join(self.main_github, this_git)
            print(f'cloning {this_git_path} branch: {this_repo["branch"]} '
                  f'to {this_path}')
            try:
                Repo.clone_from(this_git_path, this_path,
                                branch=this_repo['branch'],
                                progress=MyProgressPrinter())
                self.current_repos[repo] = self.repos[repo].copy()
            except GitCommandError as e:
                _report_error('necessary repos could not be cloned due to'
                              + e.stderr)
                break

    def update_necessary_repositories(self):
        for repo in self.repos:
            if self._branches_match(repo):
                try:
                    print('Updating {0} with repo {1} and branch {2}'
                          ''.format(repo, self.repos[repo]['name'],
                                    self.repos[repo]['branch']))
                    this_repo = Repo(os.path.join(self.main_dir, repo))
                    this_repo.remotes[0].pull(progress=MyProgressPrinter())
                except GitCommandError as e:
                    _report_error('{0} with repo {1} and branch {2} could '
                                  'not be pulled due to {4}'
                                  ''.format(repo,
                                            self.repos[repo]['name'],
                                            self.repos[repo]['branch'],
                                            e.stderr))
                    break
            else:
                try:
                    print('Checking out repo {1}, branch {2} into {0}'
                          ''.format(repo, self.repos[repo]['name'],
                                    self.repos[repo]['branch']))
                    this_repo = Repo(os.path.join(self.main_dir, repo))
                    this_repo.git.checkout(self.repos[repo]['branch'])
                except GitCommandError as e:
                    _report_error('Cannot checkout repo {1}, branch {2} into'
                                  ' {0} due to {4}'
                                  ''.format(repo,
                                            self.repos[repo]['name'],
                                            self.repos[repo]['branch'],
                                            e.stderr))
                    break

    def _branches_match(self, repo):
        match = True
        if repo not in self.current_repos:
            match = False
        elif self.repos[repo]['name'] != self.current_repos[repo]['name']:
            match = False
        elif self.repos[repo]['branch'] != self.current_repos[repo]['branch']:
            match = False
        return match

    def detect_repos(self):
        """
        Repositories already exist; determine the names, directories and
        branches
        :return repos a list of repo dictionaries that reflect the current code
        """
        repos = {}
        list_of_dirs = os.listdir(self.main_dir)
        for this_dir in list_of_dirs:
            try:
                this_repo = Repo(os.path.join(self.main_dir, this_dir))
                remote_url = this_repo.remotes[0].config_reader.get("url")
                repo_name = os.path.splitext(os.path.basename(remote_url))[0]
                repo_info = {
                    'name': repo_name,
                    'branch': this_repo.active_branch.name
                }
                repos[this_dir] = repo_info
            except InvalidGitRepositoryError:
                continue
        return repos