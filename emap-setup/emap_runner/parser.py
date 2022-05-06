import sys
from argparse import ArgumentParser, Namespace
from typing import Optional, Sequence


class Parser(ArgumentParser):
    def parse_args(self, args: Optional[Sequence[str]] = None) -> Namespace:
        """
        Parse the arguments.

        The docker-compose subcommands need to be intercepted to prevent them
        being interpreted as arguments to this script. For example:

        $ python emap_runner.py docker up -d

        doesn't work without this solution. So parse all the commands up to
        and including docker then set docker_compose_args directly from the
        remainder.
        """
        if args is None:
            args = sys.argv[1:]

        if "docker" not in args:
            return super().parse_args(args)

        idx = next(i + 1 for i, arg in enumerate(args) if arg == "docker")

        namespace = super().parse_args(args[:idx] + ["X"])
        namespace.docker_compose_args = args[idx:]

        return namespace
