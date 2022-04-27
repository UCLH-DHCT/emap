import yaml

from emap_runner.repos import Repository, Repositories


class GlobalConfiguration(dict):
    """A configuration constructed from a existing .yaml file"""

    def __init__(self, filename: str):
        super().__init__()

        with open(filename, "r") as f:
            self.update(yaml.load(f, Loader=yaml.FullLoader))

        self.filename = filename
        self.repositories = self._extract_repositories()

    def __setitem__(self, key, value):
        raise ValueError(f"Cannot set {key}. The global configuration is immutable")

    def _extract_repositories(
        self, default_branch_name: str = "master"
    ) -> Repositories:
        """Extract repository instances for all those present in the config"""

        repos = Repositories()

        for name, data in self["repositories"].items():

            if data is None:
                data = {"repo_name": name}

            repo = Repository(
                name=data.get("repo_name", name),
                branch=data.get("branch", default_branch_name),
            )

            repos.append(repo)

        return repos

    def get(self, *keys: str) -> str:
        """
        Get a value from this configuration based on a set of descending
        keys. e.g. repositories -> Emap-Core -> branch
        """

        if len(keys) == 0:
            raise ValueError("Must have at least one key")

        elif len(keys) == 1:
            return self[keys[0]]

        elif len(keys) == 2:
            return self[keys[0]][keys[1]]

        elif len(keys) == 3:
            return self[keys[0]][keys[1]][keys[2]]

        raise ValueError(f"Expecting at most 3 keys. Had: {keys}")
