from pytest import fixture
from emap_setup import define_arguments


@fixture(scope="module")
def arg_parser():
    """
    :return: instance of ArgumentParser to be used by tests
    """
    arg_parser = define_arguments()
    return arg_parser


def test_help(arg_parser):
    """
    Test the default first potential arg is 'help'
    """
    assert arg_parser._mutually_exclusive_groups[0]._actions[0].dest == 'help'


def test_setup(arg_parser):
    """
    Test that the second argument is 'setup'
    """
    assert arg_parser._mutually_exclusive_groups[0]._actions[1].dest == 'setup'


def test_setup_init_flag(arg_parser):
    """
    Test that the -i flag to the setup argument produces
    args.init True and args.update False
    """
    args = arg_parser.parse_args(['setup', '-i'])
    assert args.setup == 'setup'
    assert args.init
    assert not args.update


def test_update(arg_parser):
    """
    Test that the --update flag to the setup argument produces
    args.init False and args.update True
    """
    args = arg_parser.parse_args(['setup', '--update'])
    assert args.setup == 'setup'
    assert not args.init
    assert args.update


def test_missing_setup_option(arg_parser):
    """
    Test that the missing flag to the setup argument produces
    args.init False and args.update False
    """
    args = arg_parser.parse_args(['setup'])
    assert args.setup == 'setup'
    assert not args.init
    assert not args.update
