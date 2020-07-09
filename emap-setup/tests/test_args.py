from pytest import fixture
from emap_setup import define_arguments


@fixture(scope="module")
def arg_parser():
    arg_parser = define_arguments()
    return arg_parser


def test_help(arg_parser):
    assert arg_parser._mutually_exclusive_groups[0]._actions[0].dest == 'help'


def test_init(arg_parser):
    assert arg_parser._mutually_exclusive_groups[0]._actions[1].dest == 'setup'
    args = arg_parser.parse_args(['setup', '-i'])
    assert args.setup == 'setup'
    assert args.init
    assert not args.update


def test_update(arg_parser):
    assert arg_parser._mutually_exclusive_groups[0]._actions[1].dest == 'setup'
    args = arg_parser.parse_args(['setup', '--update'])
    assert args.setup == 'setup'
    assert not args.init
    assert args.update


def test_missing_setup_option(arg_parser):
    assert arg_parser._mutually_exclusive_groups[0]._actions[1].dest == 'setup'
    args = arg_parser.parse_args(['setup'])
    assert args.setup == 'setup'
    assert not args.init
    assert not args.update
